import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/app/**/*.{ts,tsx}",
    "./src/components/**/*.{ts,tsx}",
    "./src/lib/**/*.{ts,tsx}"
  ],
  theme: {
    extend: {
      fontFamily: {
        display: ["Fraunces", "Georgia", "serif"],
        body: ["Plus Jakarta Sans", "Segoe UI", "sans-serif"],
        mono: ["ui-monospace", "SFMono-Regular", "Menlo", "monospace"]
      },
      colors: {
        ink: "#1a2547",
        paper: "#ffffff",
        slate: {
          DEFAULT: "#33496a",
          50: "#1a2547",
          100: "#24345d",
          200: "#33496a",
          300: "#587194",
          400: "#7791b0",
          500: "#a6b4c7",
          600: "#c4cedb",
          700: "#dbe2eb",
          800: "#ebf0f6",
          900: "#f5f7fb",
          950: "#fcfaf6"
        },
        smoke: "#d8e4f0",
        accent: "#1db4e7",
        accentDeep: "#2b7bbb"
      },
      boxShadow: {
        glow: "0 24px 80px rgba(26, 37, 71, 0.12)",
        panel: "0 18px 44px rgba(26, 37, 71, 0.08)"
      }
    }
  },
  plugins: []
};

export default config;
