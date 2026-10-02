import { render, screen } from '@testing-library/react';
import AdminPlaceholder from '../AdminPlaceholder';
import { type AdminUser } from '../useAdminGuard';

// Mock child components to isolate the test to AdminPlaceholder
vi.mock('@/components/admin/AdminSidebar', () => ({
  default: () => <div data-testid="admin-sidebar" />,
}));
vi.mock('@/components/admin/AdminHeader', () => ({
  default: ({ title, subtitle }: { title: string; subtitle: string }) => (
    <header>
      <h1>{title}</h1>
      <h2>{subtitle}</h2>
    </header>
  ),
}));

describe('AdminPlaceholder', () => {
  const mockUser: AdminUser = {
    id: '1',
    email: 'test@example.com',
    role: 'ADMIN',
  };

  const defaultProps = {
    title: 'Test Page',
    subtitle: 'Test Subtitle',
    description: 'This is a test description.',
    user: mockUser,
    onLogout: vi.fn(),
  };

  it('renders the title, subtitle, and description', () => {
    render(<AdminPlaceholder {...defaultProps} />);
    
    // Check for title and subtitle (rendered by the mocked AdminHeader)
    expect(screen.getByRole('heading', { name: /Test Page/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Test Subtitle/i })).toBeInTheDocument();

    // Check for the main description
    expect(screen.getByText('This is a test description.')).toBeInTheDocument();
    
    // Check that the "Proximamente" text is there
    expect(screen.getByRole('heading', { name: /Proximamente/i })).toBeInTheDocument();
  });

  it('renders the child components', () => {
    render(<AdminPlaceholder {...defaultProps} />);
    expect(screen.getByTestId('admin-sidebar')).toBeInTheDocument();
  });
});
