import { Link, List, ListItem, ListItemText, Typography } from '@mui/material';
import type { News } from '../api/types';
import { dateTime } from '../format';

export function NewsList({ news, max = 20 }: { news: News[]; max?: number }) {
  if (news.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        Brak newsów.
      </Typography>
    );
  }
  return (
    <List dense disablePadding>
      {news.slice(0, max).map((item) => (
        <ListItem key={item.id} disableGutters>
          <ListItemText
            primary={
              <Link href={item.url} target="_blank" rel="noreferrer" underline="hover" color="inherit">
                {item.title}
              </Link>
            }
            secondary={`#${item.id} · ${dateTime(item.publishedAt)}${item.source ? ` · ${item.source}` : ''}`}
          />
        </ListItem>
      ))}
    </List>
  );
}
