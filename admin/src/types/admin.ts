export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  timestamp: string;
  requestId: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AdminOverview {
  totalUsers: number;
  totalSubscriptions: number;
  totalPayments: number;
  totalAuditLogs: number;
  totalEmailLogs: number;
  paymentProvider: string;
  paymentProviderConfigured: boolean;
  serviceStatus: string;
  timestamp: string;
}

export interface AdminUserSummary {
  id: string;
  email: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED';
  emailVerified: boolean;
  createdAt: string | null;
  lastLoginAt: string | null;
}

export interface SubscriptionRecord {
  id: string;
  userId: string;
  planCode: string;
  status: 'TRIALING' | 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
  trialEndsAt: string | null;
  currentPeriodEnd: string | null;
  providerName: string | null;
}

export interface PaymentRecord {
  id: string;
  userId: string;
  provider: string;
  providerEventId: string;
  amountMinor: number;
  currency: string;
  status: string;
  planCode: string;
  createdAt: string;
}

export interface AuditLogRecord {
  id: string;
  actorId: string;
  actorEmail: string;
  action: string;
  targetType: string;
  targetId: string;
  createdAt: string;
}

export interface EmailLogRecord {
  id: string;
  recipient: string;
  subject: string;
  templateType: string;
  status: string;
  errorMessage: string | null;
  sentAt: string;
}
