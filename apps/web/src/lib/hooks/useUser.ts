import useSWR from 'swr';
import { apiFetch } from '@/lib/api';

type UserProfile = {
  id: string;
  email: string;
  role: string;
};

export function useUser() {
  const { data, error, isLoading, mutate } = useSWR<UserProfile>('/api/auth/me', apiFetch);

  return {
    user: data,
    isLoading,
    isError: error,
    mutate,
  };
}
