import { Box, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useNavigate } from 'react-router';
import { useDashboard } from '../api/client';
import { ErrorBox, Loading } from '../components/Loading';
import { Section } from '../components/Section';
import { Pct } from '../components/Pct';
import { DirectionChip } from '../components/DirectionChip';
import { num } from '../format';

export function StocksPage() {
  const { data, isLoading, error } = useDashboard();
  const navigate = useNavigate();
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorBox error={error} />;

  return (
    <Section
      title="Akcje - towarzysz inwestycyjny"
      subtitle="Horyzont 5 i 20 sesji. Kierunek oznacza wynik WZGLĘDEM benchmarku (WIG20 / S&P 500), nie sam wzrost kursu."
    >
      <Box sx={{ overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Instrument</TableCell>
              <TableCell align="right">Kurs</TableCell>
              <TableCell align="right">1d</TableCell>
              <TableCell align="right">5d</TableCell>
              <TableCell align="right">20d</TableCell>
              <TableCell align="right">vs benchmark 20d</TableCell>
              <TableCell align="right">RSI</TableCell>
              <TableCell>AI</TableCell>
              <TableCell>Reguła bazowa</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data.instruments.map(({ instrument, snapshot, aiReport, baselineReport }) => (
              <TableRow key={instrument.id} hover sx={{ cursor: 'pointer' }} onClick={() => navigate(`/akcje/${instrument.id}`)}>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {instrument.symbol}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {instrument.name} · {instrument.market}
                  </Typography>
                </TableCell>
                <TableCell align="right">{num(snapshot?.close)}</TableCell>
                <TableCell align="right">
                  <Pct value={snapshot?.return1dPct} />
                </TableCell>
                <TableCell align="right">
                  <Pct value={snapshot?.return5dPct} />
                </TableCell>
                <TableCell align="right">
                  <Pct value={snapshot?.return20dPct} />
                </TableCell>
                <TableCell align="right">
                  <Pct value={snapshot?.excessReturn20dPct} />
                </TableCell>
                <TableCell align="right">{num(snapshot?.rsi14, 0)}</TableCell>
                <TableCell>
                  <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                    {aiReport?.predictions.map((p) => (
                      <DirectionChip key={p.id} direction={p.direction} mode="INVEST" confidence={p.confidence} horizonDays={p.horizonDays} />
                    )) ?? '–'}
                  </Stack>
                </TableCell>
                <TableCell>
                  <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                    {baselineReport?.predictions.map((p) => (
                      <DirectionChip key={p.id} direction={p.direction} mode="INVEST" horizonDays={p.horizonDays} />
                    )) ?? '–'}
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>
    </Section>
  );
}
