<?php

if (! defined('ABSPATH')) {
    exit;
}

final class Dismal_Bridge_Product_Meta
{
    public const META_SOFTWARE_ID = '_dismal_software_id';

    public static function register(): void
    {
        add_action('woocommerce_product_options_general_product_data', [self::class, 'render_field']);
        add_action('woocommerce_process_product_meta', [self::class, 'save_field']);
    }

    public static function render_field(): void
    {
        woocommerce_wp_text_input([
            'id' => self::META_SOFTWARE_ID,
            'label' => __('Dismal Software ID', 'dismal-woocommerce-bridge'),
            'description' => __('UUID of the software in the Dismal VPS backend.', 'dismal-woocommerce-bridge'),
            'desc_tip' => true,
        ]);
    }

    public static function save_field(int $product_id): void
    {
        $value = isset($_POST[self::META_SOFTWARE_ID]) ? sanitize_text_field(wp_unslash($_POST[self::META_SOFTWARE_ID])) : '';
        if ($value === '') {
            delete_post_meta($product_id, self::META_SOFTWARE_ID);
            return;
        }

        update_post_meta($product_id, self::META_SOFTWARE_ID, $value);
    }
}
