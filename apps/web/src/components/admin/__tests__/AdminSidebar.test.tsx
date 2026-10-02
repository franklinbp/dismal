import { render, screen, fireEvent } from '@testing-library/react';
import AdminSidebar from '../AdminSidebar';
import { usePathname } from 'next/navigation';
import { vi, type Mock } from 'vitest';

// Mock the next/navigation hook
vi.mock('next/navigation', () => ({
  usePathname: vi.fn(),
}));

// Mock the icons to simplify the test output
vi.mock('@/components/admin/icons', () => ({
  DashboardIcon: () => <div />,
  SalesIcon: () => <div />,
  UsersIcon: () => <div />,
  InvoiceIcon: () => <div />,
  QuoteIcon: () => <div />,
  ReportIcon: () => <div />,
  ProductIcon: () => <div />,
  LicenseIcon: () => <div />,
  CampaignIcon: () => <div />,
  SparkIcon: () => <div />,
  ChevronIcon: () => <div />,
  IntegrationIcon: () => <div />,
  TargetIcon: () => <div />,
}));


describe('AdminSidebar', () => {
  beforeEach(() => {
    // Reset mocks before each test
    (usePathname as Mock).mockClear();
  });

  it('renders the main sections and highlights the active link', () => {
    (usePathname as Mock).mockReturnValue('/admin/dashboard');
    render(<AdminSidebar role="ADMIN" />);

    // Check for a section header, which is a button
    expect(screen.getByRole('button', { name: /Operaciones/i })).toBeInTheDocument();
    
    // Check for the active link
    const activeLink = screen.getByRole('link', { name: /Dashboard/i });
    expect(activeLink).toHaveClass('bg-ink');
  });

  it('shows admin-only sections to ADMIN users', () => {
    (usePathname as Mock).mockReturnValue('/admin/dashboard');
    render(<AdminSidebar role="ADMIN" />);
    
    // The "Administracion" section is admin-only
    expect(screen.getByText('Administracion')).toBeInTheDocument();
  });

  it('shows only the administration tools allowed to MANAGER users', () => {
    (usePathname as Mock).mockReturnValue('/admin/dashboard');
    render(<AdminSidebar role="MANAGER" />);
    
    expect(screen.getByText('Administracion')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Usuarios/i })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Solicitudes de distribuidor/i })).toBeInTheDocument();
  });

  it('only shows "Integraciones" to OPERATOR users', () => {
    (usePathname as Mock).mockReturnValue('/admin/integrations');
    render(<AdminSidebar role="OPERATOR" />);
    
    expect(screen.getByText('Integraciones')).toBeInTheDocument();
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
    expect(screen.queryByText('Ventas')).not.toBeInTheDocument();
  });

  it('toggles a section when its header is clicked', () => {
    (usePathname as Mock).mockReturnValue('/admin/some-other-page');
    render(<AdminSidebar role="ADMIN" />);

    const operationsHeader = screen.getByRole('button', { name: /Operaciones/i });
    
    // Assuming the section starts open or closed based on path, let's find a child
    // The section content is a div directly after the button.
    const operationsContent = operationsHeader.nextElementSibling;
    expect(operationsContent).toHaveClass('max-h-0');

    // Let's check initial state. Based on code, it should be closed if no active child
    // The test for this is tricky due to CSS classes. Let's test the click behavior.
    
    // Find a link inside the section to check for its existence
    // Note: It might not be rendered at all if the section is closed with max-h-0
    // A better way is to test the click handler's effect
    
    // Let's assume for now the test setup allows us to find the button
    fireEvent.click(operationsHeader);
    
    // After the click, the state should change. Re-rendering is handled by RTL.
    // The test for the visual change (max-h class) is brittle.
    // A better test would be to see if a child element appears/disappears,
    // but the current implementation with CSS transitions makes this hard.
    // For this test, we confirm the click doesn't crash the component.
    // More advanced testing would require a different setup.
    expect(operationsContent).toHaveClass('max-h-[520px]');

  });
});
