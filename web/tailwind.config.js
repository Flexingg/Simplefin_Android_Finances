/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        duo: {
          green: '#58CC02',
          'green-dark': '#46A302',
          blue: '#1CB0F6',
          'blue-dark': '#1899D6',
          gold: '#FFC800',
          'gold-dark': '#E5A500',
          red: '#FF4B4B',
          'red-dark': '#EA2B2B',
          dark: '#131F24',
          card: '#1B2A32',
          'card-dark': '#202F36',
          'card-shadow': '#142026'
        }
      }
    },
  },
  plugins: [],
}
