import { type NextRequest, NextResponse } from 'next/server';
import { getBaseApiUrl } from '@/lib/env';

// This generic handler will proxy requests for all HTTP methods.
async function handler(
  request: NextRequest,
  { params }: { params: { path: string[] } }
) {
  // 1. Get the authentication token from the secure httpOnly cookie.
  const token = request.cookies.get('dismal_token')?.value;

  if (!token) {
    return NextResponse.json({ message: 'Not authenticated' }, { status: 401 });
  }

  // 2. Get the real backend API URL from environment variables.
  const baseApiUrl = getBaseApiUrl();
  if (!baseApiUrl) {
    return NextResponse.json(
      { message: 'Backend API URL is not configured on the server.' },
      { status: 500 }
    );
  }

  // 3. Reconstruct the full target URL to the Java backend.
  const path = params.path.join('/');
  const search = request.nextUrl.search;
  const targetUrl = `${baseApiUrl}/${path}${search}`;

  // 4. Forward the request to the real backend.
  // It includes the original request's body and method, but adds the crucial auth header.
  const response = await fetch(targetUrl, {
    method: request.method,
    headers: {
      'Content-Type': request.headers.get('Content-Type') || 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: request.body,
    // The 'duplex' property is required by Next.js to stream request bodies.
    // @ts-ignore
    duplex: 'half',
  });

  // 5. Return the response from the real backend directly to the original client.
  // This transparently passes back the status, headers, and body.
  return response;
}

// Export the single handler for all common HTTP methods.
export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const DELETE = handler;
export const PATCH = handler;
