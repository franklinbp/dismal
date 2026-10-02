/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DISMAL_API_URL?: string;
  readonly VITE_DISMAL_DEFAULT_COUNTRY?: "EC" | "PE";
  readonly VITE_WHATSAPP_EC?: string;
  readonly VITE_WHATSAPP_PE?: string;
  readonly VITE_TELEGRAM_EC?: string;
  readonly VITE_TELEGRAM_PE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
