import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { App } from './App';

describe('Rmap Private Admin Portal', () => {
  it('renders restricted admin login gate when unauthenticated', () => {
    render(<App />);
    expect(screen.getByText('Rmap Private Admin Portal')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign in as Administrator/i })).toBeInTheDocument();
  });
});
