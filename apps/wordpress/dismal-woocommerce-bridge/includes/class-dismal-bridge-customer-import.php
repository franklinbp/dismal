<?php

if (! defined('ABSPATH')) {
    exit;
}

final class Dismal_Bridge_Customer_Import
{
    private const META_DISMAL_USER_ID = '_dismal_user_id';
    private const META_DISMAL_CUSTOMER_TYPE = '_dismal_customer_type';
    private const META_DISMAL_ENABLED = '_dismal_enabled';
    private const META_DISMAL_LAST_SYNC_AT = '_dismal_last_sync_at';
    private const META_DISMAL_HAS_CREDIT = '_dismal_has_credit';
    private const META_DISMAL_CREDIT_LIMIT = '_dismal_credit_limit';
    private const META_DISMAL_CREDIT_USED = '_dismal_credit_used';
    private const META_DISMAL_CREDIT_DAYS = '_dismal_credit_days';
    private const NOTICE_TRANSIENT_PREFIX = 'dismal_bridge_import_notice_';

    private Dismal_Bridge_Api_Client $api_client;

    public static function register(Dismal_Bridge_Api_Client $api_client): void
    {
        $instance = new self($api_client);
        add_action('admin_post_dismal_bridge_import_customers', [$instance, 'handle_import']);
    }

    public static function render_panel(): void
    {
        if (! current_user_can('manage_woocommerce')) {
            return;
        }

        $notice = self::pull_notice();
        if (is_array($notice)) {
            $class = ! empty($notice['errors']) ? 'notice notice-warning' : 'notice notice-success';
            echo '<div class="' . esc_attr($class) . '"><p>';
            echo esc_html(
                sprintf(
                    'Importacion completada. Creados: %d. Actualizados: %d. Enlazados: %d. Omitidos: %d. Errores: %d.',
                    (int) ($notice['created'] ?? 0),
                    (int) ($notice['updated'] ?? 0),
                    (int) ($notice['linked'] ?? 0),
                    (int) ($notice['skipped'] ?? 0),
                    count($notice['errors'] ?? [])
                )
            );
            echo '</p>';
            if (! empty($notice['errors'])) {
                echo '<ul style="margin-left: 1.25rem; list-style: disc;">';
                foreach (array_slice((array) $notice['errors'], 0, 10) as $error) {
                    echo '<li>' . esc_html((string) $error) . '</li>';
                }
                echo '</ul>';
            }
            echo '</div>';
        }

        $nonce = wp_create_nonce('dismal_bridge_import_customers');
        $action = admin_url('admin-post.php');

        echo '<hr style="margin: 2rem 0;" />';
        echo '<h2>' . esc_html__('Importar clientes desde Dismal', 'dismal-woocommerce-bridge') . '</h2>';
        echo '<p>' . esc_html__('Crea o enlaza clientes existentes de Dismal en WooCommerce usando el email y el identificador maestro de Dismal. Esta accion no modifica contraseñas.', 'dismal-woocommerce-bridge') . '</p>';
        echo '<form method="post" action="' . esc_url($action) . '">';
        echo '<input type="hidden" name="action" value="dismal_bridge_import_customers" />';
        echo '<input type="hidden" name="_wpnonce" value="' . esc_attr($nonce) . '" />';
        submit_button(__('Importar clientes ahora', 'dismal-woocommerce-bridge'), 'secondary', 'submit', false);
        echo '</form>';
    }

    public function __construct(Dismal_Bridge_Api_Client $api_client)
    {
        $this->api_client = $api_client;
    }

    public function handle_import(): void
    {
        if (! current_user_can('manage_woocommerce')) {
            wp_die(esc_html__('You are not allowed to do this.', 'dismal-woocommerce-bridge'));
        }
        if (! wp_verify_nonce($_POST['_wpnonce'] ?? '', 'dismal_bridge_import_customers')) {
            wp_die(esc_html__('Invalid nonce.', 'dismal-woocommerce-bridge'));
        }

        $stats = [
            'created' => 0,
            'updated' => 0,
            'linked' => 0,
            'skipped' => 0,
            'errors' => [],
        ];

        try {
            $page = 0;
            $size = 100;

            do {
                $response = $this->api_client->fetch_customers($page, $size, true);
                if (is_wp_error($response)) {
                    throw new RuntimeException($response->get_error_message());
                }

                $content = is_array($response['content'] ?? null) ? $response['content'] : [];
                foreach ($content as $customer) {
                    try {
                        $result = $this->upsert_customer($customer);
                        if (isset($stats[$result])) {
                            $stats[$result]++;
                        }
                    } catch (Throwable $throwable) {
                        $stats['errors'][] = $throwable->getMessage();
                    }
                }

                $last = ! empty($response['last']);
                $page++;
            } while (! $last);
        } catch (Throwable $throwable) {
            $stats['errors'][] = $throwable->getMessage();
        }

        self::store_notice($stats);
        wp_safe_redirect(admin_url('admin.php?page=dismal-bridge'));
        exit;
    }

    private function upsert_customer(array $customer): string
    {
        $dismal_user_id = (string) ($customer['id'] ?? '');
        $email = sanitize_email((string) ($customer['email'] ?? ''));
        if ($dismal_user_id === '' || $email === '') {
            throw new RuntimeException('Cliente omitido por faltar ID o email.');
        }

        $user = $this->find_user_by_dismal_id($dismal_user_id);
        $mode = 'updated';

        if (! $user instanceof WP_User) {
            $user = get_user_by('email', $email);
            if ($user instanceof WP_User) {
                $mode = 'linked';
            }
        }

        if (! $user instanceof WP_User) {
            $user_id = wp_create_user(
                $this->generate_username_from_email($email),
                wp_generate_password(24, true, true),
                $email
            );
            if (is_wp_error($user_id)) {
                throw new RuntimeException($user_id->get_error_message());
            }
            $user = get_user_by('id', $user_id);
            if (! $user instanceof WP_User) {
                throw new RuntimeException('No se pudo crear el usuario de WooCommerce.');
            }
            $mode = 'created';
        }

        $this->sync_user_profile($user, $customer, $email, $dismal_user_id);
        return $mode;
    }

    private function sync_user_profile(WP_User $user, array $customer, string $email, string $dismal_user_id): void
    {
        if ($user->user_email !== $email && ! email_exists($email)) {
            wp_update_user([
                'ID' => $user->ID,
                'user_email' => $email,
            ]);
        }

        $firstname = sanitize_text_field((string) ($customer['firstname'] ?? ''));
        $lastname = sanitize_text_field((string) ($customer['lastname'] ?? ''));
        $phone = sanitize_text_field((string) ($customer['phone'] ?? ''));
        $tax_id = sanitize_text_field((string) ($customer['taxId'] ?? ''));
        $billing_email = sanitize_email((string) ($customer['billingEmail'] ?? $email));
        $customer_type = sanitize_text_field((string) ($customer['customerType'] ?? 'FINAL'));
        $has_credit = ! empty($customer['hasCredit']) ? '1' : '0';
        $credit_limit = isset($customer['creditLimit']) ? (string) $customer['creditLimit'] : '0';
        $credit_used = isset($customer['creditUsed']) ? (string) $customer['creditUsed'] : '0';
        $credit_days = isset($customer['creditDays']) ? (string) $customer['creditDays'] : '0';
        $enabled = ! empty($customer['enabled']) ? '1' : '0';

        if (! in_array('customer', (array) $user->roles, true)) {
            $user->add_role('customer');
        }
        if ($customer_type === 'DISTRIBUTOR' && get_role('wholesale_customer')) {
            $user->add_role('wholesale_customer');
        }

        update_user_meta($user->ID, 'first_name', $firstname);
        update_user_meta($user->ID, 'last_name', $lastname);
        update_user_meta($user->ID, 'billing_first_name', $firstname);
        update_user_meta($user->ID, 'billing_last_name', $lastname);
        update_user_meta($user->ID, 'billing_email', $billing_email);
        update_user_meta($user->ID, 'billing_phone', $phone);
        if ($tax_id !== '') {
            update_user_meta($user->ID, 'billing_cedula', $tax_id);
            update_user_meta($user->ID, '_billing_cedula', $tax_id);
        }

        update_user_meta($user->ID, self::META_DISMAL_USER_ID, $dismal_user_id);
        update_user_meta($user->ID, self::META_DISMAL_CUSTOMER_TYPE, $customer_type);
        update_user_meta($user->ID, self::META_DISMAL_ENABLED, $enabled);
        update_user_meta($user->ID, self::META_DISMAL_HAS_CREDIT, $has_credit);
        update_user_meta($user->ID, self::META_DISMAL_CREDIT_LIMIT, $credit_limit);
        update_user_meta($user->ID, self::META_DISMAL_CREDIT_USED, $credit_used);
        update_user_meta($user->ID, self::META_DISMAL_CREDIT_DAYS, $credit_days);
        update_user_meta($user->ID, self::META_DISMAL_LAST_SYNC_AT, current_time('mysql'));
    }

    private function find_user_by_dismal_id(string $dismal_user_id): ?WP_User
    {
        $users = get_users([
            'meta_key' => self::META_DISMAL_USER_ID,
            'meta_value' => $dismal_user_id,
            'number' => 1,
            'count_total' => false,
        ]);

        if (! empty($users[0]) && $users[0] instanceof WP_User) {
            return $users[0];
        }

        return null;
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
