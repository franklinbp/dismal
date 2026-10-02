<?php
/**
 * Plugin Name: Dismal WooCommerce Bridge
 * Plugin URI: https://dismal.com
 * Description: Synchronizes paid WooCommerce orders with the Dismal backend.
 * Version: 1.0.0
 * Author: Dismal
 * Author URI: https://dismal.com
 * Requires Plugins: woocommerce
 * Text Domain: dismal-woocommerce-bridge
 */

if (! defined('ABSPATH')) {
    exit;
}

define('MALSKN_WC_BRIDGE_VERSION', '1.0.0');
define('MALSKN_WC_BRIDGE_FILE', __FILE__);
define('MALSKN_WC_BRIDGE_DIR', plugin_dir_path(__FILE__));
define('MALSKN_WC_BRIDGE_URL', plugin_dir_url(__FILE__));

require_once MALSKN_WC_BRIDGE_DIR . 'includes/class-dismal-bridge-settings.php';
require_once MALSKN_WC_BRIDGE_DIR . 'includes/class-dismal-bridge-api-client.php';
require_once MALSKN_WC_BRIDGE_DIR . 'includes/class-dismal-bridge-product-meta.php';
require_once MALSKN_WC_BRIDGE_DIR . 'includes/class-dismal-bridge-order-sync.php';
require_once MALSKN_WC_BRIDGE_DIR . 'includes/class-dismal-bridge-customer-accounts.php';
require_once MALSKN_WC_BRIDGE_DIR . 'includes/class-dismal-bridge-customer-import.php';

final class Dismal_WooCommerce_Bridge
{
    private static ?Dismal_WooCommerce_Bridge $instance = null;

    public static function instance(): Dismal_WooCommerce_Bridge
    {
        if (self::$instance === null) {
            self::$instance = new self();
        }

        return self::$instance;
    }

    private function __construct()
    {
        add_action('plugins_loaded', [$this, 'boot']);
    }

    public function boot(): void
    {
        if (! class_exists('WooCommerce')) {
            add_action('admin_notices', [$this, 'render_missing_woocommerce_notice']);
            return;
        }

        require_once MALSKN_WC_BRIDGE_DIR . 'includes/class-dismal-bridge-credit-gateway.php';

        Dismal_Bridge_Settings::register();
        Dismal_Bridge_Product_Meta::register();
        $api_client = new Dismal_Bridge_Api_Client();
        Dismal_Bridge_Order_Sync::register($api_client);
        Dismal_Bridge_Customer_Accounts::register($api_client);
        Dismal_Bridge_Customer_Import::register($api_client);
        Dismal_Bridge_Credit_Gateway::register();
    }

    public function render_missing_woocommerce_notice(): void
    {
        if (! current_user_can('activate_plugins')) {
            return;
        }

        echo '<div class="notice notice-error"><p>';
        echo esc_html__('Dismal WooCommerce Bridge requires WooCommerce to be active.', 'dismal-woocommerce-bridge');
        echo '</p></div>';
    }
}

Dismal_WooCommerce_Bridge::instance();
