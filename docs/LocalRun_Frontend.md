# Running Frontend Locally

This guide explains how to run the P-Cal frontend on your local machine without Docker.

## Prerequisites

- **Node.js 18+** installed (20+ recommended)
- **npm 9+** or **yarn** package manager
- **Backend running** on `http://localhost:8080` (see `LocalRun_Backend.md`)

## Setup (First Time Only)

### Install Dependencies

```bash
cd frontend
npm install
```

This will install all required packages including Vue 3, Vite, TypeScript, and other dependencies.

## Running the Frontend

### Development Mode

```bash
cd frontend
npm run dev
```

The application will start on **http://localhost:5173**

**Features in dev mode:**
- Hot Module Replacement (HMR) - instant updates on file changes
- Vite dev server with built-in proxy
- Source maps enabled for debugging
- Vue DevTools support

### Vite Proxy Configuration

The frontend automatically proxies API requests to avoid CORS issues:

- Frontend: `http://localhost:5173`
- API calls to `/api/*` are proxied to → `http://localhost:8080/api/*`

No additional configuration needed!

## Building for Production

### Create Production Build

```bash
cd frontend
npm run build
```

Output will be in `frontend/dist/` directory.

### Preview Production Build

```bash
npm run preview
```

Serves the production build locally on http://localhost:4173

## Available Scripts

```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # Run ESLint to check code quality
npm run type-check   # Run TypeScript type checking
npm run test         # Run unit tests (Vitest)
npm run test:watch   # Run tests in watch mode
npm run test:ui      # Run tests with UI
npm run test:coverage # Generate test coverage report
```

## Environment Variables

The frontend uses **Vite** for environment variable management.

### Default Configuration

The only environment variable used is:
- `VITE_API_URL` - Base URL for API calls (default: `/api`)

Vite automatically loads environment files in this order:
1. `.env.local` (local overrides, gitignored)
2. `.env.development` (development mode)
3. `.env.production` (production builds)
4. `.env` (fallback)

### Current Setup

The project works out-of-the-box with defaults. The proxy is pre-configured in `vite.config.ts`.

**If you need to customize**, create `frontend/.env.local`:

```bash
# API Base URL
# Use /api to leverage Vite proxy (recommended)
VITE_API_URL=/api

# Or use direct URL (may have CORS issues)
# VITE_API_URL=http://localhost:8080/api
```

## Verify Frontend is Running

- Open browser: http://localhost:5173
- Check network tab: API calls should go to `/api/*` (proxied to `:8080`)
- Login page should load correctly

## Common Issues

**Port 5173 already in use**
- Kill the existing process: `lsof -i :5173` (Unix) or `netstat -ano | findstr :5173` (Windows)
- Or change port in `vite.config.ts`: `server.port: 5174`

**API calls fail with network error**
- Verify backend is running on `http://localhost:8080`
- Check backend health: http://localhost:8080/actuator/health
- Check browser console for specific error messages

**Hot reload not working**
- Try clearing Vite cache: `rm -rf frontend/node_modules/.vite`
- Restart dev server

**Node/npm version issues**
- Check versions: `node -v` (should be 18+), `npm -v` (should be 9+)
- Update if needed: https://nodejs.org/

## Development Workflow

### Typical Development Setup

1. Start backend: `cd backend && SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`
2. Start frontend: `cd frontend && npm run dev`
3. Open browser: http://localhost:5173
4. Edit files → changes apply instantly via HMR

### Project Structure

```
frontend/
├── src/
│   ├── components/     # Vue components
│   ├── views/          # Page views
│   ├── router/         # Vue Router config
│   ├── stores/         # Pinia stores (state management)
│   ├── services/       # API services
│   ├── composables/    # Vue composables
│   ├── types/          # TypeScript types
│   └── i18n/           # Internationalization
├── public/             # Static assets
└── dist/               # Production build output
```

## IDE Recommendations

### VS Code Extensions
- **Volar** (Vue Language Features)
- **TypeScript Vue Plugin (Volar)**
- **ESLint**
- **Prettier**
- **Tailwind CSS IntelliSense**

### WebStorm/IntelliJ IDEA
Built-in support for Vue 3, TypeScript, and Tailwind CSS.

## Stopping the Frontend

Press `Ctrl+C` in the terminal where the dev server is running.
