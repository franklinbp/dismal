export type UserRole = "ADMIN" | "MANAGER" | "USER" | "OPERATOR" | "CUSTOMER";

export const isAdmin = (role?: UserRole | null) => role === "ADMIN";

export const isManager = (role?: UserRole | null) => role === "MANAGER";

export const isAdminOrManager = (role?: UserRole | null) => isAdmin(role) || isManager(role);

export const isOperator = (role?: UserRole | null) => role === "OPERATOR";
