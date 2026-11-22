import { Alert, Card, CardActionArea, CardContent, Stack, Typography } from '@mui/material';
import { Link } from 'react-router';
import { useTrading } from '../api/client';
import { ErrorBox, Loading } from '../components/Loading';
import { Cards, Section } from '../components/Section';
import { StatTile } from '../components/StatTile';
import { ReportCard } from '../components/ReportCard';
import { PredictionsTable } from '../components/PredictionsTable';
import { CalibrationChart } from '../components/CalibrationChart';
import { Pct } from '../components/Pct';
import { num, prob, sourceLabel } from '../format';

export function TradingPage() {
  const { data, isLoading, error } = useTrading();
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorBox error={error} />;

  const ai = data.calibration.find((c) => c.source === 'AI' && c.horizonDays === 5);
  const baseline = data.calibration.find((c) => c.source === 'BASELINE' && c.horizonDays === 5);

  return (
    <Stack spacing={3}>
      <Alert severity="info" variant="outlined">
        Trading krótkoterminowy jest najtrudniejszy do przewidzenia - ruchy 1–5 sesji to w dużej części szum. Oceniaj
        setupy po średnim wyniku w R na wielu transakcjach, a nie po pojedynczych trafieniach. Symulacja zakłada wejście
        na otwarciu kolejnej sesji i nie uwzględnia prowizji ani poślizgu.
      </Alert>

      <Cards min={220}>
        {data.stats.map((stat) => (
          <StatTile
            key={stat.source}
            label={`${sourceLabel(stat.source)} - wynik transakcji`}
            value={stat.trades > 0 ? `${num(stat.averageR)}R / transakcję` : '–'}
            hint={`${stat.trades} transakcji · skuteczność ${prob(stat.winRate)} · łącznie ${num(stat.totalR)}R`}
          />
        ))}
      </Cards>

      <Section title="Setupy na dziś" subtitle="Najnowsza analiza tradingowa dla instrumentów z włączonym tradingiem">
        <Cards min={360}>
          {data.setups.map(({ instrument, snapshot, aiReport, baselineReport }) => (
            <Card key={instrument.id} variant="outlined">
              <CardActionArea component={Link} to={`/trading/${instrument.id}`}>
                <CardContent>
                  <Typography variant="h6">
                    {instrument.symbol}{' '}
                    <Typography component="span" variant="body2" color="text.secondary">
                      {num(snapshot?.close)} · 1d <Pct value={snapshot?.return1dPct} /> · ATR {num(snapshot?.atrPct)}%
                    </Typography>
                  </Typography>
                  <ReportCard report={aiReport ?? baselineReport} compact />
                </CardContent>
              </CardActionArea>
            </Card>
          ))}
        </Cards>
      </Section>

      <Section title="Otwarte transakcje (papierowe)">
        <PredictionsTable predictions={data.openTrades} showSymbol showTrade emptyText="Brak otwartych setupów." />
      </Section>

      <Section title="Zamknięte transakcje">
        <PredictionsTable predictions={data.closedTrades} showSymbol showTrade emptyText="Jeszcze nic nie zostało zamknięte." />
      </Section>

      <Section title="Kalibracja - trading 5 dni" subtitle="Czy pewność modelu pokrywa się z trafnością">
        <CalibrationChart ai={ai} baseline={baseline} />
      </Section>
    </Stack>
  );
}
