# Sangkub portability rules

Sangkub must remain movable between hosting and database providers.

## Hosting
- The Next.js app must build with `npm run build` and run with `npm run start` on a normal Node.js host.
- Do not require Vercel-only runtime APIs for core ordering, kitchen, cashier, billing, or reporting.
- Hosting configuration belongs in environment variables, not source code.

## Database
- Business data uses PostgreSQL-compatible tables and SQL migrations.
- Keep restaurant, table, menu, order, bill, payment, and device IDs owned by Sangkub.
- Do not use provider project IDs as business identifiers.
- Provider connection URL and public key are environment variables.
- Provider-specific realtime/auth/storage features must be isolated behind adapters before they become required by core flows.

## Files / images
- Store file references as portable URLs/keys. UI must not assume one storage vendor.

## Core boundary
UI -> application/service boundary -> data adapter -> PostgreSQL/provider.

Core business rules must not depend on Vercel deployment metadata or Supabase project identity.

## Required environment variables
Current compatibility names:
- `NEXT_PUBLIC_DATABASE_API_URL` (preferred portable name) or `NEXT_PUBLIC_SUPABASE_URL`
- `NEXT_PUBLIC_DATABASE_PUBLIC_KEY` (preferred portable name) or `NEXT_PUBLIC_SUPABASE_ANON_KEY`

Before migrating providers, export PostgreSQL schema/data, configure the new adapter/endpoint, set environment variables, build, and run the same application tests.
