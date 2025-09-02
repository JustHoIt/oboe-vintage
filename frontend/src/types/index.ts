// API 응답 타입
export interface ApiResponse<T = any> {
  data: T;
  message?: string;
  status: number;
}

// 사용자 타입
export interface User {
  id: number;
  email: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

// 에러 타입
export interface ApiError {
  message: string;
  status: number;
  code?: string;
}
