import { render, screen, fireEvent } from '@testing-library/react';
import AdminHeader from '../AdminHeader';

describe('AdminHeader', () => {
  const defaultProps = {
    title: 'Test Title',
    userEmail: 'test@example.com',
    onLogout: vi.fn(),
  };

  it('renders the title and user email', () => {
    render(<AdminHeader {...defaultProps} />);
    expect(screen.getByRole('heading', { name: /Test Title/i })).toBeInTheDocument();
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
  });

  it('renders the subtitle when provided', () => {
    render(<AdminHeader {...defaultProps} subtitle="Test Subtitle" />);
    expect(screen.getByText(/Test Subtitle/i)).toBeInTheDocument();
  });

  it('does not render the subtitle when not provided', () => {
    render(<AdminHeader {...defaultProps} />);
    expect(screen.queryByText(/Test Subtitle/i)).not.toBeInTheDocument();
  });

  it('renders actions when provided', () => {
    const actions = <button>Action Button</button>;
    render(<AdminHeader {...defaultProps} actions={actions} />);
    expect(screen.getByRole('button', { name: /Action Button/i })).toBeInTheDocument();
  });

  it('calls onLogout when the logout button is clicked', () => {
    const onLogoutMock = vi.fn();
    render(<AdminHeader {...defaultProps} onLogout={onLogoutMock} />);
    const logoutButton = screen.getByRole('button', { name: /Salir/i });
    fireEvent.click(logoutButton);
    expect(onLogoutMock).toHaveBeenCalledTimes(1);
  });
});
