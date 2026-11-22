import { Typography, type TypographyProps } from '@mui/material';
import { pct } from '../format';

interface Props extends Omit<TypographyProps, 'children'> {
  value: number | null | undefined;
  digits?: number;
}

/** Percentage colored green/red depending on the sign. */
export function Pct({ value, digits = 2, ...rest }: Props) {
  const color = value === null || value === undefined ? 'text.secondary' : value > 0 ? 'success.main' : value < 0 ? 'error.main' : 'text.primary';
  return (
    <Typography component="span" variant="body2" color={color} sx={{ fontVariantNumeric: 'tabular-nums' }} {...rest}>
      {pct(value, digits)}
    </Typography>
  );
}
