/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ['./app/**/*.{ts,tsx}', './src/**/*.{ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        primary: '#111827',
        surface: '#F5F6F8',
        card: '#FFFFFF',
        border: '#E5E7EB',
        accent: '#2563EB',
        success: '#16A34A',
        warning: '#D97706',
        danger: '#DC2626',
        muted: '#6B7280',
      },
      fontFamily: {
        display: ['System', 'sans-serif'],
        body: ['System', 'sans-serif'],
      },
      borderRadius: {
        DEFAULT: '10px',
        md: '10px',
        lg: '12px',
        xl: '12px',
      },
    },
  },
  plugins: [],
};
