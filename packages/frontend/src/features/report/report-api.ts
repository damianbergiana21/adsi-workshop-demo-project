import { apiClient, withBasePath } from "@/lib/api-client";

export interface MonthlyRecordResponse {
  employeeId: string;
  employeeName: string;
  departmentName: string;
  workDays: number;
  totalWorkMinutes: number;
  totalOvertimeMinutes: number;
  absentDays: number;
}

export interface MonthlyReportResponse {
  month: string;
  records: MonthlyRecordResponse[];
}

function buildReportParams(month: string, departmentId?: string): string {
  const params = new URLSearchParams({ month });
  if (departmentId) {
    params.set("departmentId", departmentId);
  }
  return params.toString();
}

export function fetchMonthlyReport(
  month: string,
  departmentId?: string,
): Promise<MonthlyReportResponse> {
  return apiClient.get<MonthlyReportResponse>(
    `/api/reports/monthly?${buildReportParams(month, departmentId)}`,
  );
}

function getCsrfToken(): string | undefined {
  if (typeof document === "undefined") return undefined;
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : undefined;
}

async function downloadFile(url: string, filename: string): Promise<void> {
  const headers: Record<string, string> = {};
  const csrf = getCsrfToken();
  if (csrf) {
    headers["X-XSRF-TOKEN"] = csrf;
  }

  const response = await fetch(withBasePath(url), {
    credentials: "include",
    headers,
  });

  if (!response.ok) {
    throw new Error("ファイルのダウンロードに失敗しました");
  }

  const blob = await response.blob();
  const blobUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(blobUrl);
}

export function downloadCsv(month: string, departmentId?: string): Promise<void> {
  const url = `/api/reports/monthly/csv?${buildReportParams(month, departmentId)}`;
  return downloadFile(url, `monthly-report-${month}.csv`);
}

export function downloadPdf(month: string, departmentId?: string): Promise<void> {
  const url = `/api/reports/monthly/pdf?${buildReportParams(month, departmentId)}`;
  return downloadFile(url, `monthly-report-${month}.pdf`);
}
