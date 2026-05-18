interface ApiResponse<T = unknown> {
  code: number;
  data: T;
  msg: string;
}

const BASE_URL = '/api';

function getToken(): string | null {
  return localStorage.getItem('token');
}

async function request<T = unknown>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}${url}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    window.location.href = '/login';
    throw new Error('未登录或登录已过期');
  }

  const result: ApiResponse<T> = await response.json();

  if (result.code !== 0) {
    throw new Error(result.msg || '请求失败');
  }

  return result.data;
}

export function get<T = unknown>(url: string): Promise<T> {
  return request<T>(url, { method: 'GET' });
}

export function post<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request<T>(url, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  });
}

export function put<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request<T>(url, {
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined,
  });
}

export function del<T = unknown>(url: string): Promise<T> {
  return request<T>(url, { method: 'DELETE' });
}
