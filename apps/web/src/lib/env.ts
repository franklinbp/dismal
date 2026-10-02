export const getBaseApiUrl = () => {
  const envUrl = process.env.BASE_API_URL || process.env.NEXT_PUBLIC_BASE_API_URL || "";
  if (envUrl) return envUrl;
  if (process.env.NODE_ENV !== "production") {
    return "http://localhost:8080";
  }
  return "";
};
