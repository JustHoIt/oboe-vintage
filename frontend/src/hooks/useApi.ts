import { useQuery, useMutation, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import { apiClient } from '@/lib/api';
import { ApiResponse, ApiError } from '@/types';

// GET 요청을 위한 커스텀 훅
export function useApiQuery<T>(
  queryKey: string[],
  url: string,
  options?: Omit<UseQueryOptions<T, ApiError>, 'queryKey' | 'queryFn'>
) {
  return useQuery<T, ApiError>({
    queryKey,
    queryFn: async () => {
      const response = await apiClient.get<ApiResponse<T>>(url);
      return response.data.data;
    },
    ...options,
  });
}

// POST/PUT/DELETE 요청을 위한 커스텀 훅
export function useApiMutation<TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<ApiResponse<TData>>,
  options?: UseMutationOptions<ApiResponse<TData>, ApiError, TVariables>
) {
  return useMutation<ApiResponse<TData>, ApiError, TVariables>({
    mutationFn,
    ...options,
  });
}
