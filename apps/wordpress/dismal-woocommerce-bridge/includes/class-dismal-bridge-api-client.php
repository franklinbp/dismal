<?php

if (! defined('ABSPATH')) {
    exit;
}

final class Dismal_Bridge_Api_Client
{
    public function health_check()
    {
        $response = $this->request('GET', '/actuator/health');
        if (is_wp_error($response)) {
            return $response;
        }

        return $this->decode_json_response($response);
    }

    public function push_paid_order(array $payload)
    {
        $settings = Dismal_Bridge_Settings::get();

        if ($settings['api_base_url'] === '' || $settings['api_key'] === '') {
            return new WP_Error('dismal_bridge_not_configured', __('Dismal Bridge is not configured.', 'dismal-woocommerce-bridge'));
        }

        $response = $this->request(
            'POST',
            '/api/public/integrations/woocommerce/order-paid',
            [
                'headers' => [
                    'X-Dismal-Integration-Key' => $settings['api_key'],
                ],
                'body' => $payload,
            ]
        );

        if (is_wp_error($response)) {
            return $response;
        }

        return $this->decode_json_response($response);
    }

    public function authenticate_customer(string $email, string $password)
    {
        $response = $this->request(
            'POST',
            '/api/v1/auth/authenticate',
            [
                'body' => [
                    'email' => $email,
                    'password' => $password,
                ],
            ]
        );

        if (is_wp_error($response)) {
            return $response;
        }

        return $this->decode_json_response($response);
    }

    public function fetch_current_user(string $token)
    {
        $response = $this->request(
            'GET',
            '/api/v1/users/me',
            [
                'headers' => [
                    'Authorization' => 'Bearer ' . $token,
                ],
            ]
        );

        if (is_wp_error($response)) {
            return $response;
        }

        return $this->decode_json_response($response);
    }

    public function register_customer(array $payload)
    {
        $response = $this->request(
            'POST',
            '/api/v1/auth/register',
            [
                'body' => $payload,
            ]
        );

        if (is_wp_error($response)) {
            return $response;
        }

        return $this->decode_json_response($response);
    }

    public function fetch_customers(int $page = 0, int $size = 100, bool $enabled_only = true)
    {
        $settings = Dismal_Bridge_Settings::get();

        if ($settings['api_base_url'] === '' || $settings['api_key'] === '') {
            return new WP_Error('dismal_bridge_not_configured', __('Dismal Bridge is not configured.', 'dismal-woocommerce-bridge'));
        }

        $query = add_query_arg([
            'page' => max(0, $page),
            'size' => max(1, $size),
            'enabledOnly' => $enabled_only ? 'true' : 'false',
        ], '/api/public/integrations/woocommerce/customers');

        $response = $this->request(
            'GET',
            $query,
            [
                'headers' => [
                    'X-Dismal-Integration-Key' => $settings['api_key'],
                ],
            ]
        );

        if (is_wp_error($response)) {
            return $response;
        }

        return $this->decode_json_response($response);
    }

    public function test_connection()
    {
        return $this->fetch_customers(0, 1, true);
    }

    private function request(string $method, string $path, array $args = [])
    {
        $settings = Dismal_Bridge_Settings::get();
        if ($settings['api_base_url'] === '') {
            return new WP_Error('dismal_bridge_not_configured', __('Dismal Bridge is not configured.', 'dismal-woocommerce-bridge'));
        }

        $headers = [
            'Content-Type' => 'application/json',
            'Accept' => 'application/json',
        ];
        if (! empty($args['headers']) && is_array($args['headers'])) {
            $headers = array_merge($headers, $args['headers']);
        }

        $body = $args['body'] ?? null;
        $request_args = [
            'method' => strtoupper($method),
            'timeout' => 20,
            'headers' => $headers,
        ];
        if ($body !== null) {
            $request_args['body'] = wp_json_encode($body);
        }

        $response = wp_remote_request($settings['api_base_url'] . $path, $request_args);
        if (is_wp_error($response)) {
            return new WP_Error(
                $response->get_error_code(),
                $response->get_error_message() . ' Target: ' . $settings['api_base_url'] . $path,
                $response->get_error_data()
            );
        }

        return $response;
    }

    private function decode_json_response($response)
    {
        $status_code = wp_remote_retrieve_response_code($response);
        $body = wp_remote_retrieve_body($response);
        $decoded = json_decode($body, true);

        if ($status_code < 200 || $status_code >= 300) {
            $message = is_array($decoded) && ! empty($decoded['message']) ? $decoded['message'] : __('Unexpected Dismal API error.', 'dismal-woocommerce-bridge');
            return new WP_Error('dismal_bridge_api_error', $message . ' HTTP ' . $status_code, ['status_code' => $status_code, 'response_body' => $body]);
        }

        if (! is_array($decoded)) {
            return new WP_Error('dismal_bridge_invalid_response', __('Invalid response from Dismal API.', 'dismal-woocommerce-bridge'), ['response_body' => $body]);
        }

        return $decoded;
    }
}
