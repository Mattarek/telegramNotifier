import { Alert, Box, CircularProgress } from '@mui/material';

export function Loading() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
      <CircularProgress />
    </Box>
  );
}

export function ErrorBox({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : String(error);
  return (
    <Alert severity="error">
      Nie udało się pobrać danych: {message}. Czy backend działa na porcie 8080?
    </Alert>
  );
}
