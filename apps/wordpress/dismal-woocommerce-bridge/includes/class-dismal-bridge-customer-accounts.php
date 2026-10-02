<?php

if (! defined('ABSPATH')) {
    exit;
}

final class Dismal_Bridge_Customer_Accounts
{
    private Dismal_Bridge_Api_Client $api_client;

    public static function register(Dismal_Bridge_Api_Client $api_client): void
    {
        $instance = new self($api_client);
        add_filter('authenticate', [$instance, 'authenticate_against_dismal'], 50, 3);
        add_action('woocommerce_created_customer', [$instance, 'sync_new_customer_to_dismal'], 20, 3);
        add_action('woocommerce_order_details_after_order_table', [$instance, 'render_dismal_order_reference']);
        add_filter('woocommerce_email_order_meta_fields', [$instance, 'inject_dismal_order_meta'], 10, 3);
    }

    public function __construct(Dismal_Bridge_Api_Client $api_client)
    {
        $this->api_client = $api_client;
    }

    public function authenticate_against_dismal($user, $username, $password)
    {
        if (! is_string($username) || trim($username) === '' || ! is_string($password) || $password === '') {
            return $user;
        }

        if ($user instanceof WP_User) {
            return $user;
        }

        $email = sanitize_email($username);
        if ($email === '') {
            return $user;
        }

        $auth = $this->api_client->authenticate_customer($email, $password);
        if (is_wp_error($auth) || empty($auth['token'])) {
            return $user;
        }

        $profile = $this->api_client->fetch_current_user($auth['token']);
        if (is_wp_error($profile) || empty($profile['email'])) {
            return $user;
        }

        if (! in_array(($profile['role'] ?? ''), ['USER', 'CUSTOMER'], true)) {
            return new WP_Error('dismal_bridge_role_not_allowed', __('This Dismal account cannot access WooCommerce.', 'dismal-woocommerce-bridge'));
        }

        $wp_user = $this->ensure_wordpress_customer(
            $profile['email'],
            $password
        );
        $this->attach_orders_by_email($wp_user->ID, $profile['email']);

        return $wp_user;
    }

    public function sync_new_customer_to_dismal(int $customer_id, array $new_customer_data = [], bool $password_generated = false): void
    {
        $wp_user = get_user_by('id', $customer_id);
        if (! $wp_user instanceof WP_User) {
            return;
        }

        $password = '';
        if (! empty($_POST['account_password'])) {
            $password = sanitize_text_field(wp_unslash($_POST['account_password']));
        } elseif (! empty($new_customer_data['user_pass'])) {
            $password = (string) $new_customer_data['user_pass'];
        }

        if ($password === '') {
            return;
        }

        $payload = [
            'firstname' => (string) get_user_meta($customer_id, 'billing_first_name', true),
            'lastname' => (string) get_user_meta($customer_id, 'billing_last_name', true),
            'email' => $wp_user->user_email,
            'password' => $password,
        ];

        $response = $this->api_client->register_customer($payload);
        if (is_wp_error($response)) {
            $code = $response->get_error_code();
            if ($code !== 'dismal_bridge_api_error') {
                return;
            }
        }
    }

    public function render_dismal_order_reference($order): void
    {
        if (! $order instanceof WC_Order) {
            return;
        }

        $sale_id = (string) $order->get_meta('_dismal_sale_id');
        $licenses = $this->get_order_delivered_licenses($order);
        if ($sale_id === '' && $licenses === []) {
            return;
        }

        echo '<section class="woocommerce-order-details">';
        echo '<h2>' . esc_html__('Referencia Dismal', 'dismal-woocommerce-bridge') . '</h2>';
        if ($sale_id !== '') {
            echo '<p><strong>' . esc_html__('Sale ID', 'dismal-woocommerce-bridge') . ':</strong> ' . esc_html($sale_id) . '</p>';
        }
        if ($licenses !== []) {
            echo '<h3>' . esc_html__('Licencias entregadas', 'dismal-woocommerce-bridge') . '</h3>';
            echo '<ul class="woocommerce-order-overview woocommerce-thankyou-order-details order_details">';
            foreach ($licenses as $license) {
                $software_name = (string) ($license['softwareName'] ?? '');
                $license_key = (string) ($license['licenseKey'] ?? '');
                echo '<li>';
                if ($software_name !== '') {
                    echo '<strong>' . esc_html($software_name) . '</strong><br />';
                }
                echo '<code>' . esc_html($license_key) . '</code>';
                echo '</li>';
            }
            echo '</ul>';
        }
        echo '</section>';
    }

    public function inject_dismal_order_meta(array $fields, bool $sent_to_admin, $order): array
    {
        if (! $order instanceof WC_Order) {
            return $fields;
        }

        $sale_id = (string) $order->get_meta('_dismal_sale_id');
        if ($sale_id !== '') {
            $fields['dismal_sale_id'] = [
                'label' => __('Dismal Sale ID', 'dismal-woocommerce-bridge'),
                'value' => $sale_id,
            ];
        }
        $licenses = $this->get_order_delivered_licenses($order);
        if ($licenses !== []) {
            $fields['dismal_licenses'] = [
                'label' => __('Licencias Dismal', 'dismal-woocommerce-bridge'),
                'value' => implode("\n", array_map(
                    static function (array $license): string {
                        $software_name = (string) ($license['softwareName'] ?? '');
                        $license_key = (string) ($license['licenseKey'] ?? '');
                        return trim($software_name . ': ' . $license_key, ': ');
                    },
                    $licenses
                )),
            ];
        }

        return $fields;
    }

    private function get_order_delivered_licenses(WC_Order $order): array
    {
        $licenses = $order->get_meta('_dismal_delivered_licenses');
        return is_array($licenses) ? $licenses : [];
    }

    public static function ensure_order_customer_account(WC_Order $order, string $email, ?string $password = null): ?WP_User
    {
        $instance = new self(new Dismal_Bridge_Api_Client());
        $wp_user = $instance->ensure_wordpress_customer($email, $password);
        $instance->sync_customer_profile_from_order($wp_user->ID, $order);
        $instance->attach_order_to_user($order, $wp_user->ID);
        $instance->attach_orders_by_email($wp_user->ID, $email);

        return $wp_user;
    }

    public static function sync_customer_from_order(WC_Order $order): ?WP_User
    {
        $email = (string) $order->get_billing_email();
        if ($email === '') {
            return null;
        }

        $instance = new self(new Dismal_Bridge_Api_Client());
        $wp_user = $instance->ensure_wordpress_customer($email, null);
        $instance->sync_customer_profile_from_order($wp_user->ID, $order);
        $instance->attach_order_to_user($order, $wp_user->ID);
        $instance->attach_orders_by_email($wp_user->ID, $email);

        return $wp_user;
    }

    public static function send_credentials_email(WC_Order $order, string $email, string $password): void
    {
        $subject = sprintf(__('Acceso a tu cuenta en %s', 'dismal-woocommerce-bridge'), wp_specialchars_decode(get_bloginfo('name'), ENT_QUOTES));
        $message = implode("\n\n", [
            sprintf(__('Hola %s,', 'dismal-woocommerce-bridge'), trim($order->get_formatted_billing_full_name()) ?: $email),
            __('Tu compra fue registrada correctamente y ya tienes acceso a tu cuenta para revisar pedidos.', 'dismal-woocommerce-bridge'),
            sprintf(__('Correo: %s', 'dismal-woocommerce-bridge'), $email),
            sprintf(__('Clave inicial: %s', 'dismal-woocommerce-bridge'), $password),
            __('Puedes ingresar con estas mismas credenciales creadas en Dismal y WooCommerce.', 'dismal-woocommerce-bridge'),
        ]);

        wp_mail($email, $subject, $message);
    }

    private function ensure_wordpress_customer(string $email, ?string $password = null): WP_User
    {
        $existing = get_user_by('email', $email);
        if ($existing instanceof WP_User) {
            if ($password !== null && $password !== '') {
                wp_set_password($password, $existing->ID);
            }
            if (! in_array('customer', (array) $existing->roles, true)) {
                $existing->add_role('customer');
            }
            return $existing;
        }

        $username = $this->generate_username_from_email($email);
        $user_id = wp_create_user($username, $password ?: wp_generate_password(16, true, true), $email);
        if (is_wp_error($user_id)) {
            $fallback = get_user_by('email', $email);
            if ($fallback instanceof WP_User) {
                return $fallback;
            }
            throw new RuntimeException($user_id->get_error_message());
        }

        $user = get_user_by('id', $user_id);
        if (! $user instanceof WP_User) {
            throw new RuntimeException('Unable to create WooCommerce customer.');
        }
        $user->set_role('customer');

        return $user;
    }

    private function sync_customer_profile_from_order(int $user_id, WC_Order $order): void
    {
        $profile_updates = [
            'first_name' => (string) $order->get_billing_first_name(),
            'last_name' => (string) $order->get_billing_last_name(),
            'billing_first_name' => (string) $order->get_billing_first_name(),
            'billing_last_name' => (string) $order->get_billing_last_name(),
            'billing_company' => (string) $order->get_billing_company(),
            'billing_phone' => (string) $order->get_billing_phone(),
            'billing_email' => (string) $order->get_billing_email(),
            'billing_address_1' => (string) $order->get_billing_address_1(),
            'billing_address_2' => (string) $order->get_billing_address_2(),
            'billing_city' => (string) $order->get_billing_city(),
            'billing_state' => (string) $order->get_billing_state(),
            'billing_postcode' => (string) $order->get_billing_postcode(),
            'billing_country' => (string) $order->get_billing_country(),
            'shipping_first_name' => (string) $order->get_shipping_first_name(),
            'shipping_last_name' => (string) $order->get_shipping_last_name(),
            'shipping_company' => (string) $order->get_shipping_company(),
            'shipping_address_1' => (string) $order->get_shipping_address_1(),
            'shipping_address_2' => (string) $order->get_shipping_address_2(),
            'shipping_city' => (string) $order->get_shipping_city(),
            'shipping_state' => (string) $order->get_shipping_state(),
            'shipping_postcode' => (string) $order->get_shipping_postcode(),
            'shipping_country' => (string) $order->get_shipping_country(),
        ];

        foreach ($profile_updates as $meta_key => $meta_value) {
            if ($meta_value === '') {
                continue;
            }

            update_user_meta($user_id, $meta_key, $meta_value);
        }

        $tax_id = (string) $order->get_meta('_billing_cedula');
        if ($tax_id !== '') {
            update_user_meta($user_id, 'billing_cedula', $tax_id);
            update_user_meta($user_id, '_billing_cedula', $tax_id);
        }
    }

    private function attach_orders_by_email(int $user_id, string $email): void
    {
        if (! function_exists('wc_get_orders')) {
            return;
        }

        $orders = wc_get_orders([
            'limit' => -1,
            'billing_email' => $email,
            'return' => 'objects',
        ]);

        foreach ($orders as $order) {
            if ($order instanceof WC_Order) {
                $this->attach_order_to_user($order, $user_id);
            }
        }
    }

    private function attach_order_to_user(WC_Order $order, int $user_id): void
    {
        if ((int) $order->get_customer_id() === $user_id) {
            return;
        }
        $order->set_customer_id($user_id);
        $order->save();
    }

    private function generate_username_from_email(string $email): string
    {
        $parts = explode('@', $email);
        $base = sanitize_user($parts[0] ?? '', true);
        if ($base === '') {
            $base = 'dismal_customer';
        }
        $candidate = $base;
        $suffix = 1;
        while (username_exists($candidate)) {
            $candidate = $base . '_' . $suffix;
            $suffix++;
        }

        return $candidate;
    }
}
