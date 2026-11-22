import { AppBar, Box, Container, Tab, Tabs, Toolbar, Typography } from '@mui/material';
import { Link, Route, Routes, useLocation } from 'react-router';
import { RunButton } from './components/RunButton';
import { DashboardPage } from './pages/DashboardPage';
import { StocksPage } from './pages/StocksPage';
import { InstrumentPage } from './pages/InstrumentPage';
import { TradingPage } from './pages/TradingPage';
import { LearningPage } from './pages/LearningPage';
import { InstrumentsPage } from './pages/InstrumentsPage';
import { RunsPage } from './pages/RunsPage';

const tabs = [
  { path: '/', label: 'Pulpit' },
  { path: '/akcje', label: 'Akcje' },
  { path: '/trading', label: 'Trading' },
  { path: '/nauka', label: 'Nauka i wnioski' },
  { path: '/instrumenty', label: 'Instrumenty' },
  { path: '/przebiegi', label: 'Przebiegi' },
];

function currentTab(pathname: string): string {
  const match = tabs
    .filter((tab) => tab.path !== '/')
    .find((tab) => pathname.startsWith(tab.path));
  return match ? match.path : '/';
}

export function App() {
  const location = useLocation();
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky" color="default" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            MarketMind
          </Typography>
          <Tabs value={currentTab(location.pathname)} sx={{ flexGrow: 1 }} variant="scrollable">
            {tabs.map((tab) => (
              <Tab key={tab.path} value={tab.path} label={tab.label} component={Link} to={tab.path} />
            ))}
          </Tabs>
          <RunButton />
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ py: 3, flexGrow: 1 }}>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/akcje" element={<StocksPage />} />
          <Route path="/akcje/:id" element={<InstrumentPage mode="INVEST" />} />
          <Route path="/trading" element={<TradingPage />} />
          <Route path="/trading/:id" element={<InstrumentPage mode="TRADE" />} />
          <Route path="/nauka" element={<LearningPage />} />
          <Route path="/instrumenty" element={<InstrumentsPage />} />
          <Route path="/przebiegi" element={<RunsPage />} />
        </Routes>
      </Container>

      <Box component="footer" sx={{ py: 2, px: 3, borderTop: 1, borderColor: 'divider' }}>
        <Typography variant="caption" color="text.secondary">
          Narzędzie do własnej analizy, nie porada inwestycyjna. Predykcje to prawdopodobieństwa, nie pewniki - oceniaj je
          po kalibracji na dziesiątkach przypadków, nie po jednym dniu. Dane Yahoo Finance i Google News mogą być opóźnione
          lub niepełne.
        </Typography>
      </Box>
    </Box>
  );
}
