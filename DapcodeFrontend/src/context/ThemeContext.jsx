import React, { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext();

export function ThemeProvider({ children }) {
  // Default to 'light' theme as requested by the user
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('dapcode_theme') || 'light';
  });

  useEffect(() => {
    localStorage.setItem('dapcode_theme', theme);
    // Bootstrap 5.3 color modes
    document.documentElement.dataset.bsTheme = theme;
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const isDark = theme === 'dark';

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, isDark }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    return {
      theme: 'light',
      toggleTheme: () => {},
      isDark: false,
    };
  }
  return context;
}
