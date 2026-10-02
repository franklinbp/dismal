import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export type User = {
  id: string;
  email: string;
  role: "ADMIN" | "MANAGER" | "USER" | "OPERATOR" | "CUSTOMER";
};

interface SessionState {
  user: User | null;
  setUser: (user: User) => void;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      user: null,
      setUser: (user: User) => set({ user }),
      clearSession: () => set({ user: null }),
    }),
    {
      name: 'session-storage', // name of the item in storage (must be unique)
      storage: createJSONStorage(() => localStorage), // (optional) by default, 'localStorage' is used
    }
  )
);
