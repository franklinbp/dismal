<?php

if (! defined('ABSPATH')) {
    exit;
}

final class Dismal_Bridge_Credit_Gateway extends WC_Payment_Gateway
{
    public const ID = 'dismal_credit';

    public static function register(): void
    {
        add_filter('woocommerce_payment_gateways', [self::class, 'add_gateway']);
        add_filter('woocommerce_available_payment_gateways', [self::class, 'filter_available_gateways']);
    }

    public static function add_gateway(array $gateways): array
    {
        $gateways[] = self::class;
        return $gateways;
    }

    public static function filter_available_gateways(array $gateways): array
    {
        if (! isset($gateways[self::ID])) {
            return $gateways;
        }

        if (! is_user_logged_in() || ! self::current_customer_has_credit()) {
            unset($gateways[self::ID]);
        }

        return $gateways;
    }

    public function __construct()
    {
        $this->id = self::ID;
        $this->method_title = __('Credito Dismal', 'dismal-woocommerce-bridge');
        $this->method_description = __('Permite comprar usando el credito aprobado en Dismal.', 'dismal-woocommerce-bridge');
        $this->has_fields = false;
        $this->supports = ['products'];

        $this->init_form_fields();
        $this->init_settings();

        $this->enabled = $this->get_option('enabled', 'yes');
        $this->title = $this->get_option('title', __('Credito Dismal', 'dismal-woocommerce-bridge'));
        $this->description = $this->get_option(
            'description',
            __('Usa tu credito aprobado en Dismal. La licencia se entrega al confirmar la venta.', 'dismal-woocommerce-bridge')
        );

        add_action('woocommerce_update_options_payment_gateways_' . $this->id, [$this, 'process_admin_options']);
    }

    public function init_form_fields(): void
    {
        $this->form_fields = [
            'enabled' => [
                'title' => __('Enable/Disable', 'dismal-woocommerce-bridge'),
                'type' => 'checkbox',
                'label' => __('Enable Dismal credit checkout', 'dismal-woocommerce-bridge'),
                'default' => 'yes',
            ],
            'title' => [
                'title' => __('Title', 'dismal-woocommerce-bridge'),
                'type' => 'text',
                'default' => __('Credito Dismal', 'dismal-woocommerce-bridge'),
            ],
            'description' => [
                'title' => __('Description', 'dismal-woocommerce-bridge'),
                'type' => 'textarea',
                'default' => __('Usa tu credito aprobado en Dismal. La licencia se entrega al confirmar la venta.', 'dismal-woocommerce-bridge'),
            ],
        ];
    }

    public function process_payment($order_id): array
    {
        $order = wc_get_order($order_id);
        if (! $order instanceof WC_Order) {
            wc_add_notice(__('Invalid WooCommerce order.', 'dismal-woocommerce-bridge'), 'error');
            return ['result' => 'failure'];
        }

        if (! self::order_customer_has_credit($order)) {
            wc_add_notice(__('Tu cuenta no tiene credito Dismal activo.', 'dismal-woocommerce-bridge'), 'error');
            return ['result' => 'failure'];
        }

        $order->update_status('processing', __('Dismal credit checkout approved. Waiting for Dismal sync.', 'dismal-woocommerce-bridge'));

        if (WC()->cart) {
            WC()->cart->empty_cart();
        }

        return [
            'result' => 'success',
            'redirect' => $this->get_return_url($order),
        ];
    }

    public static function order_customer_has_credit(WC_Order $order): bool
    {
        $user_id = (int) $order->get_user_id();
        if ($user_id <= 0) {
            return false;
        }

        return self::user_has_available_credit($user_id, (float) $order->get_total());
    }

    private static function current_customer_has_credit(): bool
    {
        $amount = WC()->cart ? (float) WC()->cart->get_total('edit') : 0.0;
        return self::user_has_available_credit(get_current_user_id(), $amount);
    }

    private static function user_has_available_credit(int $user_id, float $amount): bool
    {
        $enabled = (string) get_user_meta($user_id, '_dismal_enabled', true);
        $has_credit = (string) get_user_meta($user_id, '_dismal_has_credit', true);
        $credit_limit = (float) get_user_meta($user_id, '_dismal_credit_limit', true);
        $credit_used = (float) get_user_meta($user_id, '_dismal_credit_used', true);
        $available = max(0.0, $credit_limit - $credit_used);

        return $enabled === '1' && $has_credit === '1' && $available > 0 && $available + 0.0001 >= $amount;
    }
}
