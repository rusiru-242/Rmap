import type {
  AdminOverview,
  AdminUserSummary,
  ApiEnvelope,
  AuditLogRecord,
  EmailLogRecord,
  PageResponse,
  PaymentRecord,
  SubscriptionRecord,
} from '../types/admin';

export class AdminApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export class AdminApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080') {
    this.baseUrl = baseUrl.replace(/\/$/, '');
  }

  setToken(token: string | null): void {
    this.token = token;
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(init.headers as Record<string, string> | undefined),
    };
    if (this.token) {
      headers.Authorization = `Bearer ${this.token}`;
    }

    const res = await fetch(`${this.baseUrl}${path}`, { ...init, headers });
    const text = await res.text();
    const json = text ? (JSON.parse(text) as Record<string, unknown>) : {};

    if (!res.ok) {
      throw new AdminApiError(
        res.status,
        String(json.code ?? json.title ?? 'API_ERROR'),
        String(json.detail ?? json.message ?? `Request failed (${res.status})`)
      );
    }

    return (json as unknown as ApiEnvelope<T>).data;
  }

  async login(email: string, password: string): Promise<{ accessToken: string; user: { email: string; role: string } }> {
    return this.request('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  }

  getOverview(): Promise<AdminOverview> {
    return this.request('/api/v1/admin/overview');
  }

  getUsers(): Promise<PageResponse<AdminUserSummary>> {
    return this.request('/api/v1/admin/users?page=0&size=50');
  }

  updateUserStatus(userId: string, status: AdminUserSummary['status']): Promise<AdminUserSummary> {
    return this.request(`/api/v1/admin/users/${userId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
  }

  getSubscriptions(): Promise<PageResponse<SubscriptionRecord>> {
    return this.request('/api/v1/admin/subscriptions?page=0&size=50');
  }

  extendTrial(userId: string, extraHours: number): Promise<SubscriptionRecord> {
    return this.request(`/api/v1/admin/users/${userId}/trial/extend`, {
      method: 'POST',
      body: JSON.stringify({ extraHours }),
    });
  }

  getPayments(): Promise<PageResponse<PaymentRecord>> {
    return this.request('/api/v1/admin/payments?page=0&size=50');
  }

  getAuditLogs(): Promise<PageResponse<AuditLogRecord>> {
    return this.request('/api/v1/admin/audit-logs?page=0&size=50');
  }

  getEmailLogs(): Promise<PageResponse<EmailLogRecord>> {
    return this.request('/api/v1/admin/email-logs?page=0&size=50');
  }
}

export const adminApi = new AdminApiClient();
