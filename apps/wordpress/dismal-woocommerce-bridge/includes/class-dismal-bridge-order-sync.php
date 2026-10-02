<?php

if (! defined('ABSPATH')) {
    exit;
}

final class Dismal_Bridge_Order_Sync
{
    private const META_EXTERNAL_ORDER_ID = '_dismal_external_order_id';
    private const META_SALE_ID = '_dismal_sale_id';
    private const META_SYNC_STATUS = '_dismal_sync_status';
    private const META_SYNC_MESSAGE = '_dismal_sync_message';
    private const META_LAST_SYNC_AT = '_dismal_last_sync_at';
    private const META_DELIVERED_LICENSES = '_dismal_delivered_licenses';

    private Dismal_Bridge_Api_Client $api_client;

    public static function register(Dismal_Bridge_Api_Client $api_client): void
    {
        $instance = new self($api_client);
        add_action('woocommerce_order_status_processing', [$instance, 'maybe_sync_order'], 20);
        add_action('woocommerce_order_status_completed', [$instance, 'maybe_sync_order'], 20);
        add_action('add_meta_boxes_shop_order', [$instance, 'register_meta_box']);
        add_action('admin_post_dismal_bridge_resync_order', [$instance, 'handle_manual_resync']);
        add_action('admin_post_dismal_bridge_sync_customer', [$instance, 'handle_manual_customer_sync']);
    }

    public function __construct(Dismal_Bridge_Api_Client $api_client)
    {
        $this->api_client = $api_client;
    }

    public function maybe_sync_order(int $order_id): void
    {
        $order = wc_get_order($order_id);
        if (! $order instanceof WC_Order) {
            return;
        }

        $settings = Dismal_Bridge_Settings::get();
        if (! in_array($order->get_status(), $settings['sync_statuses'], true)) {
            return;
        }

        $this->sync_order($order, false);
    }

    public function sync_order(WC_Order $order, bool $force = false)
    {
        $sale_id = (string) $order->get_meta(self::META_SALE_ID);
        $sync_status = (string) $order->get_meta(self::META_SYNC_STATUS);
        if (! $force && $sale_id !== '' && in_array($sync_status, ['synced', 'duplicated'], true)) {
            return [
                'externalOrderId' => (string) $order->get_meta(self::META_EXTERNAL_ORDER_ID),
                'saleId' => $sale_id,
                'status' => $sync_status,
                'duplicated' => $sync_status === 'duplicated',
                'skipped' => true,
            ];
        }

        $payload = $this->build_payload($order);
        if (is_wp_error($payload)) {
            $this->mark_error($order, $payload->get_error_message());
            return $payload;
        }

        $response = $this->api_client->push_paid_order($payload);
        if (is_wp_error($response)) {
            $this->mark_error($order, $response->get_error_message());
            return $response;
        }

        $order->update_meta_data(self::META_EXTERNAL_ORDER_ID, $response['externalOrderId'] ?? (string) $order->get_id());
        $order->update_meta_data(self::META_SALE_ID, $response['saleId'] ?? '');
        $order->update_meta_data(self::META_SYNC_STATUS, ! empty($response['duplicated']) ? 'duplicated' : 'synced');
        $order->update_meta_data(self::META_SYNC_MESSAGE, $response['status'] ?? 'CONFIRMED');
        $order->update_meta_data(self::META_LAST_SYNC_AT, current_time('mysql'));
        $this->store_delivered_licenses($order, is_array($response['licenses'] ?? null) ? $response['licenses'] : []);

        $customer_email = ! empty($response['customerEmail']) ? (string) $response['customerEmail'] : (string) $order->get_billing_email();
        $generated_password = ! empty($response['generatedPassword']) ? (string) $response['generatedPassword'] : null;
        if ($customer_email !== '') {
            try {
                Dismal_Bridge_Customer_Accounts::ensure_order_customer_account($order, $customer_email, $generated_password);
            } catch (Throwable $throwable) {
                $this->mark_error($order, $throwable->getMessage());
                return new WP_Error('dismal_bridge_customer_sync_error', $throwable->getMessage());
            }
        }

        $order->save();

        if (! empty($response['newCustomerCreated']) && $generated_password && $customer_email !== '') {
            Dismal_Bridge_Customer_Accounts::send_credentials_email($order, $customer_email, $generated_password);
        }

        $order->add_order_note(
            sprintf(
                'Dismal sync successful. Sale ID: %s. Status: %s.',
                $response['saleId'] ?? 'n/a',
                $response['status'] ?? 'n/a'
            )
        );

        return $response;
    }

    private function build_payload(WC_Order $order)
    {
        $items = [];
        $items_total = 0.0;

        foreach ($order->get_items('line_item') as $item) {
            $product_id = $item->get_product_id();
            $software_id = (string) get_post_meta($product_id, Dismal_Bridge_Product_Meta::META_SOFTWARE_ID, true);
            if ($software_id === '') {
                return new WP_Error(
                    'dismal_bridge_missing_software_id',
                    sprintf(__('Product %d is missing Dismal Software ID.', 'dismal-woocommerce-bridge'), $product_id)
                );
            }

            $quantity = (int) $item->get_quantity();
            $subtotal = (float) $item->get_total();
            $unit_price = $quantity > 0 ? round($subtotal / $quantity, wc_get_price_decimals()) : 0.0;
            $items_total += $subtotal;

            $items[] = [
                'softwareId' => $software_id,
                'quantity' => $quantity,
                'unitPrice' => $unit_price,
            ];
        }

        if ($items === []) {
            return new WP_Error('dismal_bridge_empty_order', __('The WooCommerce order has no line items to synchronize.', 'dismal-woocommerce-bridge'));
        }

        return [
            'orderId' => (string) $order->get_id(),
            'orderNumber' => $order->get_order_number(),
            'firstName' => (string) $order->get_billing_first_name(),
            'lastName' => (string) $order->get_billing_last_name(),
            'email' => (string) $order->get_billing_email(),
            'phone' => (string) $order->get_billing_phone(),
            'taxId' => (string) $order->get_meta('_billing_cedula'),
            'customerType' => $this->resolve_customer_type($order),
            'saleType' => $this->resolve_sale_type($order),
            'paymentMethod' => $this->resolve_payment_method($order),
            'paymentReference' => $this->resolve_payment_reference($order),
            'totalPaid' => round($items_total, wc_get_price_decimals()),
            'items' => $items,
        ];
    }

    private function resolve_customer_type(WC_Order $order): string
    {
        $settings = Dismal_Bridge_Settings::get();
        $customer_type = $settings['default_customer_type'];
        $customer = $order->get_user();

        if ($customer instanceof WP_User) {
            $dismal_customer_type = strtoupper((string) get_user_meta($customer->ID, '_dismal_customer_type', true));
            if ($dismal_customer_type === 'DISTRIBUTOR') {
                return 'DISTRIBUTOR';
            }
            if (in_array('wholesale_customer', (array) $customer->roles, true)) {
                return 'DISTRIBUTOR';
            }
            $intersection = array_intersect($settings['distributor_roles'], (array) $customer->roles);
            if ($intersection !== []) {
                $customer_type = 'DISTRIBUTOR';
            }
        }

        return $customer_type;
    }

    private function resolve_payment_method(WC_Order $order): string
    {
        if ((string) $order->get_payment_method() === Dismal_Bridge_Credit_Gateway::ID) {
            return 'CREDIT';
        }

        $settings = Dismal_Bridge_Settings::get();
        $gateway = (string) $order->get_payment_method();
        return $settings['payment_method_map'][$gateway] ?? 'TRANSFER';
    }

    private function resolve_sale_type(WC_Order $order): string
    {
        return (string) $order->get_payment_method() === Dismal_Bridge_Credit_Gateway::ID ? 'CREDIT' : 'CASH';
    }

    private function resolve_payment_reference(WC_Order $order): string
    {
        $transaction_id = (string) $order->get_transaction_id();
        if ($transaction_id !== '') {
            return $transaction_id;
        }

        return 'woo-' . $order->get_order_number();
    }

    private function mark_error(WC_Order $order, string $message): void
    {
        $order->update_meta_data(self::META_SYNC_STATUS, 'error');
        $order->update_meta_data(self::META_SYNC_MESSAGE, $message);
        $order->update_meta_data(self::META_LAST_SYNC_AT, current_time('mysql'));
        $order->save();
        $order->add_order_note('Dismal sync failed: ' . $message);
        error_log('[Dismal Bridge] Order ' . $order->get_id() . ' sync error: ' . $message);
    }

    private function store_delivered_licenses(WC_Order $order, array $licenses): void
    {
        $normalized = [];
        foreach ($licenses as $license) {
            if (! is_array($license)) {
                continue;
            }
            $license_key = sanitize_textarea_field((string) ($license['licenseKey'] ?? ''));
            if ($license_key === '') {
                continue;
            }
            $normalized[] = [
                'softwareName' => sanitize_text_field((string) ($license['softwareName'] ?? '')),
                'licenseKey' => $license_key,
                'assignedAt' => sanitize_text_field((string) ($license['assignedAt'] ?? '')),
            ];
        }

        if ($normalized !== []) {
            $order->update_meta_data(self::META_DELIVERED_LICENSES, $normalized);
        }
    }

    public function register_meta_box(): void
    {
        add_meta_box(
            'dismal-bridge-sync',
            __('Dismal Sync', 'dismal-woocommerce-bridge'),
            [$this, 'render_meta_box'],
            'shop_order',
            'side',
            'default'
        );
    }

    public function render_meta_box(WP_Post $post): void
    {
        if (! current_user_can('manage_woocommerce')) {
            return;
        }

        $order = wc_get_order($post->ID);
        if (! $order instanceof WC_Order) {
            return;
        }

        $status = (string) $order->get_meta(self::META_SYNC_STATUS);
        $message = (string) $order->get_meta(self::META_SYNC_MESSAGE);
        $sale_id = (string) $order->get_meta(self::META_SALE_ID);
        $last_sync_at = (string) $order->get_meta(self::META_LAST_SYNC_AT);
        $resync_nonce = wp_create_nonce('dismal_bridge_resync_' . $order->get_id());
        $resync_url = admin_url('admin-post.php?action=dismal_bridge_resync_order&order_id=' . $order->get_id() . '&_wpnonce=' . $resync_nonce);
        $customer_sync_nonce = wp_create_nonce('dismal_bridge_sync_customer_' . $order->get_id());
        $customer_sync_url = admin_url('admin-post.php?action=dismal_bridge_sync_customer&order_id=' . $order->get_id() . '&_wpnonce=' . $customer_sync_nonce);

        echo '<p><strong>' . esc_html__('Status:', 'dismal-woocommerce-bridge') . '</strong> ' . esc_html($status ?: 'pending') . '</p>';
        echo '<p><strong>' . esc_html__('Sale ID:', 'dismal-woocommerce-bridge') . '</strong> ' . esc_html($sale_id ?: '-') . '</p>';
        echo '<p><strong>' . esc_html__('Message:', 'dismal-woocommerce-bridge') . '</strong> ' . esc_html($message ?: '-') . '</p>';
        echo '<p><strong>' . esc_html__('Last Sync:', 'dismal-woocommerce-bridge') . '</strong> ' . esc_html($last_sync_at ?: '-') . '</p>';
        echo '<p><a class="button button-secondary" href="' . esc_url($resync_url) . '">' . esc_html__('Retry Sync', 'dismal-woocommerce-bridge') . '</a></p>';
        echo '<p><a class="button button-secondary" href="' . esc_url($customer_sync_url) . '">' . esc_html__('Sync Customer', 'dismal-woocommerce-bridge') . '</a></p>';
    }

    public function handle_manual_resync(): void
    {
        if (! current_user_can('manage_woocommerce')) {
            wp_die(esc_html__('You are not allowed to do this.', 'dismal-woocommerce-bridge'));
        }

        $order_id = isset($_GET['order_id']) ? absint($_GET['order_id']) : 0;
        if (! wp_verify_nonce($_GET['_wpnonce'] ?? '', 'dismal_bridge_resync_' . $order_id)) {
            wp_die(esc_html__('Invalid nonce.', 'dismal-woocommerce-bridge'));
        }

        $order = wc_get_order($order_id);
        if ($order instanceof WC_Order) {
            $this->sync_order($order, true);
        }

        wp_safe_redirect(wp_get_referer() ?: admin_url('edit.php?post_type=shop_order'));
        exit;
    }

    public function handle_manual_customer_sync(): void
    {
        if (! current_user_can('manage_woocommerce')) {
            wp_die(esc_html__('You are not allowed to do this.', 'dismal-woocommerce-bridge'));
        }

        $order_id = isset($_GET['order_id']) ? absint($_GET['order_id']) : 0;
        if (! wp_verify_nonce($_GET['_wpnonce'] ?? '', 'dismal_bridge_sync_customer_' . $order_id)) {
            wp_die(esc_html__('Invalid nonce.', 'dismal-woocommerce-bridge'));
        }

        $order = wc_get_order($order_id);
        if ($order instanceof WC_Order) {
            try {
                $user = Dismal_Bridge_Customer_Accounts::sync_customer_from_order($order);
                if ($user instanceof WP_User) {
                    $order->add_order_note(
                        sprintf(
                            'Dismal customer sync successful. WordPress customer ID: %d.',
                            $user->ID
                        )
                    );
                } else {
                    $order->add_order_note('Dismal customer sync skipped: order has no billing email.');
                }
            } catch (Throwable $throwable) {
                $order->add_order_note('Dismal customer sync failed: ' . $throwable->getMessage());
            }
        }

        wp_safe_redirect(wp_get_referer() ?: admin_url('edit.php?post_type=shop_order'));
        exit;
    }
}
