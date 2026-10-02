import { render, screen, fireEvent } from '@testing-library/react';
import AdminActionButton from '../AdminActionButton';

describe('AdminActionButton', () => {
  it('renders the button with a label', () => {
    render(<AdminActionButton label="Click Me" />);
    const buttonElement = screen.getByRole('button', { name: /Click Me/i });
    expect(buttonElement).toBeInTheDocument();
  });

  it('renders children when label is not provided', () => {
    render(<AdminActionButton>Child Content</AdminActionButton>);
    const buttonElement = screen.getByRole('button', { name: /Child Content/i });
    expect(buttonElement).toBeInTheDocument();
  });

  it('calls onClick handler when clicked', () => {
    const handleClick = vi.fn();
    render(<AdminActionButton label="Clickable" onClick={handleClick} />);
    const buttonElement = screen.getByRole('button', { name: /Clickable/i });
    fireEvent.click(buttonElement);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('disables the button when disabled prop is true', () => {
    const handleClick = vi.fn();
    render(<AdminActionButton label="Disabled" onClick={handleClick} disabled />);
    const buttonElement = screen.getByRole('button', { name: /Disabled/i });
    expect(buttonElement).toBeDisabled();
    fireEvent.click(buttonElement);
    expect(handleClick).not.toHaveBeenCalled();
  });
});
