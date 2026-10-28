# Facility Maintenance – Web Frontend

Angular 15 (NgModule, lazy-loaded feature modules, Angular Material, reactive forms) client for the
Property & Facility Maintenance Management System. It talks **only to the API Gateway**.

## Layout

```
src/app/
  core/      auth (in-memory token, guards, interceptor), typed API clients (core/api), models,
             error handling, shell layout, property context, user directory
  shared/    SharedModule, badges, confirm dialog, empty/error states, user-id field, service-record dialog
  features/  auth, dashboard, properties, users, work-orders, assets, preventive-maintenance,
             notifications, profile
```

## Prerequisites

- Node.js 18+ (developed on Node 24) and npm
- Google Chrome (for `ng test`; set `CHROME_BIN` if it is not auto-detected)
- Backend running locally; gateway health: `http://localhost:8081/actuator/health`

## Commands

```powershell
cd frontend
npm install
npm start                # ng serve on http://localhost:4200
npm run build            # production build into dist/
npm test                 # Karma/Jasmine, watch mode
npm run test:ci          # single run, ChromeHeadless
```

## Environment

`src/environments/environment.ts` (and `environment.prod.ts`) contain only `apiBaseUrl`
(default `http://localhost:8081`, the gateway). No secrets belong in these files.

**CORS:** the gateway allows the origin `http://localhost:4200` by default (env `CORS_ALLOWED_ORIGINS` on the
gateway). Serve the app on port 4200 or add your origin there. Credentials are not allowed, so cookies are not
used. The gateway exposes no response headers, so the UI relies on `correlationId` in JSON error bodies.

## Development users

Start the backend with profile `dev-seed` and `DEV_SEED_PASSWORD` set locally to get `manager@example.test`,
`tenant@example.test`, `tech@example.test` and a bootstrap admin (the email in `IDENTITY_BOOTSTRAP_ADMIN_EMAIL`).
Passwords are chosen by the developer and are never stored in this repository. Ask the person running the backend.

## Security notes

- The access token lives in memory only (never localStorage/sessionStorage/URLs) and is sent only to `apiBaseUrl`.
- Tokens, passwords and request bodies of login/user creation are never logged.
- Guards and the interceptor are UX only; the backend enforces all authorization. Per-property UI is driven by
  `GET /properties/{id}/access`, and the server remains authoritative (404 means "not found or not available").
- Sorting is restricted to the whitelisted fields per endpoint (`SORT_FIELDS` in `core/models`).

## Behaviour summary

- 401 → token cleared, redirect to `/login` with `returnUrl`; a banner warns about a minute before expiry.
- 403/404/409/503/504 are mapped from the error envelope (also for empty gateway bodies). Idempotent GETs are
  retried at most twice on 0/502/503/504; mutating calls are never retried.
- Notifications are polled (`unread-count`, ~45 s while the tab is visible); there is no push.

## Known limitations

- No refresh token: the token lasts 15 minutes and a page reload requires logging in again. Logout is client-side only.
- No password change/reset or self-registration.
- README mentions Authorization Code + PKCE, but the backend implements password login; the UI follows the backend.
- No user directory for non-admins: names are resolved only for SYSTEM_ADMIN (via `/users/{id}`); others see short ids,
  and managers must paste a user UUID when adding members or assigning technicians.
- Work orders and assets always need a selected property; there is no cross-property view, dashboard, reporting or search API.
- Work orders are not linked to assets and have no attachments.
- Notification delivery is a mock channel; `SENT` does not mean emailed. Notifications arrive seconds after an action.
- Only SYSTEM_ADMIN can create properties; there is no property/building/floor/unit delete or deactivate.
- Reports are not built (no backend).
- The production bundle is ~1.1 MB initial (Angular Material); the budget warning threshold is set to 1.5 MB.
- Component tests use mocked APIs; there is no contract test against a live backend, only typed clients to make drift visible.

## Backend change requests

1. Manager-visible member directory (id → display name/email) so managers can pick technicians/members by name.
2. Expose `Location` and `X-Correlation-ID` via CORS `Access-Control-Expose-Headers` on the gateway.
3. Refresh token (or silent re-auth) and a logout/revocation endpoint; password change/reset.
4. Ensure gateway 401 responses return the standard error JSON envelope.
5. Property deactivate/delete and update/delete endpoints for buildings, floors and units; let managers create properties if intended.
6. Cross-property work-order/task lists and summary (dashboard) endpoints to avoid N calls per property.
7. Remove or implement the dead gateway routes `/maintenance-requests/**`, `/units/**`, `/maintenance-schedules/**`.
8. Validate membership user ids against Identity.
9. Optional: push channel (SSE/WebSocket) for notifications; link work orders to assets; attachments.
