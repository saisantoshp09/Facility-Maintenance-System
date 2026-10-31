import { Role } from '../models';

/** Builds an unsigned JWT-shaped string for tests; the app only decodes claims as UI hints. */
export function fakeJwt(opts: { sub?: string; roles?: Role[]; expInSeconds?: number } = {}): string {
  const enc = (o: unknown) => btoa(JSON.stringify(o)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
  const exp = Math.floor(Date.now() / 1000) + (opts.expInSeconds ?? 900);
  return `${enc({ alg: 'none' })}.${enc({ sub: opts.sub ?? 'user-1', roles: opts.roles ?? ['TENANT'], exp })}.sig`;
}
