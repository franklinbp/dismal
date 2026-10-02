<?php

if (! defined('ABSPATH')) {
    exit;
}

final class Dismal_Bridge_Settings
{
    public const OPTION_KEY = 'dismal_bridge_settings';
    private const NOTICE_TRANSIENT_PREFIX = 'dismal_bridge_settings_notice_';

    public static function register(): void
    {
        add_action('admin_init', [self::class, 'register_settings']);
        add_action('admin_menu', [self::class, 'register_menu']);
        add_action('admin_post_dismal_bridge_test_connection', [self::class, 'handle_test_connection']);
    }

    public static function defaults(): array
    {
        return [
            'api_base_url' => '',
            'api_key' => '',
            'sync_statuses' => ['processing', 'completed'],
            'distributor_roles' => [],
            'default_customer_type' => 'FINAL',
            'payment_method_map' => [
                'bacs' => 'TRANSFER',
                'cod' => 'CASH',
                'cheque' => 'TRANSFER',
                'paypal' => 'CARD',
                'stripe' => 'CARD',
            ],
        ];
    }

    public static function get(): array
    {
        $stored = get_option(self::OPTION_KEY, []);
        $settings = wp_parse_args(is_array($stored) ? $stored : [], self::defaults());
        $settings['api_base_url'] = untrailingslashit((string) $settings['api_base_url']);
        $settings['default_customer_type'] = strtoupper((string) $settings['default_customer_type']) === 'DISTRIBUTOR' ? 'DISTRIBUTOR' : 'FINAL';
        $settings['sync_statuses'] = array_values(array_intersect((array) $settings['sync_statuses'], ['processing', 'completed']));
        $settings['distributor_roles'] = array_values(array_filter(array_map('sanitize_key', (array) $settings['distributor_roles'])));
        $settings['payment_method_map'] = self::normalize_payment_map((array) $settings['payment_method_map']);

        return $settings;
    }

    public static function register_settings(): void
    {
        register_setting(
            'dismal_bridge',
            self::OPTION_KEY,
            [
                'type' => 'array',
                'sanitize_callback' => [self::class, 'sanitize_settings'],
                'default' => self::defaults(),
            ]
        );

        add_settings_section(
            'dismal_bridge_main',
            __('Dismal Backend', 'dismal-woocommerce-bridge'),
            [self::class, 'render_section_intro'],
            'dismal-bridge'
        );

        add_settings_field(
            'api_base_url',
            __('API Base URL', 'dismal-woocommerce-bridge'),
            [self::class, 'render_api_base_url_field'],
            'dismal-bridge',
            'dismal_bridge_main'
        );

        add_settings_field(
            'api_key',
            __('Integration Key', 'dismal-woocommerce-bridge'),
            [self::class, 'render_api_key_field'],
            'dismal-bridge',
            'dismal_bridge_main'
        );

        add_settings_field(
            'sync_statuses',
            __('Paid Order Statuses', 'dismal-woocommerce-bridge'),
            [self::class, 'render_sync_statuses_field'],
            'dismal-bridge',
            'dismal_bridge_main'
        );

        add_settings_field(
            'default_customer_type',
            __('Default Customer Type', 'dismal-woocommerce-bridge'),
            [self::class, 'render_default_customer_type_field'],
            'dismal-bridge',
            'dismal_bridge_main'
        );

        add_settings_field(
            'distributor_roles',
            __('Distributor WordPress Roles', 'dismal-woocommerce-bridge'),
            [self::class, 'render_distributor_roles_field'],
            'dismal-bridge',
            'dismal_bridge_main'
        );

        add_settings_field(
            'payment_method_map',
            __('Payment Method Map', 'dismal-woocommerce-bridge'),
            [self::class, 'render_payment_method_map_field'],
            'dismal-bridge',
            'dismal_bridge_main'
        );
    }

    public static function sanitize_settings($input): array
    {
        $defaults = self::defaults();
        $value = is_array($input) ? $input : [];

        return [
            'api_base_url' => esc_url_raw((string) ($value['api_base_url'] ?? $defaults['api_base_url'])),
            'api_key' => sanitize_text_field((string) ($value['api_key'] ?? $defaults['api_key'])),
            'sync_statuses' => array_values(array_intersect((array) ($value['sync_statuses'] ?? $defaults['sync_statuses']), ['processing', 'completed'])),
            'distributor_roles' => array_values(array_filter(array_map('sanitize_key', (array) ($value['distributor_roles'] ?? [])))),
            'default_customer_type' => strtoupper((string) ($value['default_customer_type'] ?? 'FINAL')) === 'DISTRIBUTOR' ? 'DISTRIBUTOR' : 'FINAL',
            'payment_method_map' => self::normalize_payment_map((array) ($value['payment_method_map'] ?? $defaults['payment_method_map'])),
        ];
    }

    private static function normalize_payment_map(array $map): array
    {
        $normalized = [];

        foreach ($map as $gateway => $payment_method) {
            $gateway = sanitize_key((string) $gateway);
            if ($gateway === '') {
                continue;
            }

            $payment_method = strtoupper(sanitize_text_field((string) $payment_method));
            if (! in_array($payment_method, ['CASH', 'CREDIT', 'CARD', 'TRANSFER'], true)) {
                continue;
            }

            $normalized[$gateway] = $payment_method;
        }

        return $normalized;
    }

    public static function register_menu(): void
    {
        add_submenu_page(
            'woocommerce',
            __('Dismal Bridge', 'dismal-woocommerce-bridge'),
            __('Dismal Bridge', 'dismal-woocommerce-bridge'),
            'manage_woocommerce',
            'dismal-bridge',
            [self::class, 'render_settings_page']
        );
    }

    public static function render_settings_page(): void
    {
        if (! current_user_can('manage_woocommerce')) {
            return;
        }
        ?>
        <div class="wrap">
            <h1><?php echo esc_html__('Dismal WooCommerce Bridge', 'dismal-woocommerce-bridge'); ?></h1>
            <?php self::render_notice(); ?>
            <form action="options.php" method="post">
                <?php
                settings_fields('dismal_bridge');
                do_settings_sections('dismal-bridge');
                submit_button(__('Save Settings', 'dismal-woocommerce-bridge'));
                ?>
            </form>
            <?php self::render_connection_test_panel(); ?>
            <?php Dismal_Bridge_Customer_Import::render_panel(); ?>
        </div>
        <?php
    }

    public static function render_section_intro(): void
    {
        echo '<p>' . esc_html__('Configure the secure connection between WooCommerce and the Dismal VPS backend.', 'dismal-woocommerce-bridge') . '</p>';
    }

    public static function render_api_base_url_field(): void
    {
        $settings = self::get();
        printf(
            '<input type="url" class="regular-text code" name="%1$s[api_base_url]" value="%2$s" placeholder="https://api.example.com" />',
            esc_attr(self::OPTION_KEY),
            esc_attr($settings['api_base_url'])
        );
    }

    public static function render_api_key_field(): void
    {
        $settings = self::get();
        printf(
            '<input type="password" class="regular-text code" name="%1$s[api_key]" value="%2$s" autocomplete="off" />',
            esc_attr(self::OPTION_KEY),
            esc_attr($settings['api_key'])
        );
    }

    public static function render_sync_statuses_field(): void
    {
        $settings = self::get();
        $statuses = [
            'processing' => __('Processing', 'dismal-woocommerce-bridge'),
            'completed' => __('Completed', 'dismal-woocommerce-bridge'),
        ];

        foreach ($statuses as $value => $label) {
            printf(
                '<label><input type="checkbox" name="%1$s[sync_statuses][]" value="%2$s" %3$s /> %4$s</label><br/>',
                esc_attr(self::OPTION_KEY),
                esc_attr($value),
                checked(in_array($value, $settings['sync_statuses'], true), true, false),
                esc_html($label)
            );
        }
    }

    public static function render_default_customer_type_field(): void
    {
        $settings = self::get();
        ?>
        <select name="<?php echo esc_attr(self::OPTION_KEY); ?>[default_customer_type]">
            <option value="FINAL" <?php selected($settings['default_customer_type'], 'FINAL'); ?>>FINAL</option>
            <option value="DISTRIBUTOR" <?php selected($settings['default_customer_type'], 'DISTRIBUTOR'); ?>>DISTRIBUTOR</option>
        </select>
        <?php
    }

    public static function render_distributor_roles_field(): void
    {
        $settings = self::get();
        $roles = wp_roles()->roles;

        foreach ($roles as $role_key => $role) {
            printf(
                '<label><input type="checkbox" name="%1$s[distributor_roles][]" value="%2$s" %3$s /> %4$s</label><br/>',
                esc_attr(self::OPTION_KEY),
                esc_attr($role_key),
                checked(in_array($role_key, $settings['distributor_roles'], true), true, false),
                esc_html($role['name'])
            );
        }

        echo '<p class="description">' . esc_html__('If the WooCommerce customer has one of these roles, the plugin sends customerType=DISTRIBUTOR to the VPS.', 'dismal-woocommerce-bridge') . '</p>';
    }

    public static function render_payment_method_map_field(): void
    {
        $settings = self::get();
        $gateways = WC()->payment_gateways() ? WC()->payment_gateways()->payment_gateways() : [];
        $gateway_ids = array_unique(array_merge(array_keys($gateways), array_keys($settings['payment_method_map'])));

        echo '<table class="widefat striped" style="max-width: 720px">';
        echo '<thead><tr><th>' . esc_html__('WooCommerce Gateway', 'dismal-woocommerce-bridge') . '</th><th>' . esc_html__('Dismal PaymentMethod', 'dismal-woocommerce-bridge') . '</th></tr></thead><tbody>';
        foreach ($gateway_ids as $gateway_id) {
            $current = $settings['payment_method_map'][$gateway_id] ?? 'TRANSFER';
            echo '<tr>';
            echo '<td><code>' . esc_html($gateway_id) . '</code></td>';
            echo '<td>';
            printf(
                '<select name="%1$s[payment_method_map][%2$s]"><option value="CASH" %3$s>CASH</option><option value="CREDIT" %4$s>CREDIT</option><option value="CARD" %5$s>CARD</option><option value="TRANSFER" %6$s>TRANSFER</option></select>',
                esc_attr(self::OPTION_KEY),
                esc_attr($gateway_id),
                selected($current, 'CASH', false),
                selected($current, 'CREDIT', false),
                selected($current, 'CARD', false),
                selected($current, 'TRANSFER', false)
            );
            echo '</td></tr>';
        }
        echo '</tbody></table>';
    }

    public static function render_connection_test_panel(): void
    {
        $nonce = wp_create_nonce('dismal_bridge_test_connection');
        echo '<hr style="margin: 2rem 0;" />';
        echo '<h2>' . esc_html__('Prueba de conexion', 'dismal-woocommerce-bridge') . '</h2>';
        echo '<p>' . esc_html__('Valida que WordPress pueda comunicarse con el backend Dismal usando la URL base y la clave configuradas.', 'dismal-woocommerce-bridge') . '</p>';
        echo '<form method="post" action="' . esc_url(admin_url('admin-post.php')) . '">';
        echo '<input type="hidden" name="action" value="dismal_bridge_test_connection" />';
        echo '<input type="hidden" name="_wpnonce" value="' . esc_attr($nonce) . '" />';
        submit_button(__('Probar conexion con Dismal', 'dismal-woocommerce-bridge'), 'secondary', 'submit', false);
        echo '</form>';
    }

    public static function handle_test_connection(): void
    {
        if (! current_user_can('manage_woocommerce')) {
            wp_die(esc_html__('You are not allowed to do this.', 'dismal-woocommerce-bridge'));
        }
        if (! wp_verify_nonce($_POST['_wpnonce'] ?? '', 'dismal_bridge_test_connection')) {
            wp_die(esc_html__('Invalid nonce.', 'dismal-woocommerce-bridge'));
        }

        $client = new Dismal_Bridge_Api_Client();
        $health = $client->health_check();
        if (is_wp_error($health)) {
            self::store_notice([
                'type' => 'error',
                'message' => 'Dismal no responde: ' . $health->get_error_message(),
            ]);
            wp_safe_redirect(admin_url('admin.php?page=dismal-bridge'));
            exit;
        }

        $response = $client->test_connection();
        if (is_wp_error($response)) {
            self::store_notice([
                'type' => 'error',
                'message' => 'Dismal responde, pero la integracion WooCommerce fallo: ' . $response->get_error_message(),
            ]);
        } else {
            self::store_notice([
                'type' => 'success',
                'message' => 'Conexion correcta con Dismal. Clientes disponibles: ' . (int) ($response['totalElements'] ?? 0) . '.',
            ]);
        }

        wp_safe_redirect(admin_url('admin.php?page=dismal-bridge'));
        exit;
    }

    private static function render_notice(): void
    {
        $notice = self::pull_notice();
        if (! is_array($notice)) {
            return;
        }

        $type = (string) ($notice['type'] ?? 'success');
        $class = $type === 'error' ? 'notice notice-error' : 'notice notice-success';
        echo '<div class="' . esc_attr($class) . '"><p>' . esc_html((string) ($notice['message'] ?? '')) . '</p></div>';
    }

    private static function store_notice(array $notice): void
    {
        set_transient(self::NOTICE_TRANSIENT_PREFIX . get_current_user_id(), $notice, MINUTE_IN_SECONDS * 10);
    }

    private static function pull_notice(): ?array
    {
        $key = self::NOTICE_TRANSIENT_PREFIX . get_current_user_id();
        $notice = get_transient($key);
        if ($notice !== false) {
            delete_transient($key);
        }

        return is_array($notice) ? $notice : null;
    }
}
