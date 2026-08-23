/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          950: '#05070c',
          900: '#080c14',
          850: '#0d131f',
          800: '#111827',
          750: '#161f32',
          700: '#1f293d',
          600: '#334155',
        },
        pulse: {
          cyan: '#00f2fe',
          blue: '#4facfe',
          purple: '#8b5cf6',
          neonGreen: '#10b981',
          danger: '#f43f5e',
          amber: '#f59e0b',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      boxShadow: {
        'glow-cyan': '0 0 25px -5px rgba(0, 242, 254, 0.25)',
        'glow-purple': '0 0 25px -5px rgba(139, 92, 246, 0.25)',
        'glow-green': '0 0 20px -4px rgba(16, 185, 129, 0.3)',
        'glow-rose': '0 0 20px -4px rgba(244, 63, 94, 0.3)',
        'glass-panel': '0 8px 32px 0 rgba(0, 0, 0, 0.45)',
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'spin-slow': 'spin 3s linear infinite',
      },
    },
  },
  plugins: [],
}
