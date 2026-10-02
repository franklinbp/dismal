import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type MouseEvent,
  type ReactNode
} from "react";
import {
  AlertTriangle,
  ArrowLeft,
  BadgeCheck,
  BadgePercent,
  BookOpenCheck,
  Boxes,
  Building2,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  CircleHelp,
  Clock3,
  Copy,
  CreditCard,
  FileCheck2,
  Globe2,
  Headphones,
  HeartHandshake,
  KeyRound,
  Landmark,
  Laptop,
  LockKeyhole,
  LogOut,
  Mail,
  Menu,
  MessageCircle,
  Minus,
  MonitorSmartphone,
  PackageCheck,
  Palette,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Send,
  ServerCog,
  ShieldCheck,
  ShoppingCart,
  Sparkles,
  Store,
  Tag,
  Trash2,
  UserRound,
  UsersRound,
  X,
  Zap,
  type LucideIcon
} from "lucide-react";
import ProductCard from "./components/ProductCard";
import ProductArtwork from "./components/ProductArtwork";
import { fetchProducts } from "./lib/catalogApi";
import {
  formatContactPhone,
  formatMoney,
  getCountry,
  resolveCountry,
  telegramUrl,
  whatsappUrl
} from "./lib/country";
import { readStoredCart, storeCart, upsertCartItem } from "./lib/cart";
import {
  createStorefrontOrder,
  fetchStorefrontPaymentAccounts,
  submitStorefrontPaymentClaim
} from "./lib/storefrontOrders";
import type { StorefrontOrderResponse, StorefrontPaymentAccount } from "./lib/storefrontOrders";
import {
  authenticate,
  changePassword,
  clearSessionToken,
  fetchLatestWholesaleApplication,
  fetchCurrentUser,
  readSessionToken,
  requestPasswordReset,
  resendVerification,
  resetPassword,
  storeSessionToken,
  submitWholesaleApplication,
  updateProfile,
  verifyEmail,
  type WholesaleApplication,
  type StorefrontUser
} from "./lib/authApi";
import { cancelAccountOrder, fetchAccountOrders, type AccountOrder } from "./lib/accountApi";
import {
  resolveStoreView,
  resolveProductSlug,
  productHref,
  routeForView,
  storefrontHref,
  type StoreView
} from "./storefrontNavigation";
import { updatePageMetadata, updateProductMetadata } from "./lib/seo";
import { captureAttribution, productEventFields, trackStorefrontEvent } from "./lib/analytics";
import { searchableProductText } from "./lib/productPresentation";
import { useDialogFocus } from "./hooks/useDialogFocus";
import type {
  CartItem,
  CheckoutCustomer,
  CountryCode,
  CustomerType,
  StorefrontProduct,
  ShippingAddress
} from "./types";

type StoreCategory = {
  label: string;
  description: string;
  keywords: string[];
  icon: LucideIcon;
};

const categories: StoreCategory[] = [
  { label: "Todos", description: "Catálogo completo", keywords: [], icon: Boxes },
  { label: "Hogar", description: "Productos para cada espacio", keywords: ["hogar", "cocina", "limpieza"], icon: Store },
  { label: "Tecnología", description: "Equipos y accesorios", keywords: ["tecnología", "electrónica", "accesorios"], icon: MonitorSmartphone },
  { label: "Cuidado personal", description: "Bienestar y uso diario", keywords: ["cuidado", "personal", "bienestar"], icon: HeartHandshake },
  { label: "Oficina", description: "Suministros para trabajar", keywords: ["oficina", "papelería"], icon: Building2 },
  { label: "Mayoristas", description: "Presentaciones comerciales", keywords: ["mayorista", "caja", "distribuidor"], icon: Boxes },
  { label: "Promociones", description: "Combos y oportunidades", keywords: ["combo", "promoción", "oferta"], icon: Sparkles }
];

function getFaqItems(countryName: string) {
  return [
    {
      question: "¿Cuándo recibo mi pedido?",
      answer: "La orden queda registrada inmediatamente. Dismal Distribuciones prepara el pedido después de confirmar el pago y las existencias."
    },
    {
      question: "¿Dónde puedo volver a consultar mi clave?",
      answer: "Inicia sesión en Mi cuenta. Allí encontrarás tus órdenes y el avance desde la preparación hasta la entrega."
    },
    {
      question: "¿Los precios para distribuidores son públicos?",
      answer: `No. Solo una cuenta de distribuidor aprobada puede consultar y comprar con el precio comercial correspondiente a ${countryName}.`
    },
    {
      question: "¿Qué sucede si un producto llega con novedad?",
      answer: "Conserva el número de orden y el empaque. Nuestro equipo revisará el caso y coordinará el cambio o la solución correspondiente."
    },
    {
      question: "¿Dónde está disponible esta tienda?",
      answer: `Esta tienda atiende exclusivamente compras para ${countryName}, con su moneda, condiciones comerciales y catálogo autorizado.`
    }
  ];
}

function productPrice(product: StorefrontProduct, customerType: CustomerType): number {
  return customerType === "wholesale"
    ? product.wholesalePrice ?? product.finalPrice
    : product.finalPrice;
}

function orderStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    PENDING_PAYMENT: "Pendiente de pago",
    PAYMENT_REVIEW: "Pago en revisión",
    PAID: "Pagada",
    PREPARING: "En preparación",
    READY_FOR_DISPATCH: "Listo para despacho",
    SHIPPED: "Despachado",
    DELIVERED: "Entregado",
    CANCELLED: "Cancelada",
    FAILED: "Fallida",
    REFUNDED: "Reembolsada"
  };
  return labels[status] || status;
}

type StoreLinkProps = {
  children: ReactNode;
  className?: string;
  current?: boolean;
  onNavigate: (view: StoreView) => void;
  view: StoreView;
};

function StoreLink({ children, className, current, onNavigate, view }: StoreLinkProps) {
  function handleClick(event: MouseEvent<HTMLAnchorElement>) {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    onNavigate(view);
  }

  return (
    <a className={className} href={storefrontHref(view)} onClick={handleClick} aria-current={current ? "page" : undefined}>
      {children}
    </a>
  );
}

function safePaymentUrl(value?: string): string | null {
  if (!value) return null;
  try {
    const url = new URL(value);
    return url.protocol === "https:" ? url.href : null;
  } catch {
    return null;
  }
}

function formatOrderDate(value: string, locale: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Fecha no disponible";
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(date);
}

function App() {
  const [view, setView] = useState<StoreView>(() => resolveStoreView());
  const [productSlug, setProductSlug] = useState<string | null>(() => resolveProductSlug());
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const countryCode: CountryCode = resolveCountry().code;
  const [customerType, setCustomerType] = useState<CustomerType>("final");
  const [products, setProducts] = useState<StorefrontProduct[]>([]);
  const [selectedProduct, setSelectedProduct] = useState<StorefrontProduct | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("Todos");
  const [sortOrder, setSortOrder] = useState("featured");
  const [cart, setCart] = useState<CartItem[]>(() => readStoredCart());
  const [cartOpen, setCartOpen] = useState(false);
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "forgot" | "reset" | "verify">("login");
  const [accountOpen, setAccountOpen] = useState(false);
  const [catalogStatus, setCatalogStatus] = useState<"loading" | "ready" | "error">("loading");
  const [catalogMessage, setCatalogMessage] = useState("");
  const [catalogReloadKey, setCatalogReloadKey] = useState(0);
  const [authToken, setAuthToken] = useState(() => readSessionToken());
  const [currentUser, setCurrentUser] = useState<StorefrontUser | null>(null);
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [authStatus, setAuthStatus] = useState<"idle" | "submitting" | "error">("idle");
  const [authMessage, setAuthMessage] = useState("");
  const [accountActionToken, setAccountActionToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirmation, setNewPasswordConfirmation] = useState("");
  const [accountOrders, setAccountOrders] = useState<AccountOrder[]>([]);
  const [accountStatus, setAccountStatus] = useState<"idle" | "loading" | "loading-more" | "error">("idle");
  const [accountPage, setAccountPage] = useState(0);
  const [accountTotalPages, setAccountTotalPages] = useState(0);
  const [accountMessage, setAccountMessage] = useState("");
  const [accountActionStatus, setAccountActionStatus] = useState<"idle" | "submitting" | "error" | "success">("idle");
  const [accountActionMessage, setAccountActionMessage] = useState("");
  const [profileForm, setProfileForm] = useState({ firstName: "", lastName: "", phone: "", taxId: "", billingEmail: "" });
  const [passwordForm, setPasswordForm] = useState({ currentPassword: "", nextPassword: "", confirmation: "" });
  const [wholesaleForm, setWholesaleForm] = useState({ businessName: "", taxId: "", phone: "", website: "", notes: "" });
  const [wholesaleApplication, setWholesaleApplication] = useState<WholesaleApplication | null>(null);
  const [cancellingOrderNumber, setCancellingOrderNumber] = useState("");
  const [checkoutCustomer, setCheckoutCustomer] = useState<CheckoutCustomer>({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    taxId: "",
    password: ""
  });
  const [shippingAddress, setShippingAddress] = useState<ShippingAddress>({
    recipient: "",
    addressLine1: "",
    addressLine2: "",
    city: "",
    region: "",
    postalCode: "",
    notes: ""
  });
  const [orderStatus, setOrderStatus] = useState<"idle" | "submitting" | "created" | "error">("idle");
  const [orderMessage, setOrderMessage] = useState("");
  const [createdOrder, setCreatedOrder] = useState<StorefrontOrderResponse | null>(null);
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [checkoutMethod, setCheckoutMethod] = useState<"BANK_TRANSFER" | "CUSTOMER_CREDIT">("BANK_TRANSFER");
  const [paymentAccounts, setPaymentAccounts] = useState<StorefrontPaymentAccount[]>([]);
  const [paymentAccountsStatus, setPaymentAccountsStatus] = useState<"loading" | "ready" | "error">("loading");
  const [paymentSubmissionStatus, setPaymentSubmissionStatus] = useState<"idle" | "submitting" | "success" | "error">("idle");
  const [paymentClaim, setPaymentClaim] = useState({
    paymentAccountId: "",
    payerName: "",
    sourceBank: "",
    reference: "",
    amount: ""
  });
  const [announcement, setAnnouncement] = useState("");

  const productDialogRef = useRef<HTMLDivElement>(null);
  const cartDialogRef = useRef<HTMLDivElement>(null);
  const checkoutDialogRef = useRef<HTMLDivElement>(null);
  const authDialogRef = useRef<HTMLDivElement>(null);
  const accountDialogRef = useRef<HTMLDivElement>(null);
  const mainContentRef = useRef<HTMLElement>(null);
  const previousViewRef = useRef(view);

  const country = getCountry(countryCode);
  const faqItems = getFaqItems(country.name);
  const contactPhone = formatContactPhone(country);
  const contactTelegramUrl = telegramUrl(country);
  const contactEmailUrl = `mailto:${country.contactEmail}?subject=${encodeURIComponent(`Consulta Dismal Distribuciones ${country.name}`)}`;
  const routedProduct = useMemo(
    () => productSlug ? products.find((product) => product.slug === productSlug) || null : null,
    [productSlug, products]
  );

  useDialogFocus(Boolean(selectedProduct), productDialogRef, () => setSelectedProduct(null));
  useDialogFocus(cartOpen, cartDialogRef, () => setCartOpen(false));
  useDialogFocus(checkoutOpen, checkoutDialogRef, () => setCheckoutOpen(false));
  useDialogFocus(authOpen, authDialogRef, () => setAuthOpen(false));
  useDialogFocus(accountOpen, accountDialogRef, () => setAccountOpen(false));

  useEffect(() => {
    captureAttribution();
    if (window.location.hash.startsWith("#/")) {
      const legacyView = resolveStoreView();
      window.history.replaceState({}, "", routeForView(legacyView));
      setView(legacyView);
    }

    const syncView = () => {
      setView(resolveStoreView());
      setProductSlug(resolveProductSlug());
      setMobileMenuOpen(false);
      window.scrollTo({ top: 0, behavior: "auto" });
    };
    window.addEventListener("popstate", syncView);
    return () => {
      window.removeEventListener("popstate", syncView);
    };
  }, []);

  useEffect(() => {
    const token = new URLSearchParams(window.location.search).get("token") || "";
    if (window.location.pathname === "/cuenta/restablecer" && token) {
      setAccountActionToken(token);
      setAuthMode("reset");
      setAuthMessage("");
      setAuthOpen(true);
      return;
    }
    if (window.location.pathname === "/cuenta/verificar" && token) {
      setAccountActionToken(token);
      setAuthMode("verify");
      setAuthStatus("submitting");
      setAuthMessage("");
      setAuthOpen(true);
      verifyEmail(token)
        .then(async (message) => {
          setAuthStatus("idle");
          setAuthMessage(message);
          if (authToken) {
            const user = await fetchCurrentUser(authToken, countryCode);
            applyAuthenticatedUser(user);
          }
        })
        .catch((error) => {
          setAuthStatus("error");
          setAuthMessage(error instanceof Error ? error.message : "No se pudo verificar el correo.");
        });
    }
  }, []);

  useEffect(() => {
    if (routedProduct) {
      updateProductMetadata(routedProduct, country);
      return;
    }
    updatePageMetadata(view, country);
  }, [country, routedProduct, view]);

  useEffect(() => {
    if (previousViewRef.current === view) return;
    previousViewRef.current = view;
    window.requestAnimationFrame(() => {
      mainContentRef.current?.focus({ preventScroll: true });
      window.scrollTo({ top: 0, behavior: "auto" });
    });
  }, [view]);

  useEffect(() => {
    if (!authToken) {
      setCurrentUser(null);
      return;
    }
    fetchCurrentUser(authToken, countryCode)
      .then((user) => applyAuthenticatedUser(user))
      .catch(() => signOut());
  }, [authToken]);

  useEffect(() => {
    let mounted = true;
    setCatalogStatus("loading");
    setCatalogMessage("");
    setProducts([]);
    fetchProducts(countryCode, authToken)
      .then((items) => {
        if (!mounted) return;
        setProducts(items);
        setCatalogStatus("ready");
      })
      .catch((error) => {
        if (!mounted) return;
        setProducts([]);
        setCatalogStatus("error");
        setCatalogMessage(error instanceof Error ? error.message : "No se pudo cargar el catálogo.");
      });
    return () => {
      mounted = false;
    };
  }, [authToken, catalogReloadKey, countryCode]);

  useEffect(() => {
    let mounted = true;
    setPaymentAccountsStatus("loading");
    fetchStorefrontPaymentAccounts(countryCode)
      .then((accounts) => {
        if (!mounted) return;
        setPaymentAccounts(accounts);
        setPaymentAccountsStatus("ready");
        setPaymentClaim((current) => ({
          ...current,
          paymentAccountId: accounts.some((account) => account.id === current.paymentAccountId)
            ? current.paymentAccountId
            : accounts[0]?.id || ""
        }));
      })
      .catch(() => {
        if (!mounted) return;
        setPaymentAccounts([]);
        setPaymentAccountsStatus("error");
      });
    return () => {
      mounted = false;
    };
  }, [countryCode]);

  useEffect(() => {
    storeCart(cart);
  }, [cart]);

  const cartLines = useMemo(
    () =>
      cart
        .map((item) => {
          const product = products.find((candidate) => candidate.id === item.productId);
          if (!product) return null;
          const unitPrice = productPrice(product, customerType);
          return {
            ...item,
            product,
            unitPrice,
            lineTotal: unitPrice * item.quantity
          };
        })
        .filter(Boolean) as Array<CartItem & {
          product: StorefrontProduct;
          unitPrice: number;
          lineTotal: number;
        }>,
    [cart, customerType, products]
  );

  const filteredProducts = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    const category = categories.find((item) => item.label === selectedCategory);
    const matches = products.filter((product) => {
      const searchable = searchableProductText(product);
      const matchesQuery = !query || searchable.includes(query);
      const matchesCategory =
        !category ||
        category.keywords.length === 0 ||
        category.keywords.some((keyword) => searchable.includes(keyword));
      return matchesQuery && matchesCategory;
    });

    return [...matches].sort((first, second) => {
      if (sortOrder === "price-asc") {
        return productPrice(first, customerType) - productPrice(second, customerType);
      }
      if (sortOrder === "price-desc") {
        return productPrice(second, customerType) - productPrice(first, customerType);
      }
      if (sortOrder === "name") {
        return first.name.localeCompare(second.name, "es");
      }
      return Number(Boolean(second.badge)) - Number(Boolean(first.badge));
    });
  }, [customerType, products, searchQuery, selectedCategory, sortOrder]);

  const offerProducts = useMemo(() => {
    return products.filter((product) => product.compareAtPrice || product.badge);
  }, [products]);

  const cartTotal = cartLines.reduce((total, item) => total + item.lineTotal, 0);
  const cartCount = cart.reduce((total, item) => total + item.quantity, 0);
  const unavailableCartCount = cartCount - cartLines.reduce((total, item) => total + item.quantity, 0);

  function navigateTo(nextView: StoreView) {
    const route = routeForView(nextView);
    if (window.location.pathname !== route) window.history.pushState({}, "", route);
    setView(nextView);
    setProductSlug(null);
    window.scrollTo({ top: 0, behavior: "auto" });
    setMobileMenuOpen(false);
  }

  function showProduct(product: StorefrontProduct) {
    const route = productHref(product.slug);
    if (window.location.pathname !== route) window.history.pushState({}, "", route);
    setProductSlug(product.slug);
    setSelectedProduct(null);
    setView("catalog");
    trackStorefrontEvent({
      event: "view_item",
      country: countryCode,
      currency: country.currency,
      customerType,
      value: productPrice(product, customerType),
      ...productEventFields(product)
    });
    window.scrollTo({ top: 0, behavior: "auto" });
  }

  function showCatalog(category = "Todos") {
    setSelectedCategory(category);
    navigateTo("catalog");
  }

  function addToCart(productId: string) {
    const product = products.find((item) => item.id === productId);
    setCart((current) => upsertCartItem(current, productId, 1));
    setSelectedProduct(null);
    setCartOpen(true);
    setAnnouncement(product ? `${product.name} agregado al carrito.` : "Producto agregado al carrito.");
    if (product) {
      trackStorefrontEvent({
        event: "add_to_cart",
        country: countryCode,
        currency: country.currency,
        customerType,
        value: productPrice(product, customerType),
        ...productEventFields(product)
      });
    }
  }

  function buyNow(productId: string) {
    const product = products.find((item) => item.id === productId);
    if (!product) return;
    setCart((current) => upsertCartItem(current, productId, 1));
    setSelectedProduct(null);
    setCartOpen(false);
    setCheckoutOpen(true);
    setOrderStatus("idle");
    setPaymentSubmissionStatus("idle");
    setOrderMessage("");
    setCreatedOrder(null);
    setTermsAccepted(false);
    setAnnouncement(`${product.name} agregado. Completa tus datos para continuar.`);
    trackStorefrontEvent({
      event: "begin_checkout",
      country: countryCode,
      currency: country.currency,
      customerType,
      value: productPrice(product, customerType),
      ...productEventFields(product)
    });
  }

  function updateQuantity(productId: string, quantity: number) {
    if (quantity <= 0) {
      setCart((current) => current.filter((item) => item.productId !== productId));
      return;
    }
    setCart((current) =>
      current.map((item) =>
        item.productId === productId
          ? { ...item, quantity: Math.min(quantity, 50) }
          : item
      )
    );
  }

  function updateCheckoutField(field: keyof CheckoutCustomer, value: string) {
    setCheckoutCustomer((current) => ({ ...current, [field]: value }));
  }

  function applyAuthenticatedUser(user: StorefrontUser) {
    setCurrentUser(user);
    setCustomerType(user.customerType === "DISTRIBUTOR" ? "wholesale" : "final");
    setCheckoutMethod(user.hasCredit && user.emailVerified ? "CUSTOMER_CREDIT" : "BANK_TRANSFER");
    setCheckoutCustomer((current) => ({
      ...current,
      firstName: user.firstName || current.firstName,
      lastName: user.lastName || current.lastName,
      email: user.email,
      phone: user.phone || current.phone,
      taxId: user.taxId || current.taxId,
      password: ""
    }));
    setProfileForm({
      firstName: user.firstName || "",
      lastName: user.lastName || "",
      phone: user.phone || "",
      taxId: user.taxId || "",
      billingEmail: user.billingEmail || user.email
    });
    setWholesaleForm((current) => ({
      ...current,
      taxId: user.taxId || current.taxId,
      phone: user.phone || current.phone
    }));
  }

  function openLogin(message = "") {
    setAuthMode("login");
    setAuthStatus("idle");
    setAuthMessage(message);
    setAuthOpen(true);
  }

  function signOut() {
    clearSessionToken();
    setAuthToken("");
    setCurrentUser(null);
    setCustomerType("final");
    setCheckoutMethod("BANK_TRANSFER");
    setAccountOrders([]);
    setAccountPage(0);
    setAccountTotalPages(0);
    setAccountOpen(false);
  }

  async function submitLogin() {
    setAuthStatus("submitting");
    setAuthMessage("");
    try {
      const token = await authenticate(loginEmail.trim(), loginPassword);
      const user = await fetchCurrentUser(token, countryCode);
      storeSessionToken(token);
      setAuthToken(token);
      applyAuthenticatedUser(user);
      setLoginPassword("");
      setAuthStatus("idle");
      setAuthOpen(false);
      await openAccount(token);
    } catch (error) {
      setAuthStatus("error");
      setAuthMessage(error instanceof Error ? error.message : "No se pudo iniciar sesión.");
    }
  }

  async function submitForgotPassword() {
    setAuthStatus("submitting");
    setAuthMessage("");
    try {
      const message = await requestPasswordReset(loginEmail, countryCode);
      setAuthStatus("idle");
      setAuthMessage(message);
    } catch (error) {
      setAuthStatus("error");
      setAuthMessage(error instanceof Error ? error.message : "No se pudo procesar la solicitud.");
    }
  }

  async function submitPasswordReset() {
    if (newPassword !== newPasswordConfirmation) {
      setAuthStatus("error");
      setAuthMessage("Las contraseñas no coinciden.");
      return;
    }
    setAuthStatus("submitting");
    setAuthMessage("");
    try {
      const message = await resetPassword(accountActionToken, newPassword);
      window.history.replaceState({}, "", "/");
      setNewPassword("");
      setNewPasswordConfirmation("");
      setAuthMode("login");
      setAuthStatus("idle");
      setAuthMessage(message);
    } catch (error) {
      setAuthStatus("error");
      setAuthMessage(error instanceof Error ? error.message : "No se pudo cambiar la contraseña.");
    }
  }

  async function openAccount(token = authToken, page = 0, append = false) {
    if (!token) {
      openLogin();
      return;
    }
    setAccountOpen(true);
    setAccountStatus(append ? "loading-more" : "loading");
    setAccountMessage("");
    try {
      const [result, latestApplication, refreshedUser] = await Promise.all([
        fetchAccountOrders(token, countryCode, page),
        page === 0
          ? fetchLatestWholesaleApplication(token, countryCode)
          : Promise.resolve(wholesaleApplication),
        page === 0 ? fetchCurrentUser(token, countryCode) : Promise.resolve(currentUser)
      ]);
      setAccountOrders((current) => append
        ? [...current, ...result.content.filter((entry) => !current.some((item) => item.order.orderId === entry.order.orderId))]
        : result.content);
      setAccountPage(result.page);
      setAccountTotalPages(result.totalPages);
      if (page === 0) setWholesaleApplication(latestApplication || null);
      if (page === 0 && refreshedUser) applyAuthenticatedUser(refreshedUser);
      setAccountStatus("idle");
    } catch (error) {
      setAccountStatus("error");
      setAccountMessage(error instanceof Error ? error.message : "No se pudo cargar el historial.");
    }
  }

  async function sendVerificationEmail() {
    if (!authToken) return;
    setAccountActionStatus("submitting");
    setAccountActionMessage("");
    try {
      const message = await resendVerification(authToken, countryCode);
      setAccountActionStatus("success");
      setAccountActionMessage(message);
    } catch (error) {
      setAccountActionStatus("error");
      setAccountActionMessage(error instanceof Error ? error.message : "No se pudo enviar el enlace.");
    }
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!authToken) return;
    setAccountActionStatus("submitting");
    setAccountActionMessage("");
    try {
      const user = await updateProfile(authToken, countryCode, profileForm);
      applyAuthenticatedUser(user);
      setAccountActionStatus("success");
      setAccountActionMessage("Datos de cuenta actualizados.");
    } catch (error) {
      setAccountActionStatus("error");
      setAccountActionMessage(error instanceof Error ? error.message : "No se pudo actualizar la cuenta.");
    }
  }

  async function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!authToken) return;
    if (passwordForm.nextPassword !== passwordForm.confirmation) {
      setAccountActionStatus("error");
      setAccountActionMessage("Las contraseñas nuevas no coinciden.");
      return;
    }
    setAccountActionStatus("submitting");
    setAccountActionMessage("");
    try {
      const message = await changePassword(authToken, passwordForm.currentPassword, passwordForm.nextPassword);
      setPasswordForm({ currentPassword: "", nextPassword: "", confirmation: "" });
      setAccountActionStatus("success");
      setAccountActionMessage(message);
      window.setTimeout(() => {
        signOut();
        openLogin("La contraseña cambió. Inicia sesión nuevamente.");
      }, 1200);
    } catch (error) {
      setAccountActionStatus("error");
      setAccountActionMessage(error instanceof Error ? error.message : "No se pudo cambiar la contraseña.");
    }
  }

  async function saveWholesaleApplication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!authToken) return;
    setAccountActionStatus("submitting");
    setAccountActionMessage("");
    try {
      const application = await submitWholesaleApplication(authToken, {
        country: countryCode,
        ...wholesaleForm
      });
      setWholesaleApplication(application);
      setAccountActionStatus("success");
      setAccountActionMessage("Solicitud de distribuidor enviada para revisión.");
    } catch (error) {
      setAccountActionStatus("error");
      setAccountActionMessage(error instanceof Error ? error.message : "No se pudo enviar la solicitud.");
    }
  }

  async function cancelOrder(order: AccountOrder["order"]) {
    if (!authToken || order.status !== "PENDING_PAYMENT") return;
    if (!window.confirm(`¿Cancelar el pedido ${order.orderNumber}? El historial se conservará.`)) return;
    setCancellingOrderNumber(order.orderNumber);
    setAccountActionStatus("submitting");
    setAccountActionMessage("");
    try {
      const updated = await cancelAccountOrder(authToken, order.orderNumber);
      setAccountOrders((current) => current.map((entry) =>
        entry.order.orderId === updated.orderId ? { ...entry, order: updated } : entry
      ));
      setCreatedOrder((current) => current?.orderId === updated.orderId ? null : current);
      setAccountActionStatus("success");
      setAccountActionMessage("Pedido cancelado. Puedes volver a agregar sus productos al carrito.");
    } catch (error) {
      setAccountActionStatus("error");
      setAccountActionMessage(error instanceof Error ? error.message : "No se pudo cancelar el pedido.");
    } finally {
      setCancellingOrderNumber("");
    }
  }

  function repeatOrder(order: AccountOrder["order"]) {
    const availableProductIds = new Set(products.map((product) => product.id));
    const nextCart = order.items
      .filter((item) => availableProductIds.has(item.productId))
      .map((item) => ({ productId: item.productId, quantity: item.quantity }));
    if (nextCart.length === 0) {
      setAccountActionStatus("error");
      setAccountActionMessage("Los productos de este pedido ya no están disponibles en el catálogo actual.");
      return;
    }
    setCart(nextCart);
    setAccountOpen(false);
    setCartOpen(true);
    setAnnouncement("Productos agregados nuevamente al carrito.");
  }

  async function copyBankValue(value: string, label: string) {
    try {
      await navigator.clipboard.writeText(value);
      setAnnouncement(`${label} copiado.`);
    } catch {
      setAnnouncement(`No se pudo copiar ${label.toLowerCase()}.`);
    }
  }

  function selectCustomerType(nextType: CustomerType) {
    if (nextType === "wholesale" && currentUser?.customerType !== "DISTRIBUTOR") {
      openLogin("Inicia sesión con una cuenta de distribuidor aprobada para consultar su precio.");
      return;
    }
    setCustomerType(nextType);
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSelectedCategory("Todos");
    navigateTo("catalog");
  }

  function contactByWhatsapp(context = "Necesito ayuda para elegir un producto") {
    const url = whatsappUrl(country, `${context}. Estoy comprando desde ${country.name}.`);
    trackStorefrontEvent({
      event: "contact_whatsapp",
      country: countryCode,
      currency: country.currency,
      customerType
    });
    if (url) {
      window.open(url, "_blank", "noopener,noreferrer");
      return;
    }
    navigateTo("help");
    setAnnouncement(`El canal de WhatsApp para ${country.name} todavía no está disponible. Mientras tanto, utiliza el centro de ayuda.`);
  }

  async function submitOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (cartLines.length === 0) return;
    if (
      !checkoutCustomer.firstName.trim() ||
      !checkoutCustomer.lastName.trim() ||
      !checkoutCustomer.email.trim() ||
      !checkoutCustomer.phone.trim() ||
      !shippingAddress.addressLine1.trim() ||
      !shippingAddress.city.trim() ||
      !shippingAddress.region.trim()
    ) {
      setOrderStatus("error");
      setOrderMessage("Completa tus datos de contacto y la dirección de entrega.");
      return;
    }
    if (!authToken && (checkoutCustomer.password || "").length < 8) {
      setOrderStatus("error");
      setOrderMessage("Crea una contraseña de al menos 8 caracteres para proteger tus compras.");
      return;
    }
    if (!termsAccepted) {
      setOrderStatus("error");
      setOrderMessage("Debes aceptar los términos de compra y la política de privacidad.");
      return;
    }
    if (customerType === "wholesale" && currentUser?.customerType !== "DISTRIBUTOR") {
      setOrderStatus("error");
      setOrderMessage("El precio de distribuidor requiere una cuenta aprobada e inicio de sesión.");
      return;
    }
    if (checkoutMethod === "CUSTOMER_CREDIT") {
      if (!currentUser?.hasCredit || !currentUser.emailVerified) {
        setOrderStatus("error");
        setOrderMessage("El crédito requiere una cuenta verificada con cupo aprobado.");
        return;
      }
      if (currentUser.creditAvailable < cartTotal) {
        setOrderStatus("error");
        setOrderMessage("El cupo de crédito disponible no cubre el total de la orden.");
        return;
      }
    }

    setOrderStatus("submitting");
    setOrderMessage("");
    setCreatedOrder(null);
    try {
      const normalizedCustomer: CheckoutCustomer = {
        firstName: checkoutCustomer.firstName.trim(),
        lastName: checkoutCustomer.lastName.trim(),
        email: checkoutCustomer.email.trim().toLowerCase(),
        phone: checkoutCustomer.phone.trim(),
        taxId: checkoutCustomer.taxId.trim(),
        password: authToken ? undefined : checkoutCustomer.password
      };
      const order = await createStorefrontOrder(
        {
          country: countryCode,
          customerType,
          checkoutMethod,
          customer: normalizedCustomer,
          shipping: {
            ...shippingAddress,
            recipient: shippingAddress.recipient.trim() || `${normalizedCustomer.firstName} ${normalizedCustomer.lastName}`.trim(),
            addressLine1: shippingAddress.addressLine1.trim(),
            addressLine2: shippingAddress.addressLine2.trim(),
            city: shippingAddress.city.trim(),
            region: shippingAddress.region.trim(),
            postalCode: shippingAddress.postalCode.trim(),
            notes: shippingAddress.notes.trim()
          },
          items: cart
        },
        authToken
      );
      setOrderStatus("created");
      trackStorefrontEvent({
        event: "purchase_order_created",
        country: countryCode,
        currency: country.currency,
        customerType,
        value: order.total,
        orderNumber: order.orderNumber
      });
      setCreatedOrder(order);
      setPaymentSubmissionStatus("idle");
      setPaymentClaim((current) => ({
        ...current,
        payerName: `${normalizedCustomer.firstName} ${normalizedCustomer.lastName}`.trim(),
        sourceBank: "",
        reference: "",
        amount: String(order.total)
      }));

      if (order.demo) {
        setOrderMessage("Simulación completada. En localhost no se creó una orden ni se procesó un pago real.");
        return;
      }

      setCart([]);
      setOrderMessage(
        order.status === "PREPARING" && order.checkoutMethod === "CUSTOMER_CREDIT"
          ? "Compra aprobada con tu crédito Dismal. El pedido entró en preparación."
          : order.paymentUrl
          ? "Orden creada. Serás dirigido a la pasarela de pago segura."
          : `Orden ${order.orderNumber} creada. Registra ahora los datos de tu transferencia.`
      );

      if (!authToken && checkoutCustomer.password) {
        try {
          const token = await authenticate(normalizedCustomer.email, checkoutCustomer.password);
          const user = await fetchCurrentUser(token, countryCode);
          storeSessionToken(token);
          setAuthToken(token);
          applyAuthenticatedUser(user);
          setCheckoutCustomer((current) => ({ ...current, password: "" }));
        } catch {
          setOrderMessage(`Orden ${order.orderNumber} creada. Inicia sesión con el correo de compra para consultar su estado.`);
        }
      }

      const paymentUrl = safePaymentUrl(order.paymentUrl);
      if (paymentUrl) window.location.assign(paymentUrl);
      if (order.paymentUrl && !paymentUrl) {
        setOrderMessage(`Orden ${order.orderNumber} creada, pero el enlace de pago no superó la validación de seguridad. Consulta Mi cuenta.`);
      }
    } catch (error) {
      setOrderStatus("error");
      setOrderMessage(error instanceof Error ? error.message : "No se pudo crear la orden.");
    }
  }

  async function submitPaymentClaim(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!createdOrder || createdOrder.checkoutMethod !== "BANK_TRANSFER") return;
    if (!authToken) {
      setCheckoutOpen(false);
      openLogin("Inicia sesión para registrar y consultar el pago de tu orden.");
      return;
    }

    const amount = Number(paymentClaim.amount);
    if (!paymentClaim.paymentAccountId || !paymentClaim.payerName.trim() ||
        !paymentClaim.sourceBank.trim() || !paymentClaim.reference.trim() ||
        !Number.isFinite(amount) || amount <= 0) {
      setPaymentSubmissionStatus("error");
      setOrderMessage("Completa la cuenta receptora, pagador, banco, referencia y monto.");
      return;
    }

    setPaymentSubmissionStatus("submitting");
    setOrderMessage("");
    try {
      const updated = await submitStorefrontPaymentClaim(createdOrder.orderNumber, authToken, {
        paymentAccountId: paymentClaim.paymentAccountId,
        payerName: paymentClaim.payerName.trim(),
        sourceBank: paymentClaim.sourceBank.trim(),
        reference: paymentClaim.reference.trim(),
        amount
      });
      setCreatedOrder(updated);
      setPaymentSubmissionStatus("success");
      setOrderMessage("Pago registrado. Dismal Distribuciones verificará el movimiento bancario antes de preparar el pedido.");
    } catch (error) {
      setPaymentSubmissionStatus("error");
      setOrderMessage(error instanceof Error ? error.message : "No se pudo registrar el pago.");
    }
  }

  function renderCustomerTypeControl() {
    return (
      <div className={`price-audience ${customerType === "wholesale" ? "wholesale" : ""}`} aria-label="Precio activo">
        {customerType === "wholesale" ? <BadgeCheck size={18} aria-hidden="true" /> : <Tag size={18} aria-hidden="true" />}
        <span>
          <strong>{customerType === "wholesale" ? "Precio distribuidor aplicado" : "Precios para cliente final"}</strong>
          <small>{customerType === "wholesale" ? "Cuenta de distribuidor aprobada" : "El precio se valida al crear la orden"}</small>
        </span>
        {!currentUser ? <button type="button" onClick={() => openLogin("Ingresa con tu cuenta de distribuidor para consultar precios privados.")}>Acceso distribuidores</button> : null}
      </div>
    );
  }

  function renderProductGrid(items: StorefrontProduct[]) {
    if (catalogStatus === "loading") {
      return (
        <div className="loading-grid" aria-label="Cargando productos" aria-busy="true">
          <span /><span /><span /><span />
        </div>
      );
    }
    if (catalogStatus === "error") {
      return (
        <div className="catalog-message catalog-error" role="alert">
          <AlertTriangle size={26} aria-hidden="true" />
          <div>
            <strong>No pudimos validar el catálogo</strong>
            <span>{catalogMessage || "Revisa tu conexión e intenta nuevamente."}</span>
          </div>
          <button type="button" onClick={() => setCatalogReloadKey((current) => current + 1)}>
            <RefreshCw size={16} aria-hidden="true" /> Reintentar
          </button>
        </div>
      );
    }
    if (products.length === 0) {
      return (
        <div className="catalog-message" role="status">
          <CircleHelp size={26} aria-hidden="true" />
          <div>
            <strong>No hay productos disponibles en este momento</strong>
            <span>El catálogo está conectado, pero todavía no tiene productos válidos para {country.name}.</span>
          </div>
        </div>
      );
    }
    if (items.length === 0) {
      return (
        <div className="catalog-message" role="status">
          <Search size={26} aria-hidden="true" />
          <div>
            <strong>No encontramos productos con esos filtros</strong>
            <span>Prueba otra categoría o utiliza una búsqueda más amplia.</span>
          </div>
          <button type="button" onClick={() => { setSearchQuery(""); setSelectedCategory("Todos"); }}>
            Limpiar filtros
          </button>
        </div>
      );
    }

    return (
      <div className="product-grid">
        {items.map((product) => {
          const compareAt =
            product.compareAtPrice && customerType === "final"
              ? formatMoney(product.compareAtPrice, country)
              : undefined;
          return (
            <ProductCard
              key={product.id}
              product={product}
              formattedPrice={formatMoney(productPrice(product, customerType), country)}
              formattedCompareAtPrice={compareAt}
              priceLabel={customerType === "wholesale" ? "Precio distribuidor" : "Precio para cliente final"}
              onBuyNow={() => buyNow(product.id)}
              onDetails={() => showProduct(product)}
            />
          );
        })}
      </div>
    );
  }

  function renderProductDetail() {
    if (!productSlug) return null;
    if (catalogStatus === "loading") {
      return <section className="page-section product-page"><div className="product-page-loading" aria-busy="true"><RefreshCw aria-hidden="true" /><strong>Validando producto y precio</strong></div></section>;
    }
    if (catalogStatus === "error") {
      return <section className="page-section product-page"><div className="catalog-message catalog-error" role="alert"><AlertTriangle aria-hidden="true" /><div><strong>No pudimos validar este producto</strong><span>{catalogMessage}</span></div><button type="button" onClick={() => setCatalogReloadKey((current) => current + 1)}>Reintentar</button></div></section>;
    }
    if (!routedProduct) {
      return <section className="page-section product-page"><div className="catalog-message" role="status"><CircleHelp aria-hidden="true" /><div><strong>Producto no disponible</strong><span>Puede haber cambiado de nombre o ya no estar habilitado para {country.name}.</span></div><button type="button" onClick={() => showCatalog()}>Ver catálogo</button></div></section>;
    }

    const product = routedProduct;
    const facts = [
      ["Plataforma", product.platform],
      ["Presentación", product.licenseType],
      ["Duración", product.duration],
      ["Dispositivos", product.devices],
      ["Activación", product.activationMode],
      ["Región", product.region || country.name],
      ["Idioma", product.language]
    ].filter((item): item is [string, string] => Boolean(item[1]));
    const related = products.filter((item) => item.id !== product.id && item.category === product.category).slice(0, 4);

    return (
      <section className="product-page">
        <nav className="breadcrumbs" aria-label="Ruta de navegación">
          <button type="button" onClick={() => navigateTo("home")}>Inicio</button><ChevronRight aria-hidden="true" />
          <button type="button" onClick={() => showCatalog(product.category)}>{product.category}</button><ChevronRight aria-hidden="true" />
          <span>{product.shortName}</span>
        </nav>
        <div className="product-detail-layout">
          <div className="product-detail-media"><ProductArtwork product={product} large /></div>
          <div className="product-detail-copy">
            <span className="product-kicker">{product.brand || product.category} · {product.platform}</span>
            <h1>{product.name}</h1>
            <p className="product-lead">{product.longDescription}</p>
            <div className="product-detail-benefits">
              {product.benefits.map((benefit) => <span key={benefit}><CheckCircle2 aria-hidden="true" />{benefit}</span>)}
            </div>
            <div className="product-detail-purchase">
              <div className="product-detail-price">
                <span>{customerType === "wholesale" ? "Precio distribuidor" : "Precio para cliente final"}</span>
                {product.compareAtPrice && customerType === "final" ? <small>{formatMoney(product.compareAtPrice, country)}</small> : null}
                <strong>{formatMoney(productPrice(product, customerType), country)}</strong>
                <em>Precio y disponibilidad se confirman al crear la orden.</em>
              </div>
              <button className="primary-button" type="button" onClick={() => addToCart(product.id)}><ShoppingCart aria-hidden="true" />Agregar al carrito</button>
              <button className="secondary-button" type="button" onClick={() => contactByWhatsapp(`Necesito confirmar la compatibilidad de ${product.name}`)}><MessageCircle aria-hidden="true" />Consultar compatibilidad</button>
            </div>
            {customerType === "wholesale" ? <p className="private-price-note"><BadgeCheck aria-hidden="true" />Precio privado habilitado para tu cuenta distribuidor.</p> : null}
          </div>
        </div>

        <div className="product-information">
          <section>
            <span>Ficha del producto</span>
            <h2>Información para decidir con claridad</h2>
            <dl>{facts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>
          </section>
          <section>
            <span>Entrega y soporte</span>
            <h2>Qué sucede después de crear la orden</h2>
            <div className="delivery-points">
              <p><PackageCheck aria-hidden="true" /><span><strong>Entrega física</strong>{product.delivery}</span></p>
              <p><CreditCard aria-hidden="true" /><span><strong>Confirmación de pago</strong>La orden avanza cuando el sistema registra una confirmación válida.</span></p>
              <p><Headphones aria-hidden="true" /><span><strong>Soporte postventa</strong>{product.support}</span></p>
            </div>
          </section>
        </div>

        <div className="compatibility-callout">
          <ShieldCheck aria-hidden="true" />
          <div><strong>Confirma antes de comprar</strong><span>Revisa edición, sistema operativo, modalidad, región y cantidad de dispositivos. Si algún dato no aparece, consúltalo antes de crear la orden.</span></div>
          <button type="button" onClick={() => contactByWhatsapp(`Necesito ayuda antes de comprar ${product.name}`)}>Solicitar ayuda</button>
        </div>

        {related.length > 0 ? <section className="content-section related-products"><div className="section-heading-row"><div className="section-heading"><span>También puede servirte</span><h2>Productos relacionados</h2></div><button className="text-button" type="button" onClick={() => showCatalog(product.category)}>Ver categoría <ChevronRight aria-hidden="true" /></button></div>{renderProductGrid(related)}</section> : null}
      </section>
    );
  }

  function renderHome() {
    return (
      <>
        <section className="home-hero" aria-labelledby="home-title">
          <img src="/assets/dismal-store-hero-v2.webp" alt="Productos disponibles en Dismal Distribuciones" width="1915" height="853" decoding="async" />
          <div className="hero-overlay" />
          <div className="hero-content">
            <span className="hero-eyebrow">Productos físicos para {country.name}</span>
            <h1 id="home-title">Todo lo que necesitas, listo para llegar hasta ti</h1>
            <p>Compara productos, confirma existencias y sigue tu pedido desde la preparación hasta la entrega.</p>
            <div className="hero-actions">
              <button className="primary-button" type="button" onClick={() => showCatalog()}>Ver productos <ChevronRight aria-hidden="true" /></button>
              <button className="secondary-button hero-secondary" type="button" onClick={() => contactByWhatsapp()}><MessageCircle aria-hidden="true" />Consultar por WhatsApp</button>
            </div>
            <div className="hero-proof" aria-label="Condiciones de la tienda">
              <span><Tag aria-hidden="true" />Precios visibles</span>
              <span><PackageCheck aria-hidden="true" />Despacho con seguimiento</span>
              <span><BookOpenCheck aria-hidden="true" />Seguimiento de orden</span>
              <span><Headphones aria-hidden="true" />Soporte postventa</span>
            </div>
          </div>
        </section>

        <section className="service-bar" aria-label="Cómo compra el cliente">
          <div><Search aria-hidden="true" /><span><strong>Elige con claridad</strong>Compara edición y compatibilidad</span></div>
          <div><CreditCard aria-hidden="true" /><span><strong>Pago confirmado</strong>La orden conserva su estado</span></div>
          <div><PackageCheck aria-hidden="true" /><span><strong>Entrega física</strong>Seguimiento de despacho</span></div>
          <div><HeartHandshake aria-hidden="true" /><span><strong>Soporte postventa</strong>Ayuda después de recibir</span></div>
        </section>

        <section className="content-section category-section" aria-labelledby="category-title">
          <div className="section-heading-row"><div className="section-heading"><span>Explora el catálogo</span><h2 id="category-title">Encuentra el software adecuado</h2><p>Categorías organizadas para identificar más rápido la solución que necesitas.</p></div><button className="text-button" type="button" onClick={() => showCatalog()}>Ver todo <ChevronRight aria-hidden="true" /></button></div>
          <div className="category-grid">
            {categories.slice(1).map(({ label, description, icon: Icon }) => <button type="button" key={label} onClick={() => showCatalog(label)}><Icon aria-hidden="true" /><span><strong>{label}</strong><small>{description}</small></span><ChevronRight aria-hidden="true" /></button>)}
          </div>
        </section>

        <section className="content-section featured-section" aria-labelledby="featured-title">
          <div className="section-heading-row"><div className="section-heading"><span>Catálogo disponible</span><h2 id="featured-title">Productos destacados</h2><p>Precios y existencias obtenidos directamente de Dismal Distribuciones para {country.name}.</p></div>{renderCustomerTypeControl()}</div>
          {renderProductGrid(products.slice(0, 8))}
        </section>

        <section className="decision-band" aria-labelledby="decision-title">
          <div><span>Compra informada</span><h2 id="decision-title">Elige la presentación adecuada</h2><p>Antes de comprar, confirma la presentación, las medidas, la cantidad y la cobertura de entrega. Nuestro equipo puede ayudarte a revisar estos datos.</p><button className="secondary-button" type="button" onClick={() => contactByWhatsapp("Necesito orientación para elegir un producto")}>Hablar con soporte <MessageCircle aria-hidden="true" /></button></div>
          <div className="decision-checklist">{["Edición y versión correctas", "Sistema operativo compatible", "Duración y dispositivos", "Modalidad de activación"].map((item) => <span key={item}><CheckCircle2 aria-hidden="true" />{item}</span>)}</div>
        </section>

        <section className="content-section process-section" aria-labelledby="process-title">
          <div className="section-heading centered-heading"><span>Proceso de compra</span><h2 id="process-title">De la elección a tu puerta</h2><p>Cada orden tiene un estado visible desde la cuenta del comprador.</p></div>
          <div className="process-grid">
            {[[Search, "1", "Compara", "Revisa producto, precio y existencias."], [ShoppingCart, "2", "Crea la orden", "Completa tus datos y confirma el carrito."], [CreditCard, "3", "Registra el pago", "El pago queda sujeto a confirmación."], [PackageCheck, "4", "Recibe tu pedido", "Consulta preparación, despacho y entrega desde tu cuenta."]].map(([Icon, number, title, body]) => { const StepIcon = Icon as LucideIcon; return <div className="process-step" key={String(title)}><span>{String(number)}</span><StepIcon aria-hidden="true" /><strong>{String(title)}</strong><p>{String(body)}</p></div>; })}
          </div>
        </section>

        <section className="support-band">
          <div><Headphones aria-hidden="true" /><span><strong>Acompañamiento antes y después de comprar</strong>Consulta compatibilidad, revisa tu orden y solicita orientación de activación.</span></div>
          <div><button type="button" onClick={() => navigateTo("help")}>Centro de ayuda</button><button type="button" onClick={() => contactByWhatsapp()}>WhatsApp</button></div>
        </section>

        <section className="content-section home-faq">
          <div className="section-heading"><span>Antes de comprar</span><h2>Preguntas frecuentes</h2><p>Respuestas directas sobre pago, entrega, cuenta y soporte.</p></div>
          <div className="faq-list">{faqItems.slice(0, 4).map(({ question, answer }) => <details key={question}><summary>{question}<ChevronDown aria-hidden="true" /></summary><p>{answer}</p></details>)}</div>
        </section>

        <section className="wholesale-band">
          <div><span>Programa para distribuidores</span><h2>Precios privados para distribuidores aprobados</h2><p>La sesión autorizada habilita automáticamente el precio distribuidor, historial y recompra sin exponer tarifas comerciales al público.</p></div>
          <div className="band-actions"><button className="light-button" type="button" onClick={() => navigateTo("wholesale")}>Conocer el programa</button><button className="outline-light-button" type="button" onClick={() => currentUser ? openAccount() : openLogin()}>Ingresar</button></div>
        </section>
      </>
    );
  }

  function renderCatalog() {
    return (
      <section className="page-section catalog-page">
        <div className="page-intro">
          <span>Tienda digital para {country.name}</span>
          <h1>Software para comprar con claridad</h1>
          <p>Filtra por categoría, compara precios y confirma la compatibilidad antes de agregar un producto.</p>
        </div>
        <div className="catalog-toolbar">
          <div>
            <strong>{filteredProducts.length}</strong>
            <span>productos encontrados</span>
          </div>
          {renderCustomerTypeControl()}
          <label>
            Ordenar
            <select value={sortOrder} onChange={(event) => setSortOrder(event.target.value)}>
              <option value="featured">Destacados</option>
              <option value="price-asc">Menor precio</option>
              <option value="price-desc">Mayor precio</option>
              <option value="name">Nombre</option>
            </select>
          </label>
        </div>
        <div className="catalog-layout">
          <aside className="catalog-filters" aria-label="Filtros de catálogo">
            <div className="filter-heading"><Boxes size={19} aria-hidden="true" /><strong>Categorías</strong></div>
            {categories.map(({ label, icon: Icon }) => (
              <button
                className={selectedCategory === label ? "active" : ""}
                type="button"
                key={label}
                onClick={() => setSelectedCategory(label)}
              >
                <Icon size={17} aria-hidden="true" /> {label}
                <ChevronRight size={15} aria-hidden="true" />
              </button>
            ))}
            <div className="filter-assurance">
              <ShieldCheck size={21} aria-hidden="true" />
              <strong>Validación del servidor</strong>
              <span>El precio y la disponibilidad se comprueban al crear la orden.</span>
            </div>
          </aside>
          <div className="catalog-results">
            {searchQuery ? (
              <div className="active-search">
                <span>Resultados para <strong>“{searchQuery}”</strong></span>
                <button type="button" onClick={() => setSearchQuery("")}><X size={15} aria-hidden="true" /> Limpiar</button>
              </div>
            ) : null}
            {renderProductGrid(filteredProducts)}
          </div>
        </div>
      </section>
    );
  }

  function renderOffers() {
    return (
      <section className="page-section">
        <div className="offers-intro">
          <div>
            <span><BadgePercent size={18} aria-hidden="true" /> Oportunidades vigentes</span>
            <h1>Ofertas visibles y condiciones claras</h1>
            <p>Promociones sujetas a disponibilidad. El servidor confirma precio, país y stock antes de crear la orden.</p>
          </div>
          <Tag size={84} aria-hidden="true" />
        </div>
        <div className="section-heading-row offers-heading">
          <div className="section-heading">
            <span>Catálogo promocional</span>
            <h2>Productos seleccionados</h2>
          </div>
          {renderCustomerTypeControl()}
        </div>
        {catalogStatus === "ready" && offerProducts.length === 0 ? (
          <div className="catalog-message" role="status">
            <BadgePercent size={26} aria-hidden="true" />
            <div><strong>No hay ofertas activas</strong><span>El catálogo general continúa disponible con sus precios vigentes.</span></div>
            <button type="button" onClick={() => showCatalog()}>Ver catálogo</button>
          </div>
        ) : renderProductGrid(offerProducts)}
        <div className="offer-conditions">
          <Clock3 size={23} aria-hidden="true" />
          <div><strong>Condiciones visibles antes de pagar</strong><span>Una oferta no cambia la política de entrega, soporte ni validación del pago.</span></div>
        </div>
      </section>
    );
  }

  function renderWholesale() {
    return (
      <section className="page-section">
        <div className="business-hero">
          <div>
            <span>Canal distribuidores</span>
            <h1>Compra recurrente con precios privados y control comercial</h1>
            <p>La experiencia para distribuidores utiliza la misma tienda, pero protege los precios especiales mediante una cuenta previamente aprobada.</p>
            <div className="hero-actions">
              <button className="primary-button" type="button" onClick={() => currentUser ? openAccount() : openLogin()}>Ingresar como distribuidor</button>
              <button className="secondary-button" type="button" onClick={() => showCatalog()}>Revisar catálogo público</button>
            </div>
          </div>
          <div className="business-summary">
            <UsersRound size={33} aria-hidden="true" />
            <strong>Perfil DISTRIBUTOR</strong>
            <span>La API valida la autorización antes de mostrar o aplicar el precio comercial.</span>
          </div>
        </div>
        <div className="business-benefits">
          {[
            [LockKeyhole, "Precios protegidos", "No se publican ni se aceptan sin autenticación."],
            [Boxes, "Compra por volumen", "Cantidades controladas y recalculadas en servidor."],
            [BookOpenCheck, "Historial unificado", "Órdenes, despachos y entregas en la misma cuenta."],
            [Globe2, `Operación en ${country.name}`, `Moneda y lista comercial configuradas para ${country.name}.`]
          ].map(([Icon, title, body]) => {
            const BenefitIcon = Icon as LucideIcon;
            return <div key={String(title)}><BenefitIcon size={25} aria-hidden="true" /><strong>{String(title)}</strong><p>{String(body)}</p></div>;
          })}
        </div>
        <div className="approval-process">
          <div className="section-heading">
            <span>Acceso responsable</span>
            <h2>Cómo se habilita una cuenta de distribuidor</h2>
            <p>La aprobación no se realiza desde un botón público: Dismal Distribuciones valida identidad, país y relación comercial.</p>
          </div>
          <ol>
            <li><span>1</span><div><strong>Solicitud comercial</strong><p>Entrega los datos de tu negocio y mercado de operación.</p></div></li>
            <li><span>2</span><div><strong>Revisión Dismal</strong><p>El equipo valida la información y define la lista aplicable.</p></div></li>
            <li><span>3</span><div><strong>Activación</strong><p>Tu cuenta obtiene el perfil de distribuidor y puede comprar.</p></div></li>
          </ol>
          <button className="primary-button" type="button" onClick={() => navigateTo("help")}>Consultar requisitos</button>
        </div>
      </section>
    );
  }

  function renderAbout() {
    return (
      <section className="page-section">
        <div className="about-hero">
          <div className="about-brand"><img src="/assets/dismal-logo.png" alt="Dismal Distribuciones" /></div>
          <div>
            <span>Quiénes somos</span>
            <h1>Una operación digital conectada con inventario, ventas y soporte</h1>
            <p>Dismal Distribuciones integra la tienda con la administración central de productos, existencias, clientes, pagos y despachos.</p>
          </div>
        </div>
        <div className="principles-grid">
          {[
            [ShieldCheck, "Seguridad por diseño", "La tienda protege los pagos y los datos de entrega."],
            [BadgeCheck, "Información comprobable", "Cada orden conserva número, estado, productos y seguimiento."],
            [HeartHandshake, "Servicio comprensible", "Explicamos qué ocurre antes, durante y después de la compra."],
            [Zap, "Automatización responsable", "Automatizamos tareas sin eliminar las validaciones de pago y stock."]
          ].map(([Icon, title, body]) => {
            const PrincipleIcon = Icon as LucideIcon;
            return <div key={String(title)}><PrincipleIcon size={27} aria-hidden="true" /><strong>{String(title)}</strong><p>{String(body)}</p></div>;
          })}
        </div>
        <div className="architecture-story">
          <div>
            <span>Arquitectura Dismal</span>
            <h2>La página pública es solo una parte del sistema</h2>
            <p>La tienda ofrece una interfaz rápida. La API central valida usuarios, precios, existencias, órdenes y despachos sin exponer lógica sensible en el navegador.</p>
          </div>
          <div className="architecture-flow" aria-label="Flujo de arquitectura">
            <span><Store size={21} aria-hidden="true" /> Tienda {country.name}</span>
            <ChevronRight size={20} aria-hidden="true" />
            <span><LockKeyhole size={21} aria-hidden="true" /> API segura</span>
            <ChevronRight size={20} aria-hidden="true" />
            <span><Building2 size={21} aria-hidden="true" /> Dismal central</span>
          </div>
        </div>
      </section>
    );
  }

  function renderHelp() {
    return (
      <section className="page-section help-page">
        <div className="help-hero">
          <div>
            <span>Centro de ayuda</span>
            <h1>Respuestas claras antes y después de comprar</h1>
            <p>Consulta compatibilidad, entrega, garantía y estado de tus órdenes desde un solo lugar.</p>
          </div>
          <CircleHelp size={78} aria-hidden="true" />
        </div>
        <div className="support-options">
          <button type="button" onClick={() => currentUser ? openAccount() : openLogin()}>
            <UserRound size={25} aria-hidden="true" /><span><strong>Consultar mis órdenes</strong><small>Estado, productos y entrega</small></span><ChevronRight size={18} aria-hidden="true" />
          </button>
          <button type="button" onClick={() => showCatalog()}>
            <BookOpenCheck size={25} aria-hidden="true" /><span><strong>Revisar productos</strong><small>Descripción y compatibilidad</small></span><ChevronRight size={18} aria-hidden="true" />
          </button>
          <button type="button" onClick={() => navigateTo("refunds")}>
            <ShieldCheck size={25} aria-hidden="true" /><span><strong>Garantía y devoluciones</strong><small>Condiciones de soporte</small></span><ChevronRight size={18} aria-hidden="true" />
          </button>
        </div>
        <div className="faq-layout">
          <div className="section-heading">
            <span>Preguntas frecuentes</span>
            <h2>Lo que debes saber</h2>
            <p>La información comercial específica de tu orden prevalece sobre estas respuestas generales.</p>
          </div>
          <div className="faq-list">
            {faqItems.map(({ question, answer }) => (
              <details key={question}>
                <summary>{question}<ChevronDown size={18} aria-hidden="true" /></summary>
                <p>{answer}</p>
              </details>
            ))}
          </div>
        </div>
        <div className="support-contact">
          <Headphones size={30} aria-hidden="true" />
          <div><strong>¿Tu caso requiere revisión?</strong><span>Ten disponible el número de orden, correo de compra y una captura del mensaje de activación.</span></div>
          <button type="button" onClick={() => currentUser ? openAccount() : openLogin()}>Abrir mi cuenta</button>
        </div>
      </section>
    );
  }

  function renderLegalPage() {
    const content = {
      terms: {
        title: "Términos de compra",
        intro: "Condiciones generales para utilizar Dismal Distribuciones.",
        sections: [
          ["Orden y precio", "La orden se crea con los productos seleccionados, pero el servidor valida nuevamente país, tipo de cliente, precio y disponibilidad."],
          ["Pago", "Una orden pendiente no representa pago confirmado. Dismal Distribuciones prepara el pedido únicamente después de recibir una confirmación válida."],
          ["Entrega física", "El pedido, la dirección y su estado se asocian a la cuenta del comprador y al correo suministrado."],
          ["Compra correcta", "El cliente debe revisar presentación, medidas y cantidad antes de pagar."],
          ["Distribuidores", "Los precios comerciales requieren una cuenta aprobada y no pueden transferirse a perfiles no autorizados."]
        ]
      },
      privacy: {
        title: "Privacidad y seguridad",
        intro: "Cómo se utilizan los datos necesarios para comprar y recibir soporte.",
        sections: [
          ["Datos recopilados", "La tienda solicita identificación básica, contacto y datos comerciales necesarios para crear la cuenta y la orden."],
          ["Finalidad", "Los datos se utilizan para procesar compras, coordinar entregas, prevenir fraude y atender solicitudes de soporte."],
          ["Credenciales", "Las contraseñas y sesiones son gestionadas por la API segura. La tienda no contiene claves privadas."],
          ["Conservación", "Las órdenes y entregas permanecen en el historial para permitir trazabilidad comercial y atención postventa."],
          ["Responsabilidad del usuario", "No compartas tu contraseña ni los datos privados de seguimiento. Cierra sesión cuando utilices un equipo compartido."]
        ]
      },
      refunds: {
        title: "Garantía y devoluciones",
        intro: "Proceso de revisión para productos digitales y problemas de activación.",
        sections: [
          ["Antes de comprar", "Confirma que el producto, edición, plataforma y región sean compatibles con el equipo donde será utilizado."],
          ["Soporte postventa", "Si un producto presenta inconvenientes, Dismal Distribuciones verifica la orden, el estado del empaque y la evidencia suministrada."],
          ["Cambio o devolución", "Cuando la revisión confirma una novedad cubierta por la política, Dismal Distribuciones coordina el cambio o devolución aplicable."],
          ["Productos ya utilizados", "Las condiciones de devolución de un producto digital dependen de si la clave fue revelada, activada o vinculada a una cuenta."],
          ["Cómo solicitar revisión", "Ingresa a Mi cuenta y conserva el número de orden, correo de compra, capturas y detalles del equipo."]
        ]
      }
    } as const;
    const page = content[view as "terms" | "privacy" | "refunds"];
    return (
      <section className="page-section legal-page">
        <div className="legal-intro">
          <FileCheck2 size={38} aria-hidden="true" />
          <span>Información comercial</span>
          <h1>{page.title}</h1>
          <p>{page.intro}</p>
        </div>
        <div className="legal-content">
          {page.sections.map(([title, body], index) => (
            <section key={title}>
              <span>{String(index + 1).padStart(2, "0")}</span>
              <div><h2>{title}</h2><p>{body}</p></div>
            </section>
          ))}
        </div>
        <div className="legal-note"><CircleHelp size={22} aria-hidden="true" /><span>Para una orden específica, consulta su estado y condiciones desde Mi cuenta.</span><button type="button" onClick={() => currentUser ? openAccount() : openLogin()}>Mi cuenta</button></div>
      </section>
    );
  }

  function renderCurrentView() {
    if (productSlug) return renderProductDetail();
    if (view === "home") return renderHome();
    if (view === "catalog") return renderCatalog();
    if (view === "offers") return renderOffers();
    if (view === "wholesale") return renderWholesale();
    if (view === "about") return renderAbout();
    if (view === "help") return renderHelp();
    return renderLegalPage();
  }

  return (
    <div className="store-shell">
      <a className="skip-link" href="#main-content" onClick={(event) => { event.preventDefault(); mainContentRef.current?.focus(); mainContentRef.current?.scrollIntoView(); }}>Saltar al contenido</a>
      <div className="sr-only" aria-live="polite" aria-atomic="true">{announcement}</div>
      <header className="store-header">
        <div className="utility-strip">
          <div className="utility-inner">
            <span><ShieldCheck size={15} aria-hidden="true" /> Productos físicos con entrega verificada</span>
            <nav aria-label={`Canales de atención para ${country.name}`}>
              {country.whatsappNumber ? (
                <button type="button" onClick={() => contactByWhatsapp()} title="Escribir por WhatsApp">
                  <MessageCircle size={15} aria-hidden="true" /><span>{contactPhone}</span>
                </button>
              ) : null}
              {contactTelegramUrl ? (
                <a href={contactTelegramUrl} target="_blank" rel="noreferrer" title="Abrir Telegram">
                  <Send size={15} aria-hidden="true" /><span>Telegram</span>
                </a>
              ) : null}
              <a href={contactEmailUrl} title={`Escribir a ${country.contactEmail}`}>
                <Mail size={15} aria-hidden="true" /><span>{country.contactEmail}</span>
              </a>
              <span className="utility-market"><Globe2 size={15} aria-hidden="true" />{country.name}</span>
            </nav>
          </div>
        </div>
        <div className="header-main">
          <button
            className="mobile-menu-button"
            type="button"
            onClick={() => setMobileMenuOpen((current) => !current)}
            aria-expanded={mobileMenuOpen}
            aria-controls="primary-navigation"
            aria-label={mobileMenuOpen ? "Cerrar menú" : "Abrir menú"}
            title={mobileMenuOpen ? "Cerrar menú" : "Abrir menú"}
          >
            {mobileMenuOpen ? <X size={21} aria-hidden="true" /> : <Menu size={21} aria-hidden="true" />}
          </button>
          <StoreLink className="brand" view="home" onNavigate={navigateTo}>
            <img src="/assets/dismal-logo.png" alt="Dismal Distribuciones" />
          </StoreLink>
          <form className="search-bar" role="search" onSubmit={submitSearch}>
            <Search size={19} aria-hidden="true" />
            <input
              type="search"
              aria-label="Buscar productos"
              placeholder="Buscar por producto, marca o categoría"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
            <button type="submit" aria-label="Buscar">Buscar</button>
          </form>
          <div className="top-actions">
            <button
              className="account-button"
              type="button"
              onClick={() => currentUser ? openAccount() : openLogin()}
              aria-label={currentUser ? "Abrir mi cuenta" : "Iniciar sesión"}
            >
              <UserRound size={20} aria-hidden="true" />
              <span><small>{currentUser ? "Hola" : "Tu cuenta"}</small><strong>{currentUser ? currentUser.firstName || "Mis compras" : "Ingresar"}</strong></span>
            </button>
            <button className="cart-button" type="button" onClick={() => setCartOpen(true)} aria-label={`Carrito con ${cartCount} productos`}>
              <ShoppingCart size={21} aria-hidden="true" />
              <span>Carrito</span>
              <strong>{cartCount}</strong>
            </button>
          </div>
        </div>
        <nav id="primary-navigation" className={`primary-nav ${mobileMenuOpen ? "open" : ""}`} aria-label="Navegación principal">
          <div className="primary-nav-inner">
            {([
              ["home", "Inicio"],
              ["catalog", "Catálogo"],
              ["offers", "Ofertas"],
              ["wholesale", "Distribuidores"],
              ["help", "Ayuda"]
            ] as Array<[StoreView, string]>).map(([target, label]) => (
              <StoreLink className={view === target ? "active" : ""} current={view === target} view={target} onNavigate={navigateTo} key={target}>{label}</StoreLink>
            ))}
            <span className="nav-assurance"><ShieldCheck size={17} aria-hidden="true" /> Pago sujeto a confirmación</span>
          </div>
        </nav>
      </header>

      <main id="main-content" className="view-content" ref={mainContentRef} tabIndex={-1} key={view}>{renderCurrentView()}</main>

      <footer className="store-footer">
        <div className="footer-main">
          <div className="footer-brand">
            <img src="/assets/dismal-logo.png" alt="Dismal Distribuciones" />
            <p>Productos físicos para hogares, negocios y distribuidores de {country.name}.</p>
            <span><ShieldCheck size={17} aria-hidden="true" /> Precio y disponibilidad validados por el sistema central</span>
          </div>
          <div>
            <strong>Tienda</strong>
            <StoreLink view="catalog" onNavigate={navigateTo}>Catálogo</StoreLink>
            <StoreLink view="offers" onNavigate={navigateTo}>Ofertas</StoreLink>
            <StoreLink view="wholesale" onNavigate={navigateTo}>Distribuidores</StoreLink>
            <button type="button" onClick={() => setCartOpen(true)}>Carrito</button>
          </div>
          <div>
            <strong>Atención</strong>
            <StoreLink view="help" onNavigate={navigateTo}>Centro de ayuda</StoreLink>
            <button type="button" onClick={() => currentUser ? openAccount() : openLogin()}>Mis órdenes</button>
            <a href={contactEmailUrl}>{country.contactEmail}</a>
            {country.whatsappNumber ? <button type="button" onClick={() => contactByWhatsapp()}>WhatsApp {contactPhone}</button> : null}
            {contactTelegramUrl ? <a href={contactTelegramUrl} target="_blank" rel="noreferrer">Telegram</a> : null}
            <StoreLink view="refunds" onNavigate={navigateTo}>Garantía</StoreLink>
            <StoreLink view="about" onNavigate={navigateTo}>Quiénes somos</StoreLink>
          </div>
          <div>
            <strong>Legal</strong>
            <StoreLink view="terms" onNavigate={navigateTo}>Términos de compra</StoreLink>
            <StoreLink view="privacy" onNavigate={navigateTo}>Privacidad</StoreLink>
            <StoreLink view="refunds" onNavigate={navigateTo}>Devoluciones</StoreLink>
            <span>Operación: {country.name}</span>
          </div>
        </div>
        <div className="footer-bottom">
          <span>© {new Date().getFullYear()} Dismal Distribuciones. Productos físicos y abastecimiento.</span>
          <span>{country.paymentMethods.join(" · ")}</span>
        </div>
      </footer>

      <div className="contact-dock" aria-label="Canales de atención rápida">
        {country.whatsappNumber ? <button className="contact-fab contact-whatsapp" type="button" onClick={() => contactByWhatsapp()} aria-label="Consultar por WhatsApp" title="WhatsApp"><MessageCircle aria-hidden="true" /></button> : null}
        {contactTelegramUrl ? <a className="contact-fab contact-telegram" href={contactTelegramUrl} target="_blank" rel="noreferrer" aria-label="Contactar por Telegram" title="Telegram"><Send aria-hidden="true" /></a> : null}
        <a className="contact-fab contact-email" href={contactEmailUrl} aria-label={`Escribir a ${country.contactEmail}`} title={country.contactEmail}><Mail aria-hidden="true" /></a>
      </div>

      {selectedProduct ? (
        <aside className="modal-shell" aria-label="Detalle del producto">
          <button className="modal-backdrop" type="button" tabIndex={-1} onClick={() => setSelectedProduct(null)} aria-label="Cerrar detalle" />
          <div className="product-dialog" ref={productDialogRef} role="dialog" aria-modal="true" aria-labelledby="product-dialog-title" tabIndex={-1}>
            <button className="icon-button dialog-close" type="button" onClick={() => setSelectedProduct(null)} aria-label="Cerrar" title="Cerrar"><X size={20} aria-hidden="true" /></button>
            <div className="dialog-media">
              <ProductArtwork product={selectedProduct} large />
            </div>
            <div className="dialog-content">
              <span className="product-kicker">{selectedProduct.category} · {selectedProduct.platform}</span>
              <h2 id="product-dialog-title">{selectedProduct.name}</h2>
              <p>{selectedProduct.description}</p>
              <div className="dialog-benefits">
                {selectedProduct.benefits.map((benefit) => <span key={benefit}><Check size={16} aria-hidden="true" />{benefit}</span>)}
              </div>
              <dl>
                <div><dt>Entrega</dt><dd>{selectedProduct.delivery}</dd></div>
                <div><dt>Soporte</dt><dd>{selectedProduct.support}</dd></div>
                <div><dt>Región</dt><dd>{country.name}</dd></div>
              </dl>
              <div className="dialog-purchase">
                <div><small>Precio {customerType === "wholesale" ? "distribuidor" : "final"}</small><strong>{formatMoney(productPrice(selectedProduct, customerType), country)}</strong></div>
                <button className="primary-button" type="button" onClick={() => addToCart(selectedProduct.id)}><ShoppingCart size={18} aria-hidden="true" /> Agregar al carrito</button>
              </div>
              <span className="secure-note"><LockKeyhole size={15} aria-hidden="true" /> Precio y disponibilidad confirmados al crear la orden.</span>
            </div>
          </div>
        </aside>
      ) : null}

      {cartOpen ? (
        <aside className="drawer" aria-label="Carrito">
          <button className="drawer-backdrop" type="button" tabIndex={-1} onClick={() => setCartOpen(false)} aria-label="Cerrar carrito" />
          <div className="drawer-panel" ref={cartDialogRef} role="dialog" aria-modal="true" aria-labelledby="cart-dialog-title" tabIndex={-1}>
            <header>
              <div><span>Tu compra</span><h2 id="cart-dialog-title">Carrito</h2></div>
              <button className="icon-button" type="button" onClick={() => setCartOpen(false)} aria-label="Cerrar" title="Cerrar"><X size={19} aria-hidden="true" /></button>
            </header>
            <div className="cart-content">
              {cart.length === 0 ? (
                <div className="empty-state"><ShoppingCart size={34} aria-hidden="true" /><strong>Tu carrito está vacío</strong><span>Explora el catálogo y agrega los productos que necesitas.</span><button type="button" onClick={() => { setCartOpen(false); showCatalog(); }}>Ver catálogo</button></div>
              ) : cartLines.length === 0 ? (
                <div className="empty-state"><RefreshCw size={34} aria-hidden="true" /><strong>Estamos validando tu carrito</strong><span>Necesitamos recuperar el catálogo para confirmar productos y precios.</span><button type="button" onClick={() => setCatalogReloadKey((current) => current + 1)}>Reintentar validación</button></div>
              ) : <>
                {unavailableCartCount > 0 ? <p className="cart-validation" role="status"><AlertTriangle size={17} aria-hidden="true" /> {unavailableCartCount} producto(s) requieren validación antes de continuar.</p> : null}
                {cartLines.map((line) => (
                <div className="cart-line" key={line.productId}>
                  <div className="cart-line-art"><ProductArtwork product={line.product} /></div>
                  <div><strong>{line.product.name}</strong><span>{formatMoney(line.unitPrice, country)} por unidad</span><button type="button" onClick={() => updateQuantity(line.productId, 0)}>Eliminar</button></div>
                  <div className="quantity-control">
                    <button type="button" onClick={() => updateQuantity(line.productId, line.quantity - 1)} aria-label="Disminuir"><Minus size={15} aria-hidden="true" /></button>
                    <span>{line.quantity}</span>
                    <button type="button" onClick={() => updateQuantity(line.productId, line.quantity + 1)} aria-label="Aumentar"><Plus size={15} aria-hidden="true" /></button>
                  </div>
                </div>
                ))}
              </>}
            </div>
            <footer className="drawer-footer">
              <div><span>Subtotal</span><strong>{formatMoney(cartTotal, country)}</strong></div>
              <small>El total definitivo será validado antes de crear la orden.</small>
              <button className="primary-button full-width" type="button" disabled={cartLines.length === 0 || unavailableCartCount > 0 || catalogStatus !== "ready"} onClick={() => { setCartOpen(false); setCheckoutOpen(true); setOrderStatus("idle"); setPaymentSubmissionStatus("idle"); setOrderMessage(""); setCreatedOrder(null); setTermsAccepted(false); }}>Continuar con la orden <ChevronRight size={18} aria-hidden="true" /></button>
              <span className="secure-note"><ShieldCheck size={15} aria-hidden="true" /> Precio y disponibilidad validados al crear la orden</span>
            </footer>
          </div>
        </aside>
      ) : null}

      {checkoutOpen ? (
        <aside className="drawer" aria-label="Checkout">
          <button className="drawer-backdrop" type="button" tabIndex={-1} onClick={() => setCheckoutOpen(false)} aria-label="Cerrar checkout" />
          <div className="drawer-panel checkout-panel" ref={checkoutDialogRef} role="dialog" aria-modal="true" aria-labelledby="checkout-dialog-title" tabIndex={-1}>
            <header>
              <div><span>Finalizar compra</span><h2 id="checkout-dialog-title">{createdOrder ? "Pago y entrega" : "Datos de entrega"}</h2></div>
              <button className="icon-button" type="button" onClick={() => setCheckoutOpen(false)} aria-label="Cerrar" title="Cerrar"><X size={19} aria-hidden="true" /></button>
            </header>
            <div className="checkout-progress"><span className={!createdOrder ? "active" : ""}>1. Datos</span><span>2. Orden</span><span className={createdOrder ? "active" : ""}>3. Pago y entrega</span></div>
            <form className="checkout-form" onSubmit={createdOrder ? submitPaymentClaim : submitOrder}>
              {!createdOrder ? (
                <>
                  <div className="form-grid">
                    <label>Nombre<input type="text" autoComplete="given-name" maxLength={80} required value={checkoutCustomer.firstName} onChange={(event) => updateCheckoutField("firstName", event.target.value)} /></label>
                    <label>Apellido<input type="text" autoComplete="family-name" maxLength={80} required value={checkoutCustomer.lastName} onChange={(event) => updateCheckoutField("lastName", event.target.value)} /></label>
                    <label className="wide-field">Email de confirmación<input type="email" autoComplete="email" maxLength={160} required value={checkoutCustomer.email} readOnly={Boolean(currentUser)} onChange={(event) => updateCheckoutField("email", event.target.value)} /></label>
                    <label>WhatsApp<input type="tel" inputMode="tel" autoComplete="tel" maxLength={24} required value={checkoutCustomer.phone} onChange={(event) => updateCheckoutField("phone", event.target.value)} /></label>
                    <label>Documento o RUC<input type="text" inputMode="numeric" autoComplete="off" maxLength={24} value={checkoutCustomer.taxId} onChange={(event) => updateCheckoutField("taxId", event.target.value)} /></label>
                    {!currentUser ? <label className="wide-field">Crea una contraseña<input type="password" autoComplete="new-password" minLength={8} maxLength={128} required value={checkoutCustomer.password || ""} onChange={(event) => updateCheckoutField("password", event.target.value)} /><small>Mínimo 8 caracteres para proteger tus pedidos.</small></label> : null}
                    <label className="wide-field">Dirección de entrega<input type="text" autoComplete="street-address" maxLength={220} required value={shippingAddress.addressLine1} onChange={(event) => setShippingAddress((current) => ({ ...current, addressLine1: event.target.value }))} /></label>
                    <label className="wide-field">Complemento o referencia<input type="text" maxLength={220} value={shippingAddress.addressLine2} onChange={(event) => setShippingAddress((current) => ({ ...current, addressLine2: event.target.value }))} /></label>
                    <label>Ciudad<input type="text" autoComplete="address-level2" maxLength={120} required value={shippingAddress.city} onChange={(event) => setShippingAddress((current) => ({ ...current, city: event.target.value }))} /></label>
                    <label>Provincia / Región<input type="text" autoComplete="address-level1" maxLength={120} required value={shippingAddress.region} onChange={(event) => setShippingAddress((current) => ({ ...current, region: event.target.value }))} /></label>
                    <label>Código postal<input type="text" autoComplete="postal-code" maxLength={32} value={shippingAddress.postalCode} onChange={(event) => setShippingAddress((current) => ({ ...current, postalCode: event.target.value }))} /></label>
                    <label>Indicaciones de entrega<input type="text" maxLength={500} value={shippingAddress.notes} onChange={(event) => setShippingAddress((current) => ({ ...current, notes: event.target.value }))} /></label>
                  </div>

                  <section className="checkout-methods" aria-labelledby="checkout-method-title">
                    <div className="checkout-section-heading"><CreditCard size={20} aria-hidden="true" /><div><strong id="checkout-method-title">Forma de pago</strong><span>El servidor validará el método y el total.</span></div></div>
                    <div className="checkout-method-grid">
                      <label className={`checkout-method-option ${checkoutMethod === "BANK_TRANSFER" ? "selected" : ""}`}>
                        <input type="radio" name="checkoutMethod" value="BANK_TRANSFER" checked={checkoutMethod === "BANK_TRANSFER"} onChange={() => setCheckoutMethod("BANK_TRANSFER")} />
                        <Landmark size={21} aria-hidden="true" />
                        <span><strong>Transferencia bancaria</strong><small>Validación manual antes de la entrega</small></span>
                      </label>
                      {currentUser?.hasCredit ? (
                        <label className={`checkout-method-option ${checkoutMethod === "CUSTOMER_CREDIT" ? "selected" : ""}`}>
                          <input type="radio" name="checkoutMethod" value="CUSTOMER_CREDIT" checked={checkoutMethod === "CUSTOMER_CREDIT"} onChange={() => setCheckoutMethod("CUSTOMER_CREDIT")} />
                          <BadgeCheck size={21} aria-hidden="true" />
                          <span><strong>Crédito Dismal</strong><small>{formatMoney(currentUser.creditAvailable, country)} disponibles · {currentUser.creditDays || 0} días</small></span>
                        </label>
                      ) : null}
                    </div>
                  </section>

                  <div className="order-summary-box">
                    <div><span>{cartCount} {cartCount === 1 ? "producto" : "productos"}</span><strong>{formatMoney(cartTotal, country)}</strong></div>
                    <p><LockKeyhole size={16} aria-hidden="true" /> {checkoutMethod === "CUSTOMER_CREDIT" ? "El cupo se valida al crear la compra." : "El pedido se prepara después de verificar la transferencia."}</p>
                  </div>
                  <label className="legal-consent">
                    <input type="checkbox" required checked={termsAccepted} onChange={(event) => setTermsAccepted(event.target.checked)} />
                    <span>Acepto los <a href={storefrontHref("terms")} target="_blank" rel="noreferrer">términos de compra</a> y la <a href={storefrontHref("privacy")} target="_blank" rel="noreferrer">política de privacidad</a>.</span>
                  </label>
                  {orderMessage ? <p className={`checkout-message ${orderStatus}`} role={orderStatus === "error" ? "alert" : "status"}>{orderMessage}</p> : null}
                  <button className="primary-button full-width" type="submit" disabled={orderStatus === "submitting" || cartLines.length === 0 || !termsAccepted}>{orderStatus === "submitting" ? "Creando orden..." : checkoutMethod === "CUSTOMER_CREDIT" ? "Comprar con mi crédito" : "Crear orden y pagar"}</button>
                </>
              ) : (
                <>
                <div className="order-created-summary">
                  <CheckCircle2 size={26} aria-hidden="true" />
                  <div><span>Número de orden</span><strong>{createdOrder.orderNumber}</strong></div>
                  {!createdOrder.demo ? <div><span>Total confirmado</span><strong>{new Intl.NumberFormat(country.locale, { style: "currency", currency: createdOrder.currency }).format(createdOrder.total)}</strong></div> : null}
                </div>
                {orderMessage ? <p className={`checkout-message ${paymentSubmissionStatus === "error" ? "error" : "created"}`} role={paymentSubmissionStatus === "error" ? "alert" : "status"}>{orderMessage}</p> : null}

                {createdOrder.demo ? (
                  <button className="secondary-button full-width" type="button" onClick={() => setCheckoutOpen(false)}>Cerrar simulación</button>
                ) : createdOrder.status === "PREPARING" ? (
                  <div className="checkout-complete-state"><PackageCheck size={30} aria-hidden="true" /><strong>Pedido en preparación</strong><span>Tu compra fue aprobada y el equipo de bodega está preparando el despacho.</span><button className="primary-button full-width" type="button" onClick={() => { setCheckoutOpen(false); openAccount(); }}>Ver mi pedido</button></div>
                ) : createdOrder.status === "PAYMENT_REVIEW" ? (
                  <div className="checkout-review-state"><Clock3 size={28} aria-hidden="true" /><strong>Pago en revisión</strong><span>Un responsable comprobará referencia y monto en la cuenta bancaria. La orden cambiará de estado cuando termine la validación.</span><button className="secondary-button full-width" type="button" onClick={() => { setCheckoutOpen(false); openAccount(); }}>Ver estado de la orden</button></div>
                ) : (
                  <section className="bank-payment-step" aria-labelledby="bank-payment-title">
                    <div className="checkout-section-heading"><Landmark size={21} aria-hidden="true" /><div><strong id="bank-payment-title">Realiza la transferencia</strong><span>Selecciona la cuenta correspondiente a {country.name}.</span></div></div>
                    {paymentAccountsStatus === "loading" ? <p className="simple-state" aria-busy="true">Cargando cuentas bancarias...</p> : null}
                    {paymentAccountsStatus === "error" ? <p className="checkout-message error">No pudimos consultar las cuentas bancarias. Intenta nuevamente.</p> : null}
                    {paymentAccountsStatus === "ready" && paymentAccounts.length === 0 ? <p className="checkout-message error">Todavía no existe una cuenta bancaria pública configurada para este país. Conserva tu número de orden y contacta a soporte.</p> : null}
                    <div className="bank-account-list">
                      {paymentAccounts.map((account) => (
                        <label className={`bank-account-option ${paymentClaim.paymentAccountId === account.id ? "selected" : ""}`} key={account.id}>
                          <input type="radio" name="paymentAccountId" value={account.id} checked={paymentClaim.paymentAccountId === account.id} onChange={() => setPaymentClaim((current) => ({ ...current, paymentAccountId: account.id }))} />
                          <div><strong>{account.bankName || account.name}</strong><span>{account.accountType || "Cuenta bancaria"} · {account.currency}</span><code>{account.accountNumber}</code><small>{account.accountHolder}{account.taxId ? ` · ${account.taxId}` : ""}</small></div>
                          <button className="icon-button" type="button" onClick={() => copyBankValue(account.accountNumber, "Número de cuenta")} aria-label={`Copiar cuenta de ${account.bankName || account.name}`} title="Copiar número"><Copy size={16} aria-hidden="true" /></button>
                        </label>
                      ))}
                    </div>
                    {paymentAccounts.length > 0 ? (
                      <div className="form-grid payment-claim-fields">
                        <label className="wide-field">Nombre del pagador<input required maxLength={160} value={paymentClaim.payerName} onChange={(event) => setPaymentClaim((current) => ({ ...current, payerName: event.target.value }))} /></label>
                        <label>Banco desde el que pagaste<input required maxLength={120} value={paymentClaim.sourceBank} onChange={(event) => setPaymentClaim((current) => ({ ...current, sourceBank: event.target.value }))} /></label>
                        <label>Número de operación<input required maxLength={160} value={paymentClaim.reference} onChange={(event) => setPaymentClaim((current) => ({ ...current, reference: event.target.value }))} /></label>
                        <label className="wide-field">Monto transferido<input required type="number" min="0.01" step="0.01" inputMode="decimal" value={paymentClaim.amount} onChange={(event) => setPaymentClaim((current) => ({ ...current, amount: event.target.value }))} /></label>
                      </div>
                    ) : null}
                    <p className="payment-verification-note"><ShieldCheck size={17} aria-hidden="true" /> Registrar una referencia no confirma el pago. Dismal Distribuciones la cotejará directamente en el banco.</p>
                    <button className="primary-button full-width" type="submit" disabled={paymentSubmissionStatus === "submitting" || paymentAccounts.length === 0 || !paymentClaim.paymentAccountId}>{paymentSubmissionStatus === "submitting" ? "Registrando pago..." : "Enviar pago a verificación"}</button>
                  </section>
                )}
                </>
              )}
            </form>
          </div>
        </aside>
      ) : null}

      {authOpen ? (
        <aside className="drawer" aria-label="Inicio de sesión">
          <button className="drawer-backdrop" type="button" tabIndex={-1} onClick={() => setAuthOpen(false)} aria-label="Cerrar inicio de sesión" />
          <div className="drawer-panel auth-panel" ref={authDialogRef} role="dialog" aria-modal="true" aria-labelledby="auth-dialog-title" tabIndex={-1}>
            <header>
              <div><span>Cuenta Dismal</span><h2 id="auth-dialog-title">{authMode === "forgot" ? "Recupera tu acceso" : authMode === "reset" ? "Crea una nueva contraseña" : authMode === "verify" ? "Verificación de correo" : "Ingresa a tus compras"}</h2></div>
              <button className="icon-button" type="button" onClick={() => setAuthOpen(false)} aria-label="Cerrar" title="Cerrar"><X size={19} aria-hidden="true" /></button>
            </header>
            <div className="auth-intro"><LockKeyhole size={27} aria-hidden="true" /><strong>Acceso privado</strong><span>Consulta pedidos, preparación, despacho y entrega.</span></div>
            {authMode === "login" ? (
              <form className="auth-form" onSubmit={(event) => { event.preventDefault(); submitLogin(); }}>
                <label>Correo<input type="email" autoComplete="email" maxLength={160} required value={loginEmail} onChange={(event) => setLoginEmail(event.target.value)} /></label>
                <label>Contraseña<input type="password" autoComplete="current-password" maxLength={128} required value={loginPassword} onChange={(event) => setLoginPassword(event.target.value)} /></label>
                {authMessage ? <p className={`checkout-message ${authStatus === "error" ? "error" : "created"}`} role={authStatus === "error" ? "alert" : "status"}>{authMessage}</p> : null}
                <button className="primary-button full-width" type="submit" disabled={authStatus === "submitting"}>{authStatus === "submitting" ? "Validando..." : "Ingresar de forma segura"}</button>
                <button className="text-action" type="button" onClick={() => { setAuthMode("forgot"); setAuthMessage(""); }}>Olvidé mi contraseña</button>
                <p className="auth-note">Los clientes nuevos crean su cuenta durante la primera compra.</p>
              </form>
            ) : null}
            {authMode === "forgot" ? (
              <form className="auth-form" onSubmit={(event) => { event.preventDefault(); submitForgotPassword(); }}>
                <label>Correo de tu cuenta<input type="email" autoComplete="email" maxLength={160} required value={loginEmail} onChange={(event) => setLoginEmail(event.target.value)} /></label>
                {authMessage ? <p className={`checkout-message ${authStatus === "error" ? "error" : "created"}`} role={authStatus === "error" ? "alert" : "status"}>{authMessage}</p> : null}
                <button className="primary-button full-width" type="submit" disabled={authStatus === "submitting"}>{authStatus === "submitting" ? "Enviando..." : "Enviar enlace seguro"}</button>
                <button className="text-action" type="button" onClick={() => openLogin()}>Volver al inicio de sesión</button>
              </form>
            ) : null}
            {authMode === "reset" ? (
              <form className="auth-form" onSubmit={(event) => { event.preventDefault(); submitPasswordReset(); }}>
                <label>Nueva contraseña<input type="password" autoComplete="new-password" minLength={8} maxLength={128} required value={newPassword} onChange={(event) => setNewPassword(event.target.value)} /></label>
                <label>Confirmar contraseña<input type="password" autoComplete="new-password" minLength={8} maxLength={128} required value={newPasswordConfirmation} onChange={(event) => setNewPasswordConfirmation(event.target.value)} /></label>
                {authMessage ? <p className={`checkout-message ${authStatus === "error" ? "error" : "created"}`} role={authStatus === "error" ? "alert" : "status"}>{authMessage}</p> : null}
                <button className="primary-button full-width" type="submit" disabled={authStatus === "submitting"}>{authStatus === "submitting" ? "Actualizando..." : "Guardar contraseña"}</button>
              </form>
            ) : null}
            {authMode === "verify" ? (
              <div className="auth-form">
                {authStatus === "submitting" ? <p className="simple-state" aria-busy="true">Validando el enlace...</p> : null}
                {authMessage ? <p className={`checkout-message ${authStatus === "error" ? "error" : "created"}`} role={authStatus === "error" ? "alert" : "status"}>{authMessage}</p> : null}
                <button className="primary-button full-width" type="button" onClick={() => { window.history.replaceState({}, "", "/"); currentUser ? openAccount() : openLogin(); }}>Continuar</button>
              </div>
            ) : null}
          </div>
        </aside>
      ) : null}

      {accountOpen && currentUser ? (
        <aside className="drawer" aria-label="Mi cuenta">
          <button className="drawer-backdrop" type="button" tabIndex={-1} onClick={() => setAccountOpen(false)} aria-label="Cerrar mi cuenta" />
          <div className="drawer-panel account-panel" ref={accountDialogRef} role="dialog" aria-modal="true" aria-labelledby="account-dialog-title" tabIndex={-1}>
            <header>
              <div><span>Mi cuenta</span><h2 id="account-dialog-title">{currentUser.firstName || currentUser.email}</h2></div>
              <div className="header-icon-actions"><button className="icon-button" type="button" onClick={signOut} aria-label="Cerrar sesión" title="Cerrar sesión"><LogOut size={18} aria-hidden="true" /></button><button className="icon-button" type="button" onClick={() => setAccountOpen(false)} aria-label="Cerrar" title="Cerrar"><X size={19} aria-hidden="true" /></button></div>
            </header>
            <div className="account-content">
              <div className="account-summary"><UserRound size={24} aria-hidden="true" /><span>{currentUser.email}<strong>{currentUser.customerType === "DISTRIBUTOR" ? "Distribuidor aprobado" : "Cliente final"}</strong></span></div>
              <section className="account-credit-overview" aria-label="Resumen de crédito">
                <div><span>Cupo disponible</span><strong>{currentUser.hasCredit ? formatMoney(currentUser.creditAvailable, country) : "Sin crédito"}</strong></div>
                <div><span>Cupo aprobado</span><strong>{currentUser.hasCredit ? formatMoney(currentUser.creditLimit, country) : "Pago directo"}</strong></div>
                <div><span>Cupo utilizado</span><strong>{currentUser.hasCredit ? formatMoney(currentUser.creditUsed, country) : formatMoney(0, country)}</strong></div>
                <div><span>Plazo</span><strong>{currentUser.hasCredit ? `${currentUser.creditDays} días` : "Transferencia"}</strong></div>
              </section>
              {!currentUser.emailVerified ? (
                <div className="account-verification" role="status">
                  <Mail size={22} aria-hidden="true" />
                  <div><strong>Confirma tu correo</strong><span>Podrás ver tus órdenes, pero las claves permanecerán protegidas hasta verificarlo.</span></div>
                  <button type="button" onClick={sendVerificationEmail} disabled={accountActionStatus === "submitting"}>Reenviar</button>
                </div>
              ) : null}
              {accountActionMessage ? <p className={`checkout-message ${accountActionStatus === "error" ? "error" : "created"}`} role={accountActionStatus === "error" ? "alert" : "status"}>{accountActionMessage}</p> : null}
              {accountStatus === "loading" ? <p className="simple-state" aria-busy="true">Cargando tus compras...</p> : null}
              {accountStatus === "error" ? <div className="account-retry"><p className="checkout-message error" role="alert">{accountMessage || "No se pudo cargar el historial."}</p><button type="button" onClick={() => openAccount(authToken)}>Reintentar</button></div> : null}
              <div className="account-section-heading"><div><span>Historial</span><strong>Compras y entregas</strong></div><span>{accountOrders.length} visibles</span></div>
              {accountStatus === "idle" && accountOrders.length === 0 ? <div className="empty-state"><Boxes size={32} aria-hidden="true" /><strong>Todavía no tienes compras</strong><span>Cuando crees tu primera orden aparecerá aquí.</span></div> : null}
              {accountOrders.map(({ order }) => (
                <article className="account-order" key={order.orderId}>
                  <div className="account-order-heading"><div><span>{formatOrderDate(order.createdAt, getCountry(order.country).locale)}</span><strong>{order.orderNumber}</strong></div><span className={`status-pill status-${order.status.toLowerCase()}`}>{orderStatusLabel(order.status)}</span></div>
                  <div className="account-order-items">{order.items.map((item) => <span key={`${order.orderId}-${item.productId}`}>{item.quantity} × {item.productName}</span>)}</div>
                  <div className="account-order-total"><span>Total</span><strong>{new Intl.NumberFormat(country.locale, { style: "currency", currency: order.currency }).format(order.total)}</strong></div>
                  {order.status === "PENDING_PAYMENT" ? <div className="account-order-actions"><button className="danger-text-button" type="button" disabled={cancellingOrderNumber === order.orderNumber} onClick={() => cancelOrder(order)}><Trash2 size={16} aria-hidden="true" />{cancellingOrderNumber === order.orderNumber ? "Cancelando..." : "Cancelar pedido"}</button></div> : null}
                  {["CANCELLED", "FAILED", "REFUNDED"].includes(order.status) ? <div className="account-order-actions"><button className="repeat-order-button" type="button" onClick={() => repeatOrder(order)}><RotateCcw size={16} aria-hidden="true" />Repetir compra</button></div> : null}
                  {order.trackingNumber ? <div className="tracking-row"><PackageCheck size={20} aria-hidden="true" /><div><strong>{order.shippingCarrier || "Transportista"}</strong><code>{order.trackingNumber}</code></div></div> : null}
                </article>
              ))}
              {accountStatus !== "loading" && accountPage + 1 < accountTotalPages ? <button className="load-more-button" type="button" disabled={accountStatus === "loading-more"} onClick={() => openAccount(authToken, accountPage + 1, true)}>{accountStatus === "loading-more" ? "Cargando..." : "Cargar compras anteriores"}</button> : null}
              <div className="account-tools">
                <details>
                  <summary><UserRound size={18} aria-hidden="true" /> Datos personales <ChevronDown size={17} aria-hidden="true" /></summary>
                  <form className="account-tool-form" onSubmit={saveProfile}>
                    <div className="form-grid two-columns">
                      <label>Nombre<input required maxLength={80} value={profileForm.firstName} onChange={(event) => setProfileForm((current) => ({ ...current, firstName: event.target.value }))} /></label>
                      <label>Apellido<input required maxLength={80} value={profileForm.lastName} onChange={(event) => setProfileForm((current) => ({ ...current, lastName: event.target.value }))} /></label>
                    </div>
                    <label>WhatsApp<input type="tel" maxLength={32} value={profileForm.phone} onChange={(event) => setProfileForm((current) => ({ ...current, phone: event.target.value }))} /></label>
                    <label>RUC o identificación<input maxLength={40} value={profileForm.taxId} onChange={(event) => setProfileForm((current) => ({ ...current, taxId: event.target.value }))} /></label>
                    <label>Correo de facturación<input type="email" maxLength={180} value={profileForm.billingEmail} onChange={(event) => setProfileForm((current) => ({ ...current, billingEmail: event.target.value }))} /></label>
                    <button className="secondary-button" type="submit" disabled={accountActionStatus === "submitting"}>Guardar datos</button>
                  </form>
                </details>
                <details>
                  <summary><KeyRound size={18} aria-hidden="true" /> Seguridad <ChevronDown size={17} aria-hidden="true" /></summary>
                  <form className="account-tool-form" onSubmit={savePassword}>
                    <label>Contraseña actual<input type="password" autoComplete="current-password" required maxLength={128} value={passwordForm.currentPassword} onChange={(event) => setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))} /></label>
                    <label>Nueva contraseña<input type="password" autoComplete="new-password" required minLength={8} maxLength={128} value={passwordForm.nextPassword} onChange={(event) => setPasswordForm((current) => ({ ...current, nextPassword: event.target.value }))} /></label>
                    <label>Confirmar contraseña<input type="password" autoComplete="new-password" required minLength={8} maxLength={128} value={passwordForm.confirmation} onChange={(event) => setPasswordForm((current) => ({ ...current, confirmation: event.target.value }))} /></label>
                    <button className="secondary-button" type="submit" disabled={accountActionStatus === "submitting"}>Cambiar contraseña</button>
                  </form>
                </details>
                {currentUser.customerType !== "DISTRIBUTOR" ? (
                  <details open={wholesaleApplication?.status === "PENDING"}>
                    <summary><Building2 size={18} aria-hidden="true" /> Programa para distribuidores <ChevronDown size={17} aria-hidden="true" /></summary>
                    {wholesaleApplication ? (
                      <div className={`wholesale-status wholesale-${wholesaleApplication.status.toLowerCase()}`}>
                        <strong>{wholesaleApplication.status === "PENDING" ? "Solicitud en revisión" : wholesaleApplication.status === "APPROVED" ? "Solicitud aprobada" : "Solicitud no aprobada"}</strong>
                        <span>{wholesaleApplication.businessName}</span>
                        {wholesaleApplication.reviewNotes ? <p>{wholesaleApplication.reviewNotes}</p> : null}
                      </div>
                    ) : null}
                    {wholesaleApplication?.status !== "PENDING" ? (
                      <form className="account-tool-form" onSubmit={saveWholesaleApplication}>
                        <label>Nombre comercial<input required maxLength={180} value={wholesaleForm.businessName} onChange={(event) => setWholesaleForm((current) => ({ ...current, businessName: event.target.value }))} /></label>
                        <div className="form-grid two-columns">
                          <label>RUC o identificación<input required maxLength={40} value={wholesaleForm.taxId} onChange={(event) => setWholesaleForm((current) => ({ ...current, taxId: event.target.value }))} /></label>
                          <label>WhatsApp<input type="tel" required maxLength={32} value={wholesaleForm.phone} onChange={(event) => setWholesaleForm((current) => ({ ...current, phone: event.target.value }))} /></label>
                        </div>
                        <label>Sitio web o red comercial<input type="url" maxLength={240} value={wholesaleForm.website} onChange={(event) => setWholesaleForm((current) => ({ ...current, website: event.target.value }))} /></label>
                        <label>Información comercial<textarea maxLength={1000} rows={4} value={wholesaleForm.notes} onChange={(event) => setWholesaleForm((current) => ({ ...current, notes: event.target.value }))} /></label>
                        <button className="primary-button" type="submit" disabled={accountActionStatus === "submitting" || !currentUser.emailVerified}>Enviar solicitud</button>
                        {!currentUser.emailVerified ? <p className="auth-note">Primero confirma tu correo.</p> : null}
                      </form>
                    ) : null}
                  </details>
                ) : null}
              </div>
            </div>
          </div>
        </aside>
      ) : null}
    </div>
  );
}

export default App;
