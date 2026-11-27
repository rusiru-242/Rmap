import { useEffect, useState, type FormEvent } from 'react';
import { adminApi, AdminApiError } from './api/adminApi';
import type {
  AdminOverview,
  AdminUserSummary,
  AuditLogRecord,
  EmailLogRecord,
  PaymentRecord,
  SubscriptionRecord,
} from './types/admin';

type Tab = 'overview' | 'users' | 'subscriptions' | 'payments' | 'audit' | 'emails';

export function App() {
  const [adminEmail, setAdminEmail] = useState<string | null>(null);
  const [emailInput, setEmailInput] = useState('');
  const [passwordInput, setPasswordInput] = useState('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [tab, setTab] = useState<Tab>('overview');
  const [dataError, setDataError] = useState<string | null>(null);
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [subscriptions, setSubscriptions] = useState<SubscriptionRecord[]>([]);
  const [payments, setPayments] = useState<PaymentRecord[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLogRecord[]>([]);
  const [emailLogs, setEmailLogs] = useState<EmailLogRecord[]>([]);

  async function handleLogin(e: FormEvent) {
    e.preventDefault();
    setAuthError(null);
    setSubmitting(true);
    try {
      const res = await adminApi.login(emailInput.trim(), passwordInput);
      if (res.user.role !== 'ADMIN') {
        setAuthError('Access denied: ADMIN role required for the Private Admin Portal.');
        setSubmitting(false);
        return;
      }
      adminApi.setToken(res.accessToken);
      setAdminEmail(res.user.email);
    } catch (err) {
      setAuthError(err instanceof AdminApiError ? err.message : 'Unable to reach Rmap backend.');
    } finally {
      setSubmitting(false);
    }
  }

  function handleLogout() {
    adminApi.setToken(null);
    setAdminEmail(null);
    setOverview(null);
  }

  async function loadTab(currentTab: Tab) {
    setDataError(null);
    try {
      if (currentTab === 'overview') setOverview(await adminApi.getOverview());
      if (currentTab === 'users') setUsers((await adminApi.getUsers()).content);
      if (currentTab === 'subscriptions') setSubscriptions((await adminApi.getSubscriptions()).content);
      if (currentTab === 'payments') setPayments((await adminApi.getPayments()).content);
      if (currentTab === 'audit') setAuditLogs((await adminApi.getAuditLogs()).content);
      if (currentTab === 'emails') setEmailLogs((await adminApi.getEmailLogs()).content);
    } catch (err) {
      setDataError(err instanceof AdminApiError ? err.message : 'Failed to load admin data.');
    }
  }

  useEffect(() => {
    if (adminEmail) {
      void loadTab(tab);
    }
  }, [adminEmail, tab]);

  if (!adminEmail) {
    return (
      <div className="login-wrap">
        <form className="card login-card" onSubmit={handleLogin}>
          <h2 style={{ margin: '0 0 4px', fontSize: 16 }}>Rmap Private Admin Portal</h2>
          <p style={{ margin: '0 0 16px', color: 'var(--text-muted)' }}>
            Restricted administrative console. All actions are verified and audited by the backend.
          </p>
          {authError && <div className="error-box" role="alert">{authError}</div>}
          <div style={{ marginBottom: 12 }}>
            <label htmlFor="admin-email" className="metric-label">Admin Email</label>
            <input
              id="admin-email"
              className="input"
              type="email"
              required
              value={emailInput}
              onChange={(e) => setEmailInput(e.target.value)}
              placeholder="admin@rmap.local"
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label htmlFor="admin-password" className="metric-label">Password</label>
            <input
              id="admin-password"
              className="input"
              type="password"
              required
              value={passwordInput}
              onChange={(e) => setPasswordInput(e.target.value)}
            />
          </div>
          <button className="btn" type="submit" disabled={submitting} style={{ width: '100%' }}>
            {submitting ? 'Signing in...' : 'Sign in as Administrator'}
          </button>
        </form>
      </div>
    );
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">Rmap Admin</div>
        {([
          ['overview', 'System Health'],
          ['users', 'Users'],
          ['subscriptions', 'Trials & Subscriptions'],
          ['payments', 'Payments'],
          ['audit', 'Audit Logs'],
          ['emails', 'Email Delivery Logs'],
        ] as [Tab, string][]).map(([id, label]) => (
          <button
            key={id}
            className={`nav-btn ${tab === id ? 'active' : ''}`}
            onClick={() => setTab(id)}
          >
            {label}
          </button>
        ))}
        <div style={{ marginTop: 'auto', paddingTop: 12, borderTop: '1px solid var(--border)' }}>
          <div style={{ fontSize: 11.5, color: 'var(--text-muted)', marginBottom: 8 }}>{adminEmail}</div>
          <button className="btn-outline" onClick={handleLogout} style={{ width: '100%' }}>Sign out</button>
        </div>
      </aside>

      <main className="content">
        {dataError && <div className="error-box" role="alert">{dataError}</div>}

        {tab === 'overview' && (
          <>
            <h2 style={{ marginTop: 0 }}>System Health & Overview</h2>
            <div className="grid-4">
              <div className="card">
                <div className="metric-label">Service Status</div>
                <div className="metric-value">{overview?.serviceStatus ?? '—'}</div>
              </div>
              <div className="card">
                <div className="metric-label">Users</div>
                <div className="metric-value">{overview?.totalUsers ?? '—'}</div>
              </div>
              <div className="card">
                <div className="metric-label">Subscriptions</div>
                <div className="metric-value">{overview?.totalSubscriptions ?? '—'}</div>
              </div>
              <div className="card">
                <div className="metric-label">Payment Provider</div>
                <div className="metric-value" style={{ fontSize: 15 }}>
                  {overview ? `${overview.paymentProvider} (${overview.paymentProviderConfigured ? 'active' : 'unconfigured'})` : '—'}
                </div>
              </div>
            </div>
          </>
        )}

        {tab === 'users' && (
          <div className="card">
            <h2 style={{ marginTop: 0 }}>Users</h2>
            <table>
              <thead>
                <tr>
                  <th>Email</th><th>Name</th><th>Role</th><th>Status</th><th>Action</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.email}</td>
                    <td>{u.fullName}</td>
                    <td>{u.role}</td>
                    <td>{u.status}</td>
                    <td>
                      <button
                        className="btn-outline"
                        onClick={async () => {
                          const next = u.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED';
                          await adminApi.updateUserStatus(u.id, next);
                          await loadTab('users');
                        }}
                      >
                        {u.status === 'SUSPENDED' ? 'Reactivate' : 'Suspend'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {tab === 'subscriptions' && (
          <div className="card">
            <h2 style={{ marginTop: 0 }}>Trials & Subscriptions</h2>
            <table>
              <thead>
                <tr>
                  <th>User ID</th><th>Plan</th><th>Status</th><th>Trial Ends</th><th>Action</th>
                </tr>
              </thead>
              <tbody>
                {subscriptions.map((s) => (
                  <tr key={s.id}>
                    <td>{s.userId}</td>
                    <td>{s.planCode}</td>
                    <td>{s.status}</td>
                    <td>{s.trialEndsAt ?? '—'}</td>
                    <td>
                      <button
                        className="btn-outline"
                        onClick={async () => {
                          await adminApi.extendTrial(s.userId, 24);
                          await loadTab('subscriptions');
                        }}
                      >
                        +24h Trial
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {tab === 'payments' && (
          <div className="card">
            <h2 style={{ marginTop: 0 }}>Verified Payment History</h2>
            <table>
              <thead>
                <tr><th>Event ID</th><th>User ID</th><th>Provider</th><th>Plan</th><th>Amount</th><th>Status</th></tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td>{p.providerEventId}</td>
                    <td>{p.userId}</td>
                    <td>{p.provider}</td>
                    <td>{p.planCode}</td>
                    <td>{(p.amountMinor / 100).toFixed(2)} {p.currency}</td>
                    <td>{p.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {tab === 'audit' && (
          <div className="card">
            <h2 style={{ marginTop: 0 }}>Audit Logs</h2>
            <table>
              <thead>
                <tr><th>Time</th><th>Actor</th><th>Action</th><th>Target</th></tr>
              </thead>
              <tbody>
                {auditLogs.map((a) => (
                  <tr key={a.id}>
                    <td>{a.createdAt}</td>
                    <td>{a.actorEmail || a.actorId}</td>
                    <td>{a.action}</td>
                    <td>{a.targetType}:{a.targetId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {tab === 'emails' && (
          <div className="card">
            <h2 style={{ marginTop: 0 }}>Email Delivery Logs</h2>
            <table>
              <thead>
                <tr><th>Time</th><th>Recipient</th><th>Template</th><th>Status</th><th>Error</th></tr>
              </thead>
              <tbody>
                {emailLogs.map((e) => (
                  <tr key={e.id}>
                    <td>{e.sentAt}</td>
                    <td>{e.recipient}</td>
                    <td>{e.templateType}</td>
                    <td>{e.status}</td>
                    <td>{e.errorMessage ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
