import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#0B0F19",
        surface: {
          DEFAULT: "#121826",
          elevated: "#1A2235",
          glass: "rgba(18, 24, 38, 0.65)",
        },
        cyan: {
          electric: "#00E5FF",
          soft: "#67E8F9",
          muted: "#0891B2",
        },
        lime: {
          neon: "#A3E635",
          soft: "#BEF264",
          muted: "#65A30D",
        },
        space: {
          100: "#E2E8F0",
          200: "#94A3B8",
          300: "#64748B",
          400: "#475569",
          500: "#334155",
          600: "#1E293B",
          700: "#0F172A",
        },
      },
      fontFamily: {
        sans: ["var(--font-geist-sans)", "system-ui", "sans-serif"],
        mono: ["var(--font-geist-mono)", "ui-monospace", "monospace"],
      },
      boxShadow: {
        glow: "0 0 20px rgba(0, 229, 255, 0.25)",
        "glow-lime": "0 0 20px rgba(163, 230, 53, 0.3)",
        "glow-strong": "0 0 40px rgba(0, 229, 255, 0.4)",
        glass: "0 8px 32px rgba(0, 0, 0, 0.4)",
      },
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "grid-pattern":
          "linear-gradient(rgba(0,229,255,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(0,229,255,0.03) 1px, transparent 1px)",
      },
      animation: {
        pulse-neon: "pulse-neon 2s cubic-bezier(0.4, 0, 0.6, 1) infinite",
        float: "float 6s ease-in-out infinite",
      },
      keyframes: {
        "pulse-neon": {
          "0%, 100%": { boxShadow: "0 0 20px rgba(0, 229, 255, 0.4)" },
          "50%": { boxShadow: "0 0 40px rgba(0, 229, 255, 0.7)" },
        },
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-12px)" },
        },
      },
    },
  },
  plugins: [],
};

export default config;
