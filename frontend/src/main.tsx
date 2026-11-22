import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { App } from './App';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
  },
});

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: { main: '#5b9cff' },
    secondary: { main: '#b48cff' },
    background: { default: '#0f1218', paper: '#161a22' },
  },
  shape: { borderRadius: 10 },
  typography: { fontFamily: 'Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif' },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ThemeProvider>
    </QueryClientProvider>
  </StrictMode>,
);
