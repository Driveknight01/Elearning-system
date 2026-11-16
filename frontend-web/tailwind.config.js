/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        quizlet: {
          blue: '#4255FF',
          darkBlue: '#2E3856',
          bg: '#F6F7FB',
          card: '#FFFFFF',
          textDark: '#1A1D28',
          textMuted: '#586380',
          yellow: '#FFCD1F',
          green: '#23B26D',
          red: '#FF725B',
          purple: '#9333EA',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
