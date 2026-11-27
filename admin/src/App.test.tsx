import { describe, expect, it, vi, afterEach } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from './App';
import { adminApi } from './api/adminApi';

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe('Rmap Private Admin Portal', () => {
  it('renders restricted admin login gate when unauthenticated', () => {
    render(<App />);
    expect(screen.getByText('Rmap Private Admin Portal')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign in as Administrator/i })).toBeInTheDocument();
  });

  it('rejects non-ADMIN accounts at login gate', async () => {
    vi.spyOn(adminApi, 'login').mockResolvedValue({
      accessToken: 'user-jwt',
      user: { email: 'user@example.com', role: 'USER' },
    });

    render(<App />);
    fireEvent.change(screen.getByLabelText(/Admin Email/i), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'Secret123!' } });
    fireEvent.click(screen.getByRole('button', { name: /Sign in as Administrator/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/ADMIN role required/i);
    });
  });

  it('loads System Health overview when ADMIN signs in', async () => {
    vi.spyOn(adminApi, 'login').mockResolvedValue({
      accessToken: 'admin-jwt',
      user: { email: 'admin@rmap.local', role: 'ADMIN' },
    });
    vi.spyOn(adminApi, 'getOverview').mockResolvedValue({
      totalUsers: 4,
      totalSubscriptions: 4,
      totalPayments: 0,
      totalAuditLogs: 12,
      totalEmailLogs: 4,
      paymentProvider: 'unconfigured',
      paymentProviderConfigured: false,
      serviceStatus: 'UP',
      timestamp: '2026-11-27T10:00:00Z',
    });

    render(<App />);
    fireEvent.change(screen.getByLabelText(/Admin Email/i), { target: { value: 'admin@rmap.local' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'AdminPass123!' } });
    fireEvent.click(screen.getByRole('button', { name: /Sign in as Administrator/i }));

    await waitFor(() => {
      expect(screen.getByText('System Health & Overview')).toBeInTheDocument();
      expect(screen.getByText('UP')).toBeInTheDocument();
    });
  });
});
