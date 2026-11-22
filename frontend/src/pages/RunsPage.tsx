import { Accordion, AccordionDetails, AccordionSummary, Chip, Stack, Typography } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useRuns } from '../api/client';
import { ErrorBox, Loading } from '../components/Loading';
import { Section } from '../components/Section';
import { dateTime, num } from '../format';

const statusColor = { RUNNING: 'info', SUCCESS: 'success', PARTIAL: 'warning', FAILED: 'error' } as const;

export function RunsPage() {
  const { data, isLoading, error } = useRuns();
  if (isLoading) return <Loading />;
  if (error || !data) return <ErrorBox error={error} />;

  return (
    <Section title="Przebiegi analizy" subtitle="Log każdego uruchomienia - przydatny, gdy coś się nie pobrało">
      {data.length === 0 && <Typography color="text.secondary">Jeszcze nie było żadnego przebiegu.</Typography>}
      {data.map((run, index) => (
        <Accordion key={run.id} defaultExpanded={index === 0} disableGutters variant="outlined">
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Chip size="small" color={statusColor[run.status]} label={run.status} />
              <Typography variant="body2">
                #{run.id} · {run.trigger === 'MANUAL' ? 'ręcznie' : 'harmonogram'} · {dateTime(run.startedAt)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {run.inputTokens + run.outputTokens} tokenów · ~${num(run.estimatedCostUsd, 3)}
              </Typography>
            </Stack>
          </AccordionSummary>
          <AccordionDetails>
            <Typography component="pre" variant="body2" sx={{ m: 0, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 12 }}>
              {run.log}
            </Typography>
          </AccordionDetails>
        </Accordion>
      ))}
    </Section>
  );
}
