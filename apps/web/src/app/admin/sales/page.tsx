"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { apiFetch } from "@/lib/api";
import AdminHeader from "@/components/admin/AdminHeader";
import AdminActionButton from "@/components/admin/AdminActionButton";
import { formatCurrency, formatDateTime, formatNumber } from "@/lib/format";
import { DownloadIcon, PlusIcon, RefreshIcon, UsersIcon } from "@/components/admin/icons";
import { type UserRole } from "@/lib/auth";
import { useAdminGuard } from "@/components/admin/useAdminGuard";

const paymentMethods = ["CASH", "CREDIT", "TRANSFER"] as const;
const saleStatuses = ["DRAFT", "CONFIRMED", "PAID", "CANCELLED"] as const;
const saleTypes = ["CASH", "CREDIT"] as const;
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function parsePaymentAmount(value: string) {
  const normalized = value.trim().replace(",", ".");
  const amount = Number(normalized);
  return Number.isFinite(amount) ? amount : null;
}

function resolvePaymentAccountId(value: string) {
  return uuidPattern.test(value) ? value : null;
}

function resolvePaymentMethod(value: string): (typeof paymentMethods)[number] {
  return paymentMethods.includes(value as (typeof paymentMethods)[number])
    ? (value as (typeof paymentMethods)[number])
    : "CASH";
}

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

type OutboxResponse = {
  id: string;
  eventType: string;
  aggregateId: string;
  status: string;
  attempts: number;
  nextAttemptAt: string | null;
  sentAt: string | null;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
};

type DeliveryLog = {
  id: string;
  outboxEventId: string;
  channel: "EMAIL" | "WHATSAPP";
  status: "SENT" | "DELIVERED" | "FAILED";
  error: string | null;
  createdAt: string;
  updatedAt: string;
};

type AdminUser = {
  id: string;
  firstname: string;
  lastname: string;
  email: string;
  phone: string | null;
  customerType?: "FINAL" | "DISTRIBUTOR";
  hasCredit: boolean;
  creditLimit: number;
  creditDays: number;
};

type AdminUserPayload = {
  firstname: string;
  lastname: string;
  email: string;
  phone: string | null;
  role: "CUSTOMER";
  country: "EC" | "PE";
  enabled: boolean;
  taxId: string | null;
  billingEmail: string;
  hasCredit: boolean;
  creditLimit: number;
  creditDays: number;
  password?: string | null;
};

type UserMe = {
  id: string;
  email: string;
  role: UserRole;
};

type Customer = {
  id: string;
  firstname: string;
  lastname: string;
  email: string;
  phone?: string;
  customerType?: "FINAL" | "DISTRIBUTOR";
  hasCredit: boolean;
  creditLimit: number;
  creditDays: number;
};

type Software = {
  id: string;
  name: string;
  price: number;
  ecFinalPrice?: number | null;
  ecDistributorPrice?: number | null;
  peFinalPrice?: number | null;
  peDistributorPrice?: number | null;
};

type StockSummary = {
  softwareId: string;
  softwareName: string;
  available: number;
  assigned: number;
};

type SaleItem = {
  softwareId: string;
  softwareName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
};

type Sale = {
  id: string;
  saleNumber?: string | null;
  clientId: string;
  saleType: string;
  country?: "EC" | "PE";
  currency?: "USD" | "PEN";
  status: string;
  total: number;
  paid: number;
  balance: number;
  createdAt: string;
  updatedAt: string;
  items: SaleItem[];
};

type Payment = {
  id: string;
  saleId: string;
  amount: number;
  method: string;
  reference: string | null;
  paymentAccountId?: string | null;
  paymentAccountName?: string | null;
  createdAt: string;
};

type PaymentAccount = {
  id: string;
  name: string;
  type: "CASH" | "BANK" | "WALLET" | "OTHER";
  currency: string;
  active: boolean;
  defaultAccount: boolean;
};

type SaleLicense = {
  id: string;
  licenseId: string;
  licenseKey: string;
  softwareId: string;
  softwareName: string;
  assignedAt: string;
};

type CreateItem = {
  softwareId: string;
  quantity: number;
  unitPrice: number;
  selectedLicenseIds: string[];
};

type AvailableLicense = {
  id: string;
  licenseKey: string;
  available: boolean;
  usedActivations: number;
  maxActivations: number;
  ownerEmail?: string | null;
};

type CrmLeadDraft = {
  id: string;
  name: string;
  email: string;
  phone: string;
  interest: string;
  customerType: "FINAL" | "DISTRIBUTOR";
  estimatedValue: string;
};

const defaultPage: PageResponse<unknown> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

function formatName(first?: string, last?: string) {
  return `${first || ""} ${last || ""}`.trim() || "-";
}

function splitFullName(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return { firstname: "Cliente", lastname: "CRM" };
  }
  if (parts.length === 1) {
    return { firstname: parts[0], lastname: "CRM" };
  }
  return { firstname: parts.slice(0, -1).join(" "), lastname: parts[parts.length - 1] || "CRM" };
}

function normalizePhone(value?: string | null) {
  return (value || "").replace(/\D/g, "");
}

function normalizeEmail(value?: string | null) {
  return (value || "").trim().toLowerCase();
}

function getStatusLabel(status: string) {
  switch (status) {
    case "DRAFT":
      return "Borrador";
    case "CONFIRMED":
      return "Confirmada";
    case "PAID":
      return "Pagada";
    case "CANCELLED":
      return "Cancelada";
    default:
      return status;
  }
}

function getStatusTone(status: string) {
  switch (status) {
    case "PAID":
      return "bg-emerald-50 text-emerald-700 border-emerald-200";
    case "CONFIRMED":
      return "bg-sky-50 text-sky-700 border-sky-200";
    case "DRAFT":
      return "bg-amber-50 text-amber-700 border-amber-200";
    case "CANCELLED":
      return "bg-red-50 text-red-700 border-red-200";
    default:
      return "bg-slate-50 text-slate-600 border-slate-200";
  }
}

function getSaleTypeLabel(type: string) {
  switch (type) {
    case "CASH":
      return "Contado";
    case "CREDIT":
      return "Crédito";
    default:
      return type;
  }
}

function getPaymentMethodLabel(method: string) {
  switch (method) {
    case "CASH":
      return "Contado";
    case "CREDIT":
    case "CARD":
      return "Credito";
    case "TRANSFER":
      return "Transferencia";
    default:
      return method;
  }
}

function resolveSegmentUnitPrice(
  product: Software | undefined,
  marketCode: "EC" | "PE",
  customerType: "FINAL" | "DISTRIBUTOR"
) {
  if (!product) return 0;
  if (marketCode === "EC" && customerType === "FINAL") {
    return Number(product.ecFinalPrice ?? product.price ?? 0);
  }
  if (marketCode === "EC" && customerType === "DISTRIBUTOR") {
    return Number(product.ecDistributorPrice ?? product.ecFinalPrice ?? product.price ?? 0);
  }
  if (marketCode === "PE" && customerType === "FINAL") {
    return Number(product.peFinalPrice ?? product.ecFinalPrice ?? product.price ?? 0);
  }
  return Number(
    product.peDistributorPrice
    ?? product.peFinalPrice
    ?? product.ecDistributorPrice
    ?? product.ecFinalPrice
    ?? product.price
    ?? 0
  );
}

export default function AdminSalesPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, loading, allowed } = useAdminGuard("ADMIN_MANAGER");
  const createSectionRef = useRef<HTMLDivElement | null>(null);
  const customerAutocompleteRef = useRef<HTMLDivElement | null>(null);
  const crmLeadAppliedRef = useRef<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [crmLeadDraft, setCrmLeadDraft] = useState<CrmLeadDraft | null>(null);

  const [sales, setSales] = useState<PageResponse<Sale>>(defaultPage as PageResponse<Sale>);
  const [selectedSale, setSelectedSale] = useState<Sale | null>(null);
  const selectedSaleRef = useRef<Sale | null>(null);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [paymentAccounts, setPaymentAccounts] = useState<PaymentAccount[]>([]);
  const [licenses, setLicenses] = useState<SaleLicense[]>([]);
  const [deliveryLogs, setDeliveryLogs] = useState<DeliveryLog[]>([]);
  const [deliveryEventId, setDeliveryEventId] = useState<string | null>(null);
  const [deliveryLastError, setDeliveryLastError] = useState<string | null>(null);
  const [confirmingSaleId, setConfirmingSaleId] = useState<string | null>(null);
  const [savingSale, setSavingSale] = useState(false);
  const [savingPayment, setSavingPayment] = useState(false);
  const [retryingDelivery, setRetryingDelivery] = useState(false);

  const [page, setPage] = useState(0);
  const pageSize = 25;

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Software[]>([]);
  const [stockMap, setStockMap] = useState<Record<string, StockSummary>>({});
  const [licenseOptionsBySoftware, setLicenseOptionsBySoftware] = useState<Record<string, AvailableLicense[]>>({});

  const [filters, setFilters] = useState({
    status: "",
    clientId: "",
    from: "",
    to: "",
  });
  const [searchTerm, setSearchTerm] = useState("");
  const [customerSearchTerm, setCustomerSearchTerm] = useState("");
  const [isCustomerMenuOpen, setIsCustomerMenuOpen] = useState(false);
  const [sortOrder, setSortOrder] = useState<"desc" | "asc">("desc");

  const [showCreateForm, setShowCreateForm] = useState(true);
  const [showCreateCustomerModal, setShowCreateCustomerModal] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editingSaleId, setEditingSaleId] = useState<string | null>(null);
  const [newSale, setNewSale] = useState({
    clientId: "",
    saleType: "CASH",
    marketCode: "EC" as "EC" | "PE",
    items: [{ softwareId: "", quantity: 1, unitPrice: 0, selectedLicenseIds: [] }] as CreateItem[],
  });
  const defaultDeliveryChannels = { email: true, whatsapp: true };
  const [newSaleChannels, setNewSaleChannels] = useState(defaultDeliveryChannels);
  const [newCustomer, setNewCustomer] = useState({
    firstname: "",
    lastname: "",
    email: "",
    phone: "",
    taxId: "",
  });
  const [saleDeliveryPrefs, setSaleDeliveryPrefs] = useState<
    Record<string, { email: boolean; whatsapp: boolean }>
  >({});

  const [paymentForm, setPaymentForm] = useState({
    amount: "",
    method: "CASH",
    reference: "",
    paymentAccountId: "",
    notifyClient: true,
  });

  const [toasts, setToasts] = useState<{ id: string; type: "success" | "error"; message: string }[]>(
    []
  );

  const pushToast = (type: "success" | "error", message: string) => {
    const id = `${Date.now()}-${Math.random()}`;
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 2800);
  };

  const validCreateItems = useMemo(() => {
    return newSale.items.filter(
      (item) => item.softwareId && item.quantity > 0 && Number.isFinite(item.unitPrice)
    );
  }, [newSale.items]);

  const saleTotal = useMemo(() => {
    return validCreateItems.reduce((acc, item) => acc + item.quantity * item.unitPrice, 0);
  }, [validCreateItems]);

  const canCreateSale = Boolean(newSale.clientId && validCreateItems.length > 0);

  const createStockOk = useMemo(() => {
    return validCreateItems.every((item) => {
      const stock = stockMap[item.softwareId];
      if (!stock) return false;
      return stock.available >= item.quantity;
    });
  }, [validCreateItems, stockMap]);

  const confirmStockOk = useMemo(() => {
    if (!selectedSale) return false;
    return selectedSale.items.every((item) => {
      const stock = stockMap[item.softwareId];
      if (!stock) return false;
      return stock.available >= item.quantity;
    });
  }, [selectedSale, stockMap]);

  const filteredCustomers = useMemo(() => {
    const term = customerSearchTerm.trim().toLowerCase();
    if (!term) {
      return customers;
    }
    return customers.filter((customer) => {
      const haystack = `${customer.firstname} ${customer.lastname} ${customer.email} ${customer.phone || ""}`.toLowerCase();
      return haystack.includes(term);
    });
  }, [customers, customerSearchTerm]);

  const customerOptions = useMemo(() => filteredCustomers.slice(0, 8), [filteredCustomers]);

  const selectedCustomer = useMemo(
    () => customers.find((customer) => customer.id === newSale.clientId) ?? null,
    [customers, newSale.clientId]
  );
  const selectedSaleClient = useMemo(
    () => customers.find((customer) => customer.id === selectedSale?.clientId) ?? null,
    [customers, selectedSale?.clientId]
  );
  const selectedCustomerType = selectedCustomer?.customerType ?? "FINAL";
  const applySegmentPricesToItems = useCallback(
    (
      items: CreateItem[],
      marketCode: "EC" | "PE",
      customerType: "FINAL" | "DISTRIBUTOR"
    ) =>
      items.map((item) => {
        const product = products.find((candidate) => candidate.id === item.softwareId);
        if (!product) {
          return item;
        }
        return {
          ...item,
          unitPrice: resolveSegmentUnitPrice(product, marketCode, customerType),
        };
      }),
    [products]
  );

  const selectSale = useCallback((sale: Sale | null) => {
    selectedSaleRef.current = sale;
    setSelectedSale(sale);
  }, []);

  const upsertSaleInList = useCallback((sale: Sale) => {
    setSales((prev) => ({
      ...prev,
      content: [sale, ...prev.content.filter((current) => current.id !== sale.id)].slice(0, pageSize),
      totalElements: prev.content.some((current) => current.id === sale.id)
        ? prev.totalElements
        : prev.totalElements + 1,
    }));
  }, [pageSize]);

  const loadSales = useCallback(async () => {
    const params = new URLSearchParams();
    if (filters.status) params.set("status", filters.status);
    if (filters.clientId) params.set("clientId", filters.clientId);
    if (filters.from) params.set("from", filters.from);
    if (filters.to) params.set("to", filters.to);
    params.set("country", newSale.marketCode);
    params.set("page", String(page));
    params.set("size", String(pageSize));

    const data = await apiFetch<PageResponse<Sale>>(`/api/backend/api/v1/sales?${params.toString()}`);
    setSales(data);
    const currentSelectedSale = selectedSaleRef.current;
    if (data.content.length > 0 && !currentSelectedSale) {
      selectSale(data.content[0]);
    } else if (currentSelectedSale && !data.content.some((sale) => sale.id === currentSelectedSale.id)) {
      selectSale(data.content[0] || null);
    }
  }, [filters, page, pageSize, selectSale, newSale.marketCode]);

  const loadAux = useCallback(async () => {
    try {
      const adminUsers = await apiFetch<PageResponse<AdminUser>>(
        `/api/backend/api/v1/admin/users?type=CLIENT&country=${newSale.marketCode}&page=0&size=200`
      );
      const mapped = adminUsers.content.map((adminUser) => ({
        id: adminUser.id,
        firstname: adminUser.firstname,
        lastname: adminUser.lastname,
        email: adminUser.email,
        phone: adminUser.phone || undefined,
        customerType: adminUser.customerType,
        hasCredit: adminUser.hasCredit,
        creditLimit: adminUser.creditLimit,
        creditDays: adminUser.creditDays,
      }));
      setCustomers(mapped);
    } catch {
      setCustomers([]);
    }

    try {
      const productData = await apiFetch<Software[]>("/api/backend/api/v1/store/products");
      setProducts(productData);
    } catch (err: any) {
      setProducts([]);
      pushToast("error", err?.message || "No se pudieron cargar productos.");
    }
  }, [newSale.marketCode]);

  const loadStock = useCallback(async () => {
    const stockData = await apiFetch<StockSummary[]>("/api/backend/api/v1/inventory/stock");
    const map: Record<string, StockSummary> = {};
    stockData.forEach((entry) => {
      map[entry.softwareId] = entry;
    });
    setStockMap(map);
  }, []);

  const loadPaymentAccounts = useCallback(async () => {
    try {
      const accounts = await apiFetch<PaymentAccount[]>("/api/backend/api/v1/payment-accounts");
      setPaymentAccounts(accounts);
      const defaultAccount = accounts.find((account) => account.defaultAccount) ?? accounts[0];
      if (defaultAccount) {
        setPaymentForm((prev) => ({
          ...prev,
          paymentAccountId: prev.paymentAccountId || defaultAccount.id,
        }));
      }
    } catch {
      setPaymentAccounts([]);
    }
  }, []);

  const loadLicenseOptions = useCallback(async (softwareId: string) => {
    if (!softwareId || licenseOptionsBySoftware[softwareId]) {
      return;
    }
    const response = await apiFetch<PageResponse<AvailableLicense>>(
      `/api/backend/api/v1/inventory/licenses?softwareId=${softwareId}&status=ACTIVE&page=0&size=100`
    );
    setLicenseOptionsBySoftware((prev) => ({
      ...prev,
      [softwareId]: response.content.filter((license) => license.available),
    }));
  }, [licenseOptionsBySoftware]);

  const loadSaleDetail = useCallback(async (sale: Sale) => {
    const safeFetch = async <T,>(fn: () => Promise<T>, fallback: T) => {
      try {
        return await fn();
      } catch (err: any) {
        if (err?.status === 404) {
          return fallback;
        }
        throw err;
      }
    };

    const [paymentsData, licensesData] = await Promise.all([
      safeFetch(() => apiFetch<Payment[]>(`/api/backend/api/v1/sales/${sale.id}/payments`), []),
      safeFetch(() => apiFetch<SaleLicense[]>(`/api/backend/api/v1/sales/${sale.id}/licenses`), []),
    ]);
    setPayments(paymentsData);
    setLicenses(licensesData);

    try {
      const outboxPage = await apiFetch<PageResponse<OutboxResponse>>(
        `/api/backend/api/v1/integrations/outbox?eventType=LICENSES_DELIVERED&aggregateId=${sale.id}&page=0&size=1`
      );
      const event = outboxPage.content[0];
      if (!event) {
        setDeliveryEventId(null);
        setDeliveryLogs([]);
        setDeliveryLastError(null);
        return;
      }
      setDeliveryEventId(event.id);
      setDeliveryLastError(event.lastError || null);
      const logs = await apiFetch<DeliveryLog[]>(
        `/api/backend/api/v1/integrations/notifications/templates/delivery-logs?outboxEventId=${event.id}`
      );
      setDeliveryLogs(logs);
    } catch {
      setDeliveryEventId(null);
      setDeliveryLogs([]);
      setDeliveryLastError(null);
    }
  }, []);

  const displaySales = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    const filtered = term
      ? sales.content.filter((sale) => {
          const client = customers.find((customer) => customer.id === sale.clientId);
          const clientText = `${client?.firstname || ""} ${client?.lastname || ""} ${client?.email || ""}`;
          const itemText = sale.items.map((item) => item.softwareName).join(" ");
          const haystack = `${clientText} ${itemText} ${sale.saleNumber ?? ""} ${sale.id}`.toLowerCase();
          return haystack.includes(term);
        })
      : sales.content;

    const sorted = [...filtered].sort((a, b) => {
      const left = new Date(a.createdAt).getTime();
      const right = new Date(b.createdAt).getTime();
      return sortOrder === "desc" ? right - left : left - right;
    });

    return sorted;
  }, [sales.content, customers, searchTerm, sortOrder]);

  const canModify = Boolean(selectedSale && selectedSale.status === "DRAFT");
  const canDeleteSale = Boolean(selectedSale);
  const canPay = Boolean(
    selectedSale &&
      selectedSale.status !== "CANCELLED" &&
      selectedSale.status !== "DRAFT" &&
      selectedSale.balance > 0
  );
  const isConfirmingSelectedSale = Boolean(selectedSale && confirmingSaleId === selectedSale.id);

  const statusCounts = useMemo(() => {
    return displaySales.reduce(
      (acc, sale) => {
        acc.total += 1;
        acc[sale.status] = (acc[sale.status] || 0) + 1;
        return acc;
      },
      { total: 0 } as Record<string, number>
    );
  }, [displaySales]);

  const todayRevenue = useMemo(() => {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    const end = new Date(start);
    end.setDate(end.getDate() + 1);
    return displaySales.reduce((acc, sale) => {
      const createdAt = new Date(sale.createdAt).getTime();
      if (sale.status === "CANCELLED") return acc;
      if (createdAt >= start.getTime() && createdAt < end.getTime()) {
        return acc + sale.total;
      }
      return acc;
    }, 0);
  }, [displaySales]);

  const deliveryStatus = useMemo(() => {
    const latest: Record<string, DeliveryLog> = {};
    deliveryLogs.forEach((log) => {
      const existing = latest[log.channel];
      if (!existing || new Date(log.updatedAt).getTime() > new Date(existing.updatedAt).getTime()) {
        latest[log.channel] = log;
      }
    });
    return {
      EMAIL: latest.EMAIL?.status || (deliveryEventId ? "PENDING" : "NOT_SENT"),
      WHATSAPP: latest.WHATSAPP?.status || (deliveryEventId ? "PENDING" : "NOT_SENT"),
    } as const;
  }, [deliveryLogs, deliveryEventId]);

  const deliveryTone = (status: string) => {
    switch (status) {
      case "DELIVERED":
        return "bg-emerald-100 text-emerald-700 border-emerald-200";
      case "SENT":
        return "bg-sky-100 text-sky-700 border-sky-200";
      case "FAILED":
        return "bg-red-100 text-red-700 border-red-200";
      case "PENDING":
        return "bg-amber-100 text-amber-700 border-amber-200";
      default:
        return "bg-slate-100 text-slate-600 border-slate-200";
    }
  };

  useEffect(() => {
    const boot = async () => {
      if (!allowed || !user) return;
      setError(null);
      try {
        await Promise.all([loadAux(), loadStock(), loadPaymentAccounts()]);
      } catch (err: any) {
        if (err?.status === 401) {
          router.push("/login");
          return;
        }
        setError(err?.message || "No se pudo cargar ventas.");
      }
    };

    boot();
  }, [allowed, user, loadAux, loadStock, loadPaymentAccounts, router]);

  useEffect(() => {
    if (!allowed || !user) return;
    loadSales().catch((err: any) => {
      if (err?.status === 401) {
        router.push("/login");
        return;
      }
      setError(err?.message || "No se pudo cargar ventas.");
    });
  }, [allowed, user, loadSales, router]);

  useEffect(() => {
    if (!selectedSale) {
      setPayments([]);
      setLicenses([]);
      return;
    }
    loadSaleDetail(selectedSale).catch(() => null);
  }, [selectedSale, loadSaleDetail]);

  useEffect(() => {
    const handlePointerDown = (event: MouseEvent) => {
      if (!customerAutocompleteRef.current?.contains(event.target as Node)) {
        setIsCustomerMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  useEffect(() => {
    const intent = searchParams.get("intent");
    if (intent === "create-sale") {
      setShowCreateForm(true);
      requestAnimationFrame(() => {
        createSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      });
      router.replace("/admin/sales");
    } else if (intent === "create-customer") {
      setShowCreateForm(true);
      setShowCreateCustomerModal(true);
      requestAnimationFrame(() => {
        createSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      });
      router.replace("/admin/sales");
    }
  }, [router, searchParams]);

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
  };

  const handleOpenCreate = () => {
    setShowCreateForm(true);
    requestAnimationFrame(() => {
      createSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  };

  const deliveryLabel = (status: string) => {
    switch (status) {
      case "DELIVERED":
        return "Entregado";
      case "SENT":
        return "Enviado";
      case "FAILED":
        return "Falló";
      case "PENDING":
        return "Pendiente";
      case "NOT_SENT":
        return "No enviado";
      default:
        return status || "Sin estado";
    }
  };

  const openCreateCustomerModal = () => {
    setShowCreateForm(true);
    setShowCreateCustomerModal(true);
    requestAnimationFrame(() => {
      createSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  };

  const handleStartEdit = () => {
    if (!selectedSale) return;
    if (selectedSale.status !== "DRAFT") {
      pushToast("error", "Solo puedes modificar ventas en borrador.");
      return;
    }
    setIsEditing(true);
    setEditingSaleId(selectedSale.id);
    setNewSale({
      clientId: selectedSale.clientId,
      saleType: selectedSale.saleType,
      marketCode: selectedSale.country || newSale.marketCode,
      items: selectedSale.items.map((item) => ({
        softwareId: item.softwareId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        selectedLicenseIds: [],
      })),
    });
    const client = customers.find((customer) => customer.id === selectedSale.clientId);
    setCustomerSearchTerm(client ? formatName(client.firstname, client.lastname) : "");
    setIsCustomerMenuOpen(false);
    handleOpenCreate();
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setEditingSaleId(null);
    setNewSale((current) => ({
      clientId: "",
      saleType: "CASH",
      marketCode: current.marketCode,
      items: [{ softwareId: "", quantity: 1, unitPrice: 0, selectedLicenseIds: [] }],
    }));
    setCustomerSearchTerm("");
    setIsCustomerMenuOpen(false);
  };

  const handleExport = () => {
    if (sales.content.length === 0) {
      setNotice("No hay ventas para exportar.");
      setTimeout(() => setNotice(null), 2200);
      return;
    }

    const rows = displaySales.map((sale) => {
      const client = customers.find((c) => c.id === sale.clientId);
      const items = sale.items
        .map((item) => `${item.softwareName} x${item.quantity}`)
        .join(" | ");
      return {
        id: sale.id,
        date: sale.createdAt,
        client: formatName(client?.firstname, client?.lastname),
        email: client?.email || "",
        saleType: sale.saleType,
        status: sale.status,
        total: sale.total,
        paid: sale.paid,
        balance: sale.balance,
        items,
      };
    });

    const headers = [
      "id",
      "date",
      "client",
      "email",
      "saleType",
      "status",
      "total",
      "paid",
      "balance",
      "items",
    ];

    const csv = [
      headers.join(","),
      ...rows.map((row) => headers.map((key) => String((row as any)[key] ?? "")).join(",")),
    ].join("\n");

    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `ventas_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const updateItem = (index: number, patch: Partial<CreateItem>) => {
    setNewSale((prev) => {
      const items = prev.items.map((item, idx) => (idx === index ? { ...item, ...patch } : item));
      return { ...prev, items };
    });
  };

  const addItem = () => {
    setNewSale((prev) => ({
      ...prev,
      items: [...prev.items, { softwareId: "", quantity: 1, unitPrice: 0, selectedLicenseIds: [] }],
    }));
  };

  const removeItem = (index: number) => {
    setNewSale((prev) => ({
      ...prev,
      items: prev.items.filter((_, idx) => idx !== index),
    }));
  };

  const handleCreateSale = async () => {
    setError(null);
    try {
      if (!newSale.clientId || validCreateItems.length === 0) {
        setError("Selecciona un cliente y agrega al menos un item válido.");
        pushToast("error", "Completa cliente e items para guardar.");
        return;
      }
      if (!createStockOk) {
        setError("Stock insuficiente para esta venta.");
        pushToast("error", "Stock insuficiente. Ajusta cantidad o carga licencias.");
        return;
      }
      const payload = {
        clientId: newSale.clientId,
        saleType: newSale.saleType,
        country: newSale.marketCode,
        items: validCreateItems.map((item) => ({
          softwareId: item.softwareId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          selectedLicenseIds: item.selectedLicenseIds,
        })),
      };
      setSavingSale(true);
      const created = await apiFetch<Sale>("/api/backend/api/v1/sales", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setNotice("Venta creada.");
      pushToast("success", "Venta creada correctamente.");
      setNewSale({
        clientId: "",
        saleType: "CASH",
        marketCode: newSale.marketCode,
        items: [{ softwareId: "", quantity: 1, unitPrice: 0, selectedLicenseIds: [] }],
      });
      setNewSaleChannels(defaultDeliveryChannels);
      setShowCreateForm(false);
      selectSale(created);
      upsertSaleInList(created);
      setSortOrder("desc");
      setPage(0);
      await loadStock();
      setSaleDeliveryPrefs((prev) => ({
        ...prev,
        [created.id]: { ...newSaleChannels },
      }));
    } catch (err: any) {
      setError(err?.message || "No se pudo crear la venta.");
      pushToast("error", err?.message || "No se pudo crear la venta.");
    } finally {
      setSavingSale(false);
    }
  };

  const handleUpdateSale = async () => {
    if (!editingSaleId) return;
    setError(null);
    try {
      if (!newSale.clientId || validCreateItems.length === 0) {
        setError("Selecciona un cliente y agrega al menos un item válido.");
        pushToast("error", "Completa cliente e items para guardar.");
        return;
      }
      if (!createStockOk) {
        setError("Stock insuficiente para esta venta.");
        pushToast("error", "Stock insuficiente. Ajusta cantidad o carga licencias.");
        return;
      }
      const payload = {
        clientId: newSale.clientId,
        saleType: newSale.saleType,
        country: newSale.marketCode,
        items: validCreateItems.map((item) => ({
          softwareId: item.softwareId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          selectedLicenseIds: item.selectedLicenseIds,
        })),
      };
      setSavingSale(true);
      const updated = await apiFetch<Sale>(`/api/backend/api/v1/sales/${editingSaleId}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      pushToast("success", "Venta actualizada.");
      setIsEditing(false);
      setEditingSaleId(null);
      setNewSale({
        clientId: "",
        saleType: "CASH",
        marketCode: newSale.marketCode,
        items: [{ softwareId: "", quantity: 1, unitPrice: 0, selectedLicenseIds: [] }],
      });
      setShowCreateForm(false);
      selectSale(updated);
      upsertSaleInList(updated);
      await Promise.all([loadSales(), loadStock()]);
      await loadSaleDetail(updated);
    } catch (err: any) {
      setError(err?.message || "No se pudo actualizar la venta.");
      pushToast("error", err?.message || "No se pudo actualizar la venta.");
    } finally {
      setSavingSale(false);
    }
  };

  const handleConfirmSale = async () => {
    if (!selectedSale) return;
    if (confirmingSaleId === selectedSale.id || selectedSale.status !== "DRAFT") return;
    setError(null);
    setConfirmingSaleId(selectedSale.id);
    try {
      if (!confirmStockOk) {
        setError("Stock insuficiente para confirmar.");
        pushToast("error", "Stock insuficiente. Carga licencias antes de confirmar.");
        return;
      }
      const deliveryPrefs = saleDeliveryPrefs[selectedSale.id] ?? defaultDeliveryChannels;
      const deliveryChannels = [
        ...(deliveryPrefs.email ? ["EMAIL"] : []),
        ...(deliveryPrefs.whatsapp ? ["WHATSAPP"] : []),
      ];
      if (deliveryChannels.length === 0) {
        setError("Selecciona al menos un canal de entrega.");
        pushToast("error", "Selecciona al menos un canal de entrega.");
        return;
      }
      const updated = await apiFetch<Sale>(`/api/backend/api/v1/sales/${selectedSale.id}/confirm`, {
        method: "POST",
        body: JSON.stringify({ deliveryChannels }),
      });
      selectSale(updated);
      upsertSaleInList(updated);
      await Promise.all([loadSales(), loadStock()]);
      await loadSaleDetail(updated);
      pushToast("success", "Venta confirmada.");
    } catch (err: any) {
      const message =
        err?.status === 409
          ? "Stock insuficiente para confirmar."
          : err?.message || "No se pudo confirmar la venta.";
      setError(message);
      pushToast("error", message);
    } finally {
      setConfirmingSaleId(null);
    }
  };

  const handleAddPayment = async () => {
    if (!selectedSale) return;
    setError(null);
    try {
      const amount = parsePaymentAmount(paymentForm.amount);
      if (!amount || amount <= 0) {
        setError("Ingresa un monto válido.");
        pushToast("error", "Ingresa un monto válido.");
        return;
      }
      setSavingPayment(true);
      await apiFetch<Sale>("/api/backend/api/v1/payments", {
        method: "POST",
        body: JSON.stringify({
          saleId: selectedSale.id,
          amount,
          method: resolvePaymentMethod(paymentForm.method),
          reference: paymentForm.reference || null,
          paymentAccountId: resolvePaymentAccountId(paymentForm.paymentAccountId),
          notifyClient: paymentForm.notifyClient,
        }),
      });
      setPaymentForm((prev) => ({
        amount: "",
        method: "CASH",
        reference: "",
        paymentAccountId: prev.paymentAccountId,
        notifyClient: true,
      }));
      const refreshed = await apiFetch<Sale>(`/api/backend/api/v1/sales/${selectedSale.id}`);
      selectSale(refreshed);
      upsertSaleInList(refreshed);
      await Promise.all([loadSales(), loadStock()]);
      await loadSaleDetail(refreshed);
      pushToast("success", "Pago registrado.");
    } catch (err: any) {
      const message =
        err?.status === 409
          ? "Stock insuficiente para entregar licencias."
          : err?.message || "No se pudo registrar el pago.";
      setError(message);
      pushToast("error", message);
    } finally {
      setSavingPayment(false);
    }
  };

  const handleRetryDelivery = async () => {
    if (!deliveryEventId) return;
    setRetryingDelivery(true);
    try {
      await apiFetch(`/api/backend/api/v1/integrations/outbox/${deliveryEventId}/retry`, {
        method: "POST",
      });
      pushToast("success", "Reintento enviado.");
      if (selectedSale) {
        await loadSaleDetail(selectedSale);
      }
    } catch (err: any) {
      pushToast("error", err?.message || "No se pudo reintentar el envio.");
    } finally {
      setRetryingDelivery(false);
    }
  };

  const handleDeleteSale = async () => {
    if (!selectedSale) return;
    const ok = window.confirm(
      "¿Eliminar esta venta de forma definitiva? Se revertira stock, pagos y cuentas por cobrar."
    );
    if (!ok) return;
    setError(null);
    try {
      await apiFetch(`/api/backend/api/v1/sales/${selectedSale.id}`, {
        method: "DELETE",
      });
      selectSale(null);
      await Promise.all([loadSales(), loadStock()]);
      pushToast("success", "Venta eliminada.");
    } catch (err: any) {
      if (err?.status === 404) {
        selectSale(null);
        await Promise.all([loadSales(), loadStock()]);
        pushToast("success", "La venta ya fue eliminada.");
        return;
      }
      setError(err?.message || "No se pudo eliminar la venta.");
      pushToast("error", err?.message || "No se pudo eliminar la venta.");
    }
  };

  const clearNewSale = () => {
    setNewSale((current) => ({
      clientId: "",
      saleType: "CASH",
      marketCode: current.marketCode,
      items: [{ softwareId: "", quantity: 1, unitPrice: 0, selectedLicenseIds: [] }],
    }));
    setCustomerSearchTerm("");
    setIsCustomerMenuOpen(false);
  };

  const handleSelectCustomer = (customer: Customer) => {
    const nextCustomerType = customer.customerType ?? "FINAL";
    setNewSale((prev) => ({
      ...prev,
      clientId: customer.id,
      items: applySegmentPricesToItems(prev.items, prev.marketCode, nextCustomerType),
    }));
    setCustomerSearchTerm(formatName(customer.firstname, customer.lastname));
    setIsCustomerMenuOpen(false);
  };

  useEffect(() => {
    const intent = searchParams.get("intent");
    const crmLeadId = searchParams.get("crmLeadId") || "";

    if (intent !== "crm-sale" && !crmLeadId) {
      return;
    }

    const draft: CrmLeadDraft = {
      id: crmLeadId,
      name: searchParams.get("name") || "",
      email: searchParams.get("email") || "",
      phone: searchParams.get("phone") || "",
      interest: searchParams.get("interest") || "",
      customerType:
        searchParams.get("customerType") === "DISTRIBUTOR" ? "DISTRIBUTOR" : "FINAL",
      estimatedValue: searchParams.get("estimatedValue") || "",
    };

    const key = JSON.stringify(draft);
    if (crmLeadAppliedRef.current === key) {
      return;
    }
    crmLeadAppliedRef.current = key;
    setCrmLeadDraft(draft);
    setShowCreateForm(true);
    setIsEditing(false);
    setEditingSaleId(null);

    const draftEmail = normalizeEmail(draft.email);
    const draftPhone = normalizePhone(draft.phone);
    const existingCustomer = customers.find((customer) => {
      const customerEmail = normalizeEmail(customer.email);
      const customerPhone = normalizePhone(customer.phone);
      return Boolean(
        (draftEmail && customerEmail === draftEmail) ||
          (draftPhone && customerPhone && (customerPhone.endsWith(draftPhone) || draftPhone.endsWith(customerPhone)))
      );
    });

    if (existingCustomer) {
      handleSelectCustomer(existingCustomer);
      setShowCreateCustomerModal(false);
    } else {
      const names = splitFullName(draft.name);
      setNewSale((prev) => ({ ...prev, clientId: "" }));
      setCustomerSearchTerm(draft.name || draft.phone || draft.email);
      setNewCustomer({
        firstname: names.firstname,
        lastname: names.lastname,
        email: draft.email,
        phone: draft.phone,
        taxId: "",
      });
      setShowCreateCustomerModal(true);
    }

    requestAnimationFrame(() => {
      createSectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }, [customers, searchParams]);

  const resetNewCustomer = () => {
    setNewCustomer({
      firstname: "",
      lastname: "",
      email: "",
      phone: "",
      taxId: "",
    });
  };

  const handleCreateCustomer = async () => {
    setError(null);
    try {
      if (!newCustomer.firstname.trim() || !newCustomer.lastname.trim() || !newCustomer.email.trim()) {
        setError("Completa nombre, apellido y correo del cliente.");
        pushToast("error", "Completa nombre, apellido y correo del cliente.");
        return;
      }
      const payload: AdminUserPayload = {
        firstname: newCustomer.firstname.trim(),
        lastname: newCustomer.lastname.trim(),
        email: newCustomer.email.trim(),
        phone: newCustomer.phone.trim() || null,
        role: "CUSTOMER",
        country: newSale.marketCode,
        enabled: true,
        taxId: newCustomer.taxId.trim() || null,
        billingEmail: newCustomer.email.trim(),
        hasCredit: false,
        creditLimit: 0,
        creditDays: 0,
        password: null,
      };
      const created = await apiFetch<AdminUser>("/api/backend/api/v1/admin/users", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      const createdCustomer: Customer = {
        id: created.id,
        firstname: created.firstname,
        lastname: created.lastname,
        email: created.email,
        phone: created.phone || undefined,
        hasCredit: created.hasCredit,
        creditLimit: created.creditLimit,
        creditDays: created.creditDays,
      };
      setCustomers((prev) => {
        const next = prev.filter((customer) => customer.id !== createdCustomer.id);
        return [createdCustomer, ...next];
      });
      handleSelectCustomer(createdCustomer);
      setShowCreateCustomerModal(false);
      resetNewCustomer();
      pushToast("success", "Cliente creado y seleccionado.");
    } catch (err: any) {
      setError(err?.message || "No se pudo crear el cliente.");
      pushToast("error", err?.message || "No se pudo crear el cliente.");
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen grid place-items-center">
        <div className="glass-panel rounded-3xl px-6 py-4">Cargando ventas...</div>
      </div>
    );
  }

  if (!user || !allowed) {
    return null;
  }

  return (
    <div className="min-h-screen flex">

      <main className="flex-1 px-6 py-8 lg:px-10">
        <div className="glass-panel hero-band rounded-[32px] px-6 py-6 fade-up">
          <AdminHeader
            title="Ventas"
            subtitle="Gestiona ventas, pagos y entregas en un solo lugar."
            userEmail={user.email}
            onLogout={handleLogout}
            actions={
              <>
                <AdminActionButton
                  icon={<PlusIcon className="h-4 w-4" />}
                  label="Nueva venta"
                  variant="primary"
                  onClick={handleOpenCreate}
                />
                <AdminActionButton
                  icon={<UsersIcon className="h-4 w-4" />}
                  label="Crear nuevo cliente"
                  onClick={openCreateCustomerModal}
                />
                <AdminActionButton
                  icon={<DownloadIcon className="h-4 w-4" />}
                  label="Exportar"
                  onClick={handleExport}
                />
                <AdminActionButton
                  icon={<RefreshIcon className="h-4 w-4" />}
                  label="Actualizar"
                  onClick={() => Promise.all([loadSales(), loadStock()])}
                />
              </>
            }
          />
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2 lg:grid-cols-5">
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Resultados</p>
            <p className="mt-2 font-display text-2xl text-slate">{statusCounts.total}</p>
            <p className="text-xs text-slate/60">Filtrado actual</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Pagadas</p>
            <p className="mt-2 font-display text-2xl text-slate">{statusCounts.PAID || 0}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Confirmadas</p>
            <p className="mt-2 font-display text-2xl text-slate">{statusCounts.CONFIRMED || 0}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Borradores</p>
            <p className="mt-2 font-display text-2xl text-slate">{statusCounts.DRAFT || 0}</p>
          </div>
          <div className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Vendido hoy</p>
            <p className="mt-2 font-display text-2xl text-slate">{formatCurrency(todayRevenue)}</p>
            <p className="text-xs text-slate/60">Según filtros</p>
          </div>
        </div>

        {toasts.length > 0 && (
          <div className="mt-6 space-y-2">
            {toasts.map((toast) => (
              <div
                key={toast.id}
                className={`rounded-2xl border p-3 text-xs ${
                  toast.type === "success"
                    ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                    : "border-red-200 bg-red-50 text-red-700"
                }`}
              >
                {toast.message}
              </div>
            ))}
          </div>
        )}

        {error && (
          <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {error}
          </div>
        )}
        {notice && (
          <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-700">
            {notice}
          </div>
        )}

        <section className="mt-10 grid gap-6 xl:grid-cols-[minmax(340px,0.85fr)_minmax(0,1.35fr)]">
          <div className="glass-panel rounded-3xl p-6 fade-up delay-1">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl text-slate">Listado de ventas</h2>
              <span className="text-xs uppercase tracking-[0.2em] text-slate/50">
                Total {sales.totalElements}
              </span>
            </div>
            <div className="mt-4 grid gap-3 rounded-2xl border border-smoke/60 bg-white/70 p-3 text-xs text-slate/70 md:grid-cols-4">
              <label className="flex flex-col gap-1">
                Estado de venta
                <select
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                  value={filters.status}
                  onChange={(event) => setFilters((prev) => ({ ...prev, status: event.target.value }))}
                >
                  <option value="">Todos</option>
                  {saleStatuses.map((status) => (
                    <option key={status} value={status}>
                      {getStatusLabel(status)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-1">
                Cliente
                <select
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                  value={filters.clientId}
                  onChange={(event) => setFilters((prev) => ({ ...prev, clientId: event.target.value }))}
                >
                  <option value="">Todos</option>
                  {customers.map((customer) => (
                    <option key={customer.id} value={customer.id}>
                      {formatName(customer.firstname, customer.lastname)}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-1">
                Desde (fecha)
                <input
                  type="date"
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                  value={filters.from}
                  onChange={(event) => setFilters((prev) => ({ ...prev, from: event.target.value }))}
                />
              </label>
              <label className="flex flex-col gap-1">
                Hasta (fecha)
                <input
                  type="date"
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                  value={filters.to}
                  onChange={(event) => setFilters((prev) => ({ ...prev, to: event.target.value }))}
                />
              </label>
              <label className="flex flex-col gap-1 md:col-span-2">
                Buscar
                <input
                  placeholder="Cliente, correo, producto, ID..."
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                  value={searchTerm}
                  onChange={(event) => setSearchTerm(event.target.value)}
                />
              </label>
              <label className="flex flex-col gap-1 md:col-span-2">
                Ordenar por fecha
                <select
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                  value={sortOrder}
                  onChange={(event) => setSortOrder(event.target.value as "asc" | "desc")}
                >
                  <option value="desc">Más recientes</option>
                  <option value="asc">Más antiguas</option>
                </select>
              </label>
              <button
                onClick={() => {
                  if (page === 0) {
                    loadSales().catch(() => null);
                    return;
                  }
                  setPage(0);
                }}
                className="col-span-full btn btn-secondary text-xs uppercase tracking-[0.2em]"
              >
                Aplicar filtros
              </button>
            </div>
            <div className="mt-4 max-h-[32rem] space-y-3 overflow-y-auto pr-2">
              {displaySales.map((sale) => (
                <button
                  key={sale.id}
                  onClick={() => selectSale(sale)}
                  className={`w-full rounded-2xl border px-4 py-3 text-left transition ${
                    selectedSale?.id === sale.id
                      ? "border-ink bg-ink/90 text-white"
                      : "border-smoke/60 bg-white/80 text-slate hover:bg-white"
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-[10px] font-semibold uppercase tracking-[0.14em] opacity-70">
                        {sale.saleNumber ?? "Borrador sin numero"}
                      </p>
                      <p className="text-sm font-medium">
                        {sale.items[0]?.softwareName || "Venta"} · {getSaleTypeLabel(sale.saleType)}
                      </p>
                      <p className="text-xs opacity-70">
                        {formatName(
                          customers.find((customer) => customer.id === sale.clientId)?.firstname,
                          customers.find((customer) => customer.id === sale.clientId)?.lastname
                        )}
                      </p>
                      <p className="text-xs opacity-70">{formatDateTime(sale.createdAt)}</p>
                    </div>
                    <div className="text-right text-sm">
                      {formatCurrency(sale.total)}
                      <div className="mt-1 text-xs opacity-70">Saldo {formatCurrency(sale.balance)}</div>
                      <div
                        className={`mt-1 inline-flex rounded-full border px-2 py-0.5 text-[10px] uppercase tracking-[0.18em] ${getStatusTone(
                          sale.status
                        )}`}
                      >
                        {getStatusLabel(sale.status)}
                      </div>
                    </div>
                  </div>
                </button>
              ))}
              {displaySales.length === 0 && (
                <div className="rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
                  No hay ventas con estos filtros.
                </div>
              )}
            </div>
            <div className="mt-4 flex items-center justify-between text-xs text-slate/60">
              <span>
                Pagina {sales.number + 1} de {Math.max(sales.totalPages, 1)}
              </span>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    if (page === 0) return;
                    setPage((prev) => prev - 1);
                  }}
                  disabled={page === 0}
                  className="rounded-full border border-smoke/60 bg-white px-3 py-1 uppercase tracking-[0.2em] text-slate disabled:opacity-50"
                >
                  Prev
                </button>
                <button
                  onClick={() => {
                    if (page + 1 >= sales.totalPages) return;
                    setPage((prev) => prev + 1);
                  }}
                  disabled={page + 1 >= sales.totalPages}
                  className="rounded-full border border-smoke/60 bg-white px-3 py-1 uppercase tracking-[0.2em] text-slate disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          </div>

          <div className="glass-panel rounded-3xl p-6 fade-up delay-2 xl:sticky xl:top-6 xl:self-start">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.24em] text-slate/45">Operacion</p>
                <h2 className="mt-1 font-display text-xl text-slate">Detalle de la venta</h2>
                {selectedSale?.saleNumber && (
                  <p className="mt-1 text-xs font-semibold uppercase tracking-[0.18em] text-ink">
                    {selectedSale.saleNumber}
                  </p>
                )}
              </div>
              {selectedSale && (
                <span className={`rounded-full border px-3 py-1 text-xs ${getStatusTone(selectedSale.status)}`}>
                  {getStatusLabel(selectedSale.status)}
                </span>
              )}
            </div>
            {!selectedSale && (
              <div className="mt-6 rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
                Selecciona una venta para ver el detalle.
              </div>
            )}
            {selectedSale && (
              <div className="mt-4 space-y-4 text-[13px] text-slate/80">
                {isConfirmingSelectedSale && (
                  <div className="rounded-2xl border border-sky-200 bg-sky-50 p-4 text-sky-800">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-sky-950">Confirmando venta y preparando entrega</p>
                        <p className="mt-1 text-xs text-sky-700">
                          Validando stock, asignando licencias y notificando por los canales seleccionados.
                        </p>
                      </div>
                      <span className="h-3 w-3 animate-pulse rounded-full bg-sky-500" />
                    </div>
                    <div className="mt-3 grid gap-2 text-[11px] md:grid-cols-3">
                      {["Stock", "Licencias", "Email / WhatsApp"].map((step) => (
                        <div key={step} className="rounded-xl border border-sky-200 bg-white/70 px-3 py-2">
                          {step}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-smoke/60 bg-white/95 p-2">
                  <button
                    onClick={() => Promise.all([loadSales(), loadStock()])}
                    className="rounded-full border border-smoke/70 bg-white px-2 py-1 text-[9px] uppercase tracking-[0.16em] text-ink"
                  >
                    Actualizar
                  </button>
                  <button
                    onClick={handleStartEdit}
                    disabled={!canModify || isConfirmingSelectedSale}
                    className="rounded-full border border-smoke/70 bg-white px-2 py-1 text-[9px] uppercase tracking-[0.16em] text-ink disabled:opacity-50"
                  >
                    Modificar
                  </button>
                  <button
                    onClick={handleDeleteSale}
                    disabled={!canDeleteSale || isConfirmingSelectedSale}
                    className="rounded-full border border-red-200 bg-red-50 px-2 py-1 text-[9px] uppercase tracking-[0.16em] text-red-700 disabled:opacity-50"
                  >
                    Eliminar definitivo
                  </button>
                </div>

                <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-smoke/60 bg-white/95 p-3 text-[9px] uppercase tracking-[0.16em]">
                  <span
                    className={`rounded-xl px-3 py-2 ${
                      selectedSale.status === "DRAFT" ? "bg-ink text-white" : "bg-smoke/50 text-slate"
                    }`}
                  >
                    Borrador
                  </span>
                  <span className="text-slate/40">→</span>
                  <span
                    className={`rounded-xl px-3 py-2 ${
                      selectedSale.status === "CONFIRMED" || selectedSale.status === "PAID"
                        ? "bg-ink text-white"
                        : "bg-smoke/50 text-slate"
                    }`}
                  >
                    Confirmada
                  </span>
                  <span className="text-slate/40">→</span>
                  <span
                    className={`rounded-xl px-3 py-2 ${
                      selectedSale.status === "PAID" ? "bg-ink text-white" : "bg-smoke/50 text-slate"
                    }`}
                  >
                    Pagada
                  </span>
                </div>

                <div className="grid gap-3 md:grid-cols-3">
                  <div className="rounded-2xl border border-smoke/60 bg-white/98 p-4">
                    <p className="text-[9px] uppercase tracking-[0.16em] text-slate/60">Total</p>
                    <p className="mt-2 text-xl font-semibold text-ink">{formatCurrency(selectedSale.total)}</p>
                  </div>
                  <div className="rounded-2xl border border-smoke/60 bg-white/98 p-4">
                    <p className="text-[9px] uppercase tracking-[0.16em] text-slate/60">Pagado</p>
                    <p className="mt-2 text-xl font-semibold text-ink">{formatCurrency(selectedSale.paid)}</p>
                  </div>
                  <div className="rounded-2xl border border-smoke/60 bg-white/98 p-4">
                    <p className="text-[9px] uppercase tracking-[0.16em] text-slate/60">Saldo</p>
                    <p className="mt-2 text-xl font-semibold text-ink">{formatCurrency(selectedSale.balance)}</p>
                  </div>
                </div>

                <div className="grid gap-3 rounded-2xl border border-smoke/60 bg-white/98 p-4 md:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)]">
                  <div>
                    <p className="text-[9px] uppercase tracking-[0.16em] text-slate/60">Cliente</p>
                    <p className="mt-1 text-[13px] font-semibold text-ink">
                      {formatName(selectedSaleClient?.firstname, selectedSaleClient?.lastname)}
                    </p>
                    <p className="text-[10px] text-slate/60">{selectedSaleClient?.email || "-"}</p>
                    <p className="text-[10px] text-slate/60">{selectedSaleClient?.phone || "-"}</p>
                  </div>
                  <div className="grid gap-1 text-[10px] text-slate/70">
                    <div className="flex items-center justify-between">
                      <span>Tipo</span>
                      <span className="font-mono text-[10px] text-ink">{getSaleTypeLabel(selectedSale.saleType)}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Creada</span>
                      <span>{formatDateTime(selectedSale.createdAt)}</span>
                    </div>
                  </div>
                </div>

                <div className="rounded-2xl border border-smoke/60 bg-white/98 p-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-ink">Items</h3>
                  </div>
                  <div className="mt-2 divide-y divide-smoke/50">
                    {selectedSale.items.map((item) => {
                      const stock = stockMap[item.softwareId];
                      const stockOk = stock ? stock.available >= item.quantity : false;
                      return (
                        <div key={item.softwareId} className="flex items-center justify-between py-2">
                          <div className="space-y-1">
                            <p className="text-[13px] font-semibold text-ink">{item.softwareName}</p>
                            <p className="text-[10px] text-slate/60">
                              {item.quantity} x {formatCurrency(item.unitPrice)}
                            </p>
                      <p className={`text-[10px] ${stockOk ? "text-emerald-700" : "text-red-600"}`}>
                        Activaciones disponibles: {stock ? stock.available : 0}
                      </p>
                          </div>
                          <span className="text-[13px] font-semibold text-ink">
                            {formatCurrency(item.subtotal)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>

                <div className="rounded-2xl border border-smoke/60 bg-white/98 p-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-ink">Envios</h3>
                    <span className="text-xs text-slate/60">
                      {deliveryEventId ? "Seguimiento activo" : "Sin envio"}
                    </span>
                  </div>
                  <div className="mt-3 grid gap-2 text-xs md:grid-cols-2">
                    <div className="flex items-center justify-between rounded-xl border border-smoke/60 bg-white px-3 py-2">
                      <span className="text-slate/60">Email</span>
                      <span className={`rounded-full border px-2 py-0.5 text-[10px] ${deliveryTone(deliveryStatus.EMAIL)}`}>
                        {deliveryLabel(deliveryStatus.EMAIL)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between rounded-xl border border-smoke/60 bg-white px-3 py-2">
                      <span className="text-slate/60">WhatsApp</span>
                      <span
                        className={`rounded-full border px-2 py-0.5 text-[10px] ${deliveryTone(
                          deliveryStatus.WHATSAPP
                        )}`}
                      >
                        {deliveryLabel(deliveryStatus.WHATSAPP)}
                      </span>
                    </div>
                  </div>
                  {deliveryLastError && (
                    <div className="mt-3 rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                      Error: {deliveryLastError}
                    </div>
                  )}
                  {deliveryEventId && (
                    <button
                      onClick={handleRetryDelivery}
                      disabled={retryingDelivery}
                      className="mt-3 rounded-full border border-smoke/70 bg-white px-3 py-1.5 text-[9px] uppercase tracking-[0.16em] text-ink disabled:opacity-50"
                    >
                      {retryingDelivery ? "Reintentando..." : "Reintentar envio"}
                    </button>
                  )}
                </div>

                <div className="flex flex-col gap-3 rounded-2xl border border-smoke/60 bg-white/98 p-4">
                  <div className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[11px] uppercase tracking-[0.2em] text-slate/60">
                    Envio:{" "}
                    {(saleDeliveryPrefs[selectedSale.id]?.email ?? defaultDeliveryChannels.email) && "Correo"}
                    {(saleDeliveryPrefs[selectedSale.id]?.email ?? defaultDeliveryChannels.email) &&
                    (saleDeliveryPrefs[selectedSale.id]?.whatsapp ?? defaultDeliveryChannels.whatsapp)
                      ? " · "
                      : ""}
                    {(saleDeliveryPrefs[selectedSale.id]?.whatsapp ?? defaultDeliveryChannels.whatsapp) && "WhatsApp"}
                  </div>
                  {!confirmStockOk && selectedSale.status === "DRAFT" && (
                    <div className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                      Activaciones insuficientes para confirmar esta venta.
                    </div>
                  )}
                  <button
                    onClick={handleConfirmSale}
                    disabled={selectedSale.status !== "DRAFT" || !confirmStockOk || confirmingSaleId === selectedSale.id}
                    className="rounded-2xl bg-ink px-4 py-3 text-[10px] uppercase tracking-[0.16em] text-white disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {confirmingSaleId === selectedSale.id ? "Confirmando..." : "Confirmar"}
                  </button>
                </div>
              </div>
            )}
          </div>
        </section>

        <section ref={createSectionRef} className="mt-10 grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(360px,0.8fr)]">
          <div className="glass-panel rounded-3xl p-6 fade-up delay-1">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl text-slate">
                {isEditing ? "Modificar venta" : "Crear venta"}
              </h2>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setShowCreateForm((prev) => !prev)}
                  className="rounded-full border border-smoke/70 bg-white px-3 py-1 text-[11px] uppercase tracking-[0.2em] text-slate"
                >
                  {showCreateForm ? "Ocultar" : "Mostrar"}
                </button>
                <span className="text-xs uppercase tracking-[0.2em] text-slate/50">
                  Total {formatCurrency(saleTotal)}
                </span>
              </div>
            </div>
            {crmLeadDraft && (
              <div className="mt-4 rounded-2xl border border-sky-200 bg-sky-50/80 p-4 text-sm text-slate">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-xs uppercase tracking-[0.22em] text-sky-700">
                      Lead desde DismalCRM
                    </p>
                    <p className="mt-1 font-medium text-ink">
                      {crmLeadDraft.name || "Cliente sin nombre"}
                    </p>
                    <p className="text-xs text-slate/70">
                      {crmLeadDraft.email || "Sin correo"} · {crmLeadDraft.phone || "Sin telefono"}
                    </p>
                  </div>
                  <span className="rounded-full border border-sky-200 bg-white px-3 py-1 text-xs text-sky-700">
                    {crmLeadDraft.customerType === "DISTRIBUTOR" ? "Mayorista" : "Cliente final"}
                  </span>
                </div>
                {(crmLeadDraft.interest || crmLeadDraft.estimatedValue) && (
                  <div className="mt-3 rounded-xl border border-sky-100 bg-white/75 px-3 py-2 text-xs text-slate/75">
                    {crmLeadDraft.interest && <p>Interes: {crmLeadDraft.interest}</p>}
                    {crmLeadDraft.estimatedValue && Number.isFinite(Number(crmLeadDraft.estimatedValue)) && (
                      <p>Valor estimado: {formatCurrency(Number(crmLeadDraft.estimatedValue))}</p>
                    )}
                  </div>
                )}
              </div>
            )}
            {showCreateForm && (
              <div className="mt-4 grid gap-4">
              <div className="flex flex-col gap-2 text-sm" ref={customerAutocompleteRef}>
                <span>Cliente</span>
                <div className="relative">
                  <input
                    className="w-full rounded-xl border border-smoke/60 bg-white px-3 py-2"
                  placeholder="Buscar por nombres, apellidos, correo o teléfono"
                  value={customerSearchTerm}
                  onFocus={() => setIsCustomerMenuOpen(true)}
                  onChange={(event) => {
                    const value = event.target.value;
                    setCustomerSearchTerm(value);
                    setIsCustomerMenuOpen(true);
                    if (!value.trim() && newSale.clientId) {
                      setNewSale((prev) => ({ ...prev, clientId: "" }));
                    }
                  }}
                />
                {isCustomerMenuOpen && (
                  <div className="absolute z-20 mt-2 max-h-72 w-full overflow-y-auto rounded-2xl border border-smoke/60 bg-white shadow-[0_20px_50px_rgba(15,23,42,0.18)]">
                  {customerOptions.map((customer) => (
                    <button
                      key={customer.id}
                      type="button"
                      className={`flex w-full items-start justify-between gap-3 border-t border-smoke/40 px-3 py-3 text-left text-sm first:border-t-0 ${
                        newSale.clientId === customer.id
                          ? "bg-ink text-white"
                          : "text-slate hover:bg-smoke/30"
                      }`}
                      onClick={() => handleSelectCustomer(customer)}
                    >
                      <span className="min-w-0">
                        <span className="block font-medium">
                          {formatName(customer.firstname, customer.lastname)}
                        </span>
                        <span className="block truncate text-[11px] opacity-70">
                          {customer.email}
                          {customer.phone ? ` · ${customer.phone}` : ""}
                        </span>
                      </span>
                    </button>
                  ))}
                  {customerOptions.length === 0 && (
                    <div className="px-3 py-3 text-sm text-slate/60">No hay clientes con ese criterio.</div>
                  )}
                  </div>
                )}
                </div>
                {selectedCustomer ? (
                  <div className="flex items-center justify-between rounded-2xl border border-smoke/60 bg-white/70 px-3 py-2 text-xs text-slate">
                    <div>
                      <p className="text-sm font-medium text-ink">
                        {formatName(selectedCustomer.firstname, selectedCustomer.lastname)}
                      </p>
                      <p className="text-slate/70">
                        {selectedCustomer.email}
                        {selectedCustomer.phone ? ` · ${selectedCustomer.phone}` : ""}
                      </p>
                    </div>
                    <button
                      type="button"
                      className="rounded-full border border-smoke/60 px-3 py-1 text-[11px] uppercase tracking-[0.2em] text-slate"
                      onClick={() => {
                        setNewSale((prev) => ({ ...prev, clientId: "" }));
                        setCustomerSearchTerm("");
                        setIsCustomerMenuOpen(false);
                      }}
                    >
                      Limpiar
                    </button>
                  </div>
                ) : (
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-xs text-slate/60">
                      Escribe el nombre o apellido y selecciona el cliente a facturar.
                    </p>
                    <button
                      type="button"
                      className="rounded-full border border-smoke/60 px-3 py-1 text-[11px] uppercase tracking-[0.2em] text-slate"
                      onClick={openCreateCustomerModal}
                    >
                      Crear cliente
                    </button>
                  </div>
                )}
              </div>
              <label className="flex flex-col gap-2 text-sm">
                Tipo de venta
                <select
                  className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                  value={newSale.saleType}
                  onChange={(event) => setNewSale((prev) => ({ ...prev, saleType: event.target.value }))}
                >
                  {saleTypes.map((type) => (
                    <option key={type} value={type}>
                      {getSaleTypeLabel(type)}
                    </option>
                  ))}
                </select>
              </label>
              <div className="grid gap-3 rounded-2xl border border-smoke/60 bg-white/70 p-3 md:grid-cols-2">
                <label className="flex flex-col gap-2 text-sm">
                  Mercado
                  <select
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    value={newSale.marketCode}
                    onChange={(event) => {
                      const nextMarketCode = event.target.value as "EC" | "PE";
                      setNewSale((prev) => ({
                        ...prev,
                        clientId: "",
                        marketCode: nextMarketCode,
                        items: applySegmentPricesToItems(prev.items, nextMarketCode, "FINAL"),
                      }));
                      setCustomerSearchTerm("");
                    }}
                  >
                    <option value="EC">Ecuador</option>
                  </select>
                </label>
                <div className="flex flex-col gap-2 text-sm">
                  <span>Tipo de cliente</span>
                  <div className="rounded-xl border border-smoke/60 bg-slate-50 px-3 py-2 text-slate">
                    {selectedCustomerType === "DISTRIBUTOR" ? "Distribuidor" : "Cliente final"}
                  </div>
                  <p className="text-xs text-slate/60">
                    Este valor se toma del cliente seleccionado y define el precio sugerido.
                  </p>
                </div>
              </div>
              <div className="rounded-2xl border border-smoke/60 bg-white/70 p-3">
                <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Envio de licencia</p>
                <div className="mt-3 flex flex-wrap items-center gap-4 text-sm text-slate">
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={newSaleChannels.email}
                      onChange={(event) =>
                        setNewSaleChannels((prev) => ({ ...prev, email: event.target.checked }))
                      }
                    />
                    Correo
                  </label>
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={newSaleChannels.whatsapp}
                      onChange={(event) =>
                        setNewSaleChannels((prev) => ({ ...prev, whatsapp: event.target.checked }))
                      }
                    />
                    WhatsApp
                  </label>
                </div>
                {!newSaleChannels.email && !newSaleChannels.whatsapp && (
                  <p className="mt-2 text-xs text-amber-700">
                    Debes seleccionar al menos un canal para enviar licencias.
                  </p>
                )}
              </div>

              <div className="space-y-3">
                {newSale.items.map((item, index) => (
                  <div key={index} className="grid gap-3 rounded-2xl border border-smoke/60 bg-white/70 p-3 md:grid-cols-4">
                    <label className="flex flex-col gap-1 text-xs">
                      Producto
                      <select
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                        value={item.softwareId}
                        onChange={(event) => {
                          const product = products.find((p) => p.id === event.target.value);
                          if (event.target.value) {
                            loadLicenseOptions(event.target.value).catch(() => null);
                          }
                          updateItem(index, {
                            softwareId: event.target.value,
                            unitPrice: resolveSegmentUnitPrice(
                              product,
                              newSale.marketCode,
                              selectedCustomerType
                            ),
                            selectedLicenseIds: [],
                          });
                        }}
                      >
                        <option value="">Selecciona</option>
                        {products.map((product) => (
                          <option key={product.id} value={product.id}>
                            {product.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label className="flex flex-col gap-1 text-xs">
                      Cantidad
                      <input
                        type="number"
                        min={1}
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                        value={item.quantity}
                        onChange={(event) => updateItem(index, { quantity: Number(event.target.value) })}
                      />
                    </label>
                    <label className="flex flex-col gap-1 text-xs">
                      Precio unitario
                      <input
                        type="number"
                        min={0}
                        className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                        value={item.unitPrice}
                        onChange={(event) => updateItem(index, { unitPrice: Number(event.target.value) })}
                      />
                    </label>
                    <div className="flex flex-col justify-between text-xs text-slate/70">
                      <span>Subtotal</span>
                      <span className="text-sm font-medium text-slate">
                        {formatCurrency(item.quantity * item.unitPrice)}
                      </span>
                      <span
                        className={`text-xs ${
                          item.softwareId && stockMap[item.softwareId]?.available >= item.quantity
                            ? "text-emerald-700"
                            : "text-red-600"
                        }`}
                      >
                        Activaciones disponibles: {item.softwareId ? stockMap[item.softwareId]?.available ?? 0 : "-"}
                      </span>
                      <button
                        onClick={() => removeItem(index)}
                        className="mt-1 text-xs uppercase tracking-[0.2em] text-accentDeep"
                      >
                        Quitar
                      </button>
                    </div>
                    {item.softwareId && (
                      <div className="md:col-span-4 rounded-2xl border border-smoke/60 bg-white p-3">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-xs uppercase tracking-[0.2em] text-slate/50">Selección manual</p>
                            <p className="text-sm text-slate">
                              Puedes priorizar otras licencias; si faltan activaciones el sistema completa automáticamente.
                            </p>
                          </div>
                          <span className="text-xs text-slate/60">
                            {item.selectedLicenseIds.length} seleccionadas
                          </span>
                        </div>
                        <div className="mt-3 max-h-40 overflow-y-auto rounded-xl border border-smoke/60">
                          {(licenseOptionsBySoftware[item.softwareId] || []).map((license) => {
                            const checked = item.selectedLicenseIds.includes(license.id);
                            const availableSlots = Math.max(
                              (license.maxActivations || 0) - (license.usedActivations || 0),
                              0
                            );
                            return (
                              <label
                                key={license.id}
                                className="flex items-center justify-between gap-3 border-b border-smoke/40 px-3 py-2 text-sm last:border-b-0"
                              >
                                <span className="flex items-center gap-2">
                                  <input
                                    type="checkbox"
                                    checked={checked}
                                    onChange={(event) => {
                                      const next = event.target.checked
                                        ? [...item.selectedLicenseIds, license.id]
                                        : item.selectedLicenseIds.filter((current) => current !== license.id);
                                      updateItem(index, { selectedLicenseIds: next });
                                    }}
                                  />
                                  <span className="font-mono text-xs text-slate">{license.licenseKey}</span>
                                </span>
                                <span className="text-[11px] text-slate/60">
                                  {availableSlots} activaciones
                                </span>
                              </label>
                            );
                          })}
                          {(licenseOptionsBySoftware[item.softwareId] || []).length === 0 && (
                            <div className="px-3 py-3 text-sm text-slate/60">
                              No hay licencias activas disponibles para este producto.
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {!createStockOk && validCreateItems.length > 0 && (
                <div className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                  Activaciones insuficientes para uno o más items. Ajusta cantidades o carga licencias.
                </div>
              )}

              <div className="flex flex-wrap items-center gap-3">
                <button
                  onClick={addItem}
                  className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate"
                >
                  Agregar item
                </button>
                {isEditing ? (
                  <>
                    <button
                      onClick={handleUpdateSale}
                      disabled={!canCreateSale || !createStockOk || savingSale}
                      className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {savingSale ? "Guardando..." : "Guardar cambios"}
                    </button>
                    <button
                      onClick={handleCancelEdit}
                      className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate"
                    >
                      Cancelar edicion
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      onClick={handleCreateSale}
                      disabled={!canCreateSale || !createStockOk || savingSale}
                      className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {savingSale ? "Guardando..." : "Guardar venta"}
                    </button>
                    <button
                      onClick={clearNewSale}
                      className="rounded-full border border-smoke/70 bg-white px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate"
                    >
                      Limpiar
                    </button>
                  </>
                )}
              </div>
              </div>
            )}
          </div>

          <div className="glass-panel rounded-3xl p-6 fade-up delay-2 xl:sticky xl:top-6 xl:self-start">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.24em] text-slate/45">Cobranza</p>
                <h2 className="mt-1 font-display text-xl text-slate">Pagos</h2>
              </div>
              {selectedSale && (
                <span className="rounded-full border border-smoke/60 bg-white px-3 py-1 text-xs text-slate">
                  Saldo {formatCurrency(selectedSale.balance)}
                </span>
              )}
            </div>
            {selectedSale ? (
              <div className="mt-4 space-y-4">
                <div className="grid gap-2 rounded-2xl border border-smoke/60 bg-white/70 p-4 text-xs">
                  <label className="flex flex-col gap-1">
                    Monto
                    <input
                      type="number"
                      min={0}
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                      value={paymentForm.amount}
                      onChange={(event) => setPaymentForm((prev) => ({ ...prev, amount: event.target.value }))}
                    />
                  </label>
                  <button
                    onClick={() =>
                      setPaymentForm((prev) => ({
                        ...prev,
                        amount: selectedSale ? String(selectedSale.balance) : prev.amount,
                      }))
                    }
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-[11px] uppercase tracking-[0.2em] text-slate"
                  >
                    Pagar saldo
                  </button>
                  <label className="flex flex-col gap-1">
                    Metodo
                    <select
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                      value={paymentForm.method}
                      onChange={(event) => setPaymentForm((prev) => ({ ...prev, method: event.target.value }))}
                    >
                      {paymentMethods.map((method) => (
                        <option key={method} value={method}>
                          {getPaymentMethodLabel(method)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="flex flex-col gap-1">
                    Cuenta destino
                    <select
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                      value={paymentForm.paymentAccountId}
                      onChange={(event) =>
                        setPaymentForm((prev) => ({ ...prev, paymentAccountId: event.target.value }))
                      }
                    >
                      {paymentAccounts.length === 0 && <option value="">Caja principal</option>}
                      {paymentAccounts.map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.name} · {account.currency}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="flex flex-col gap-1">
                    Referencia
                    <input
                      className="rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm"
                      placeholder="Ej. transferencia #123, recibo, observacion"
                      value={paymentForm.reference}
                      onChange={(event) => setPaymentForm((prev) => ({ ...prev, reference: event.target.value }))}
                    />
                  </label>
                  <label className="flex items-center gap-2 rounded-xl border border-smoke/60 bg-white px-3 py-2 text-sm text-slate">
                    <input
                      type="checkbox"
                      checked={paymentForm.notifyClient}
                      onChange={(event) =>
                        setPaymentForm((prev) => ({ ...prev, notifyClient: event.target.checked }))
                      }
                    />
                    Notificar al cliente por email y WhatsApp
                  </label>
                  <button
                    onClick={handleAddPayment}
                    disabled={!canPay || savingPayment}
                    className="rounded-xl bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {savingPayment ? "Registrando..." : "Registrar pago"}
                  </button>
                </div>
                {!canPay && (
                  <div className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700">
                    {selectedSale.status === "DRAFT"
                      ? "Confirma la venta antes de registrar pagos."
                      : selectedSale.status === "CANCELLED"
                        ? "La venta cancelada no admite pagos."
                        : "La venta no tiene saldo pendiente."}
                  </div>
                )}
                <div className="space-y-2 text-sm">
                  {payments.map((payment) => (
                    <div key={payment.id} className="rounded-2xl border border-smoke/60 bg-white/80 p-3">
                      <div className="flex items-center justify-between">
                        <span>{formatCurrency(payment.amount)}</span>
                        <span className="text-xs text-slate/60">{formatDateTime(payment.createdAt)}</span>
                      </div>
                      <div className="text-xs font-medium text-slate">
                        Cuenta: {payment.paymentAccountName || "Caja principal"}
                      </div>
                      <div className="text-xs text-slate/60">
                        {getPaymentMethodLabel(payment.method)} · {payment.reference || "-"}
                      </div>
                    </div>
                  ))}
                  {payments.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
                      Sin pagos registrados.
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="mt-4 rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
                Selecciona una venta para registrar pagos.
              </div>
            )}
          </div>
        </section>

        <section className="mt-10 glass-panel rounded-3xl p-6 fade-up delay-2">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl text-slate">Licencias entregadas</h2>
            <span className="text-xs uppercase tracking-[0.2em] text-slate/50">
              {formatNumber(licenses.length)} entregadas
            </span>
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {licenses.map((license) => (
              <div key={license.id} className="rounded-2xl border border-smoke/60 bg-white/80 p-4">
                <p className="text-xs uppercase text-slate/50">{license.softwareName}</p>
                <p className="mt-2 text-sm font-mono text-slate">{license.licenseKey}</p>
                <p className="mt-2 text-xs text-slate/60">Asignada {formatDateTime(license.assignedAt)}</p>
              </div>
            ))}
            {licenses.length === 0 && (
              <div className="rounded-2xl border border-dashed border-smoke/60 p-6 text-center text-sm text-slate/60">
                No hay licencias entregadas para esta venta.
              </div>
            )}
          </div>
        </section>

        {showCreateCustomerModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/35 px-4">
            <div className="w-full max-w-xl rounded-[28px] border border-smoke/60 bg-white p-6 shadow-[0_30px_80px_rgba(15,23,42,0.25)]">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs uppercase tracking-[0.3em] text-slate/45">Ventas</p>
                  <h3 className="mt-2 font-display text-2xl text-slate">Crear nuevo cliente</h3>
                  <p className="mt-2 text-sm text-slate/60">
                    Guarda el cliente y sigue de inmediato con la venta.
                  </p>
                </div>
                <button
                  type="button"
                  className="rounded-full border border-smoke/60 px-3 py-1 text-[11px] uppercase tracking-[0.2em] text-slate"
                  onClick={() => {
                    setShowCreateCustomerModal(false);
                    resetNewCustomer();
                  }}
                >
                  Cerrar
                </button>
              </div>

              <div className="mt-6 grid gap-4 md:grid-cols-2">
                <label className="flex flex-col gap-2 text-sm">
                  Nombre
                  <input
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    value={newCustomer.firstname}
                    onChange={(event) =>
                      setNewCustomer((prev) => ({ ...prev, firstname: event.target.value }))
                    }
                  />
                </label>
                <label className="flex flex-col gap-2 text-sm">
                  Apellido
                  <input
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    value={newCustomer.lastname}
                    onChange={(event) =>
                      setNewCustomer((prev) => ({ ...prev, lastname: event.target.value }))
                    }
                  />
                </label>
                <label className="flex flex-col gap-2 text-sm md:col-span-2">
                  Correo
                  <input
                    type="email"
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    value={newCustomer.email}
                    onChange={(event) =>
                      setNewCustomer((prev) => ({ ...prev, email: event.target.value }))
                    }
                  />
                </label>
                <label className="flex flex-col gap-2 text-sm">
                  Telefono
                  <input
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    placeholder="+593..."
                    value={newCustomer.phone}
                    onChange={(event) =>
                      setNewCustomer((prev) => ({ ...prev, phone: event.target.value }))
                    }
                  />
                </label>
                <label className="flex flex-col gap-2 text-sm">
                  Documento
                  <input
                    className="rounded-xl border border-smoke/60 bg-white px-3 py-2"
                    value={newCustomer.taxId}
                    onChange={(event) =>
                      setNewCustomer((prev) => ({ ...prev, taxId: event.target.value }))
                    }
                  />
                </label>
              </div>

              <div className="mt-6 flex flex-wrap items-center gap-3">
                <button
                  type="button"
                  className="rounded-full bg-ink px-4 py-2 text-xs uppercase tracking-[0.2em] text-white"
                  onClick={handleCreateCustomer}
                >
                  Crear y continuar venta
                </button>
                <button
                  type="button"
                  className="rounded-full border border-smoke/60 px-4 py-2 text-xs uppercase tracking-[0.2em] text-slate"
                  onClick={() => {
                    setShowCreateCustomerModal(false);
                    resetNewCustomer();
                  }}
                >
                  Cancelar
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
