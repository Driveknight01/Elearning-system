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
        quizlet: {
          blue: '#4255FF',
          blueHover: '#3545E0',
          darkBlue: '#2E3856',
          bg: '#F6F7FB',
          bgHover: '#EDEFF4',
          card: '#FFFFFF',
          border: '#EDEFF4',
          borderStrong: '#D9DDE8',
          textDark: '#282E3E',
          textMuted: '#586380',
          textSubtle: '#939BB4',
          yellow: '#FFCD1F',
          green: '#23B26D',
          red: '#FF725B',
          purple: '#9333EA',
          darkCanvas: '#0A082D',
          darkSurface: '#202140',
          darkElevated: '#2E3856',
        },
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
