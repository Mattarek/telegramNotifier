import { Button, CircularProgress, Snackbar, Tooltip } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { useState } from 'react';
import { useDashboard, useRunPipeline } from '../api/client';

export function RunButton() {
  const dashboard = useDashboard();
  const run = useRunPipeline();
  const [message, setMessage] = useState<string | null>(null);
  const running = dashboard.data?.pipelineRunning || run.isPending;

  const start = () =>
    run.mutate(undefined, {
      onSuccess: () => setMessage('Analiza uruchomiona - wyniki pojawią się za kilka minut.'),
      onError: (error) => setMessage(`Nie udało się uruchomić: ${error.message}`),
    });

  return (
    <>
      <Tooltip title="Pobierz notowania i newsy, rozlicz stare tezy, wyciągnij wnioski i zrób nowe analizy">
        <span>
          <Button
            variant="contained"
            onClick={start}
            disabled={running}
            startIcon={running ? <CircularProgress size={16} color="inherit" /> : <PlayArrowIcon />}
          >
            {running ? 'Analizuję…' : 'Uruchom analizę'}
          </Button>
        </span>
      </Tooltip>
      <Snackbar open={message !== null} autoHideDuration={5000} onClose={() => setMessage(null)} message={message} />
    </>
  );
}
