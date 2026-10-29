# Security Review Request: OAuth for the Hosted SonarQube Server MCP Endpoint

**Ticket:** [SONAR-33234](https://sonarsource.atlassian.net/browse/SONAR-33234) · **Requester:** Aurélien Poscia (Identity)

This request describes the proposed implementation under [SONAR-33234](https://sonarsource.atlassian.net/browse/SONAR-33234), based on [POC PR #19437](https://github.com/SonarSource/sonar-enterprise/pull/19437), its [specification](https://github.com/SonarSource/sonar-enterprise/blob/475ca2630709d5000af90bcbbc28bae12165f3fc/private/core-extension-mcp/OAUTH-SPRING-PLAN.md), and the Jira work breakdown. Security review is tracked in [SONAR-33254](https://sonarsource.atlassian.net/browse/SONAR-33254).

| Component | Repo | Responsibility |
| --- | --- | --- |
| Embedded authorization server and MCP proxy | sonar-enterprise | Discovery, browser identity adapter, authorization, token issuance, A verification, and local A → B delegation |
| API authentication | sonar-enterprise | B and parent-authorization verification, scope enforcement, active-user checks, and existing permissions |
| Private MCP process | sonarqube-mcp-server | Tool execution and downstream API calls using B |
| Consent frontend | sonarcloud-webapp | Display validated application/access details and submit Allow/Deny decisions |
| OAuth persistence | sonar-enterprise | Registered clients, authorizations, and consent in the SonarQube database |

## Why

The hosted SonarQube Server MCP endpoint currently accepts personal access tokens pasted into the client configuration. We want approved MCP clients to connect through the user's existing SonarQube login, with explicit consent, scope-limited access, and a fixed token lifetime.

## What: target flow

```mermaid
sequenceDiagram
    autonumber
    participant C as MCP client
    participant S as SonarQube MCP proxy
    participant O as Embedded authorization server
    participant M as Private MCP process
    participant A as SonarQube APIs

    C->>S: MCP request, no token
    S-->>C: 401 + WWW-Authenticate (resource_metadata, scope)
    C->>O: Protected-resource and authorization-server metadata
    O-->>C: Resource, issuer, endpoints, scopes
    C->>O: Authorization Code + PKCE S256, client_id, resource=…/mcp
    O->>O: Existing browser login + consent
    O-->>C: Authorization code through validated callback
    C->>O: Code + verifier + client + original callback + resource
    O-->>C: Opaque token A, expires_in=604800; no refresh token
    C->>S: MCP request + Bearer A
    S->>S: Validate A, active user, resource, and scopes
    S->>S: Create or reuse B, bound to A and its scopes
    S->>M: MCP request + Bearer B
    M->>A: API call + Bearer B
    A->>A: Validate B and parent authorization
    A->>A: Check scope + current user/resource permissions
    A-->>M: Result or authorization error
    M-->>S: MCP result
    S-->>C: MCP result
```

## Security-relevant decisions

### D1. SonarQube embeds the authorization server and reuses existing login

- Spring Authorization Server handles Authorization Code, PKCE, token responses, and JDBC persistence inside the SonarQube web process.
- The browser authenticates through the existing SonarQube login and configured identity providers. The OAuth adapter uses the validated user's UUID; token authentication resolves an existing active user.
- Clients are public clients: no client secret, mandatory PKCE S256, and user consent.
- Only Authorization Code is supported. Dynamic Client Registration and refresh-token grants are not enabled.
- The Spring security chain is scoped to OAuth routes; existing application authentication and resource permissions remain in place.

### D2. Two scopes: `mcp:readAll` and `mcp:writeAll`, aligned with SQC

- `mcp:readAll` permits read operations; `mcp:writeAll` permits write operations. Neither scope implies the other. Clients needing both request both.
- Scopes are a **delegation ceiling**, not permissions. Effective access is **granted scopes ∩ the user's current SonarQube permissions**. Delegation does not create project or administrative rights.
- Discovery, registration, authorization, consent, A, B, and backend authorization use the same scope contract. B's scopes must be a subset of A's.
- Backend APIs enforce scopes for the actual operation. The outer MCP POST, tool visibility, `readOnlyHint`, and client-supplied read-only settings are not authorization boundaries.
- Review the operation mapping and sensitive writes, including any operation that can create credentials or change permissions. A read-only grant must not be able to perform writes or obtain broader access.

### D3. Access token A expires after one week; no refresh tokens

- **A expires seven days after issuance: 604,800 seconds.** The lifetime is absolute; activity and B reuse do not extend it.
- The token response contains no `refresh_token`. The server does not advertise or accept `grant_type=refresh_token`.
- When A expires, the client must run Authorization Code + PKCE again. Existing browser login and stored consent may reduce the required interaction.
- **A stolen bearer token can remain usable until expiry or revocation.** PKCE protects code redemption, not replay of an issued access token. Client-side storage, redaction, and usable revocation are therefore important review points.
- Browser logout is not a substitute for connection revocation. B's shorter lifetime does not shorten A's exposure window: a valid A can obtain further B credentials.

### D4. Separate client and internal credentials instead of token passthrough

- **Token A:** opaque, bound in stored metadata to the canonical MCP resource, and accepted only at the public MCP boundary. It cannot authenticate business APIs and is never forwarded downstream.
- **Credential B:** opaque, created locally by SonarQube, and used only by the private MCP process for API calls. It cannot authenticate public MCP requests and is never returned to the external client.
- B lasts **at most five minutes**, capped by A's expiry. Its authorization record references the parent authorization and the same user.
- A → B is a local service operation. It requires no external token-exchange endpoint or exchange-client secret.
- The POC generates tokens from 32 random bytes using `SecureRandom`, with separate prefixes for A and B. Prefixes select the validation path; they do not establish validity.

### D5. B verification preserves parent validity, scopes, and normal permissions

- Each API request validates B's expiry and purpose, its active parent authorization, resource binding, and user binding.
- The user must still exist and be active. Scope enforcement and normal endpoint/resource permissions both apply.
- Revoking the parent authorization prevents subsequent use of every associated B. Permission changes take effect through normal API authorization.
- A selected OAuth credential that fails validation stops authentication. It must not fall back to PAT or browser authentication.
- Review wrong-purpose, wrong-resource, wrong-user, expired, revoked, malformed, and insufficient-scope credentials, including ambiguous or duplicate authorization headers.

### D6. Browser consent and callbacks remain bound to the authorization request

- Authorization codes are single-use; the POC sets a 60-second lifetime. Redemption validates the client, PKCE verifier, original callback, and MCP resource.
- Consent displays the validated application identity, callback, requested read/write access, and token lifetime. Details come from the pending server-side authorization, bound to the current user and registered client.
- Allow/Deny processing retains SonarQube browser-session CSRF protection. The POC disables Spring's generic CSRF filter for the scoped OAuth chain and performs the browser-session check explicitly; this integration needs review.
- The frontend must preserve those checks, escape untrusted metadata, and prevent framing. Administrator approval of a client does not replace user consent.
- Callbacks use HTTPS or HTTP loopback. For public clients with PKCE, a portless `http://localhost/callback` registration can accept a dynamic port; other URI components must match. Token exchange must use the exact callback selected during authorization, including the port.
- Review callback substitution, open redirects, consent tampering, wrong-user/expired state, denial, and code replay.

### D7. CIMD onboarding is administrator-approved

- Clients are preregistered or imported from exact administrator-approved Client ID Metadata Document (CIMD) URLs. Unknown client IDs do not trigger runtime network discovery.
- Administrator APIs manage registration, update, enable/disable, and deletion. Imported metadata cannot broaden local scope, grant, PKCE, or consent policy.
- Direct JSON import supports air-gapped deployments with the same validation and exact client-ID matching as URL imports. Authorization and offline startup use the stored registration.
- Client updates and disable/delete operations need consistent effects on existing grants and consent across nodes. Callback or identity changes must not silently expand previously approved access.

### D8. CIMD fetching is a server-side request forgery boundary

- The POC fetcher requires approved HTTPS URLs, disables redirects and outbound proxies, and rejects DNS results containing blocked addresses.
- Retrieval has a three-second call timeout, a 16 KiB body limit, and requires HTTP 200. Metadata is validated before persistence.
- Review literal IP addresses, IPv6, DNS rebinding, mixed DNS answers, redirects, and access to local or cloud metadata services. A failed import must not weaken an existing registration.
- CIMD integration uses `org.springaicommunity:mcp-authorization-server`. The plan recommends retaining this community dependency while explicitly reviewing its maturity, the integration we use, and ownership of security fixes and upgrades.

### D9. Spring JDBC persists token values

- Spring stores clients, authorizations, and consent in `oauth2_registered_client`, `oauth2_authorization`, and `oauth2_authorization_consent`, created through SonarQube migrations.
- **Bearer-token values are persisted, not only hashes.** B is a separate authorization record linked to A.
- Database access, backups, snapshots, and diagnostic exports must be reviewed as potential access to usable credentials, particularly with A valid for a week.
- Token values, authorization codes, PKCE verifiers, and credential-bearing headers must not appear in logs.
- Bounded cleanup removes expired records without deleting live grants or useful consent. Credential rejection depends on validation, not cleanup timing.

### D10. Revocation and credential reuse must work together

- Users and administrators need authorized connection listing and revocation, with defined consent-removal behavior.
- Revocation must block subsequent A and B use across nodes, including cached B. Account deactivation and client lifecycle changes must be handled consistently.
- B reuse uses a bounded per-node cache keyed by a fingerprint of the exact A, not just user or client. A and the active user are validated before every cache lookup.
- Cached B remains subject to its own expiry, A's expiry, parent validation, and scope restrictions. The cache reuses a credential; it does not cache permission to execute requests.
- Concurrent misses should be coalesced without mixing grants. Credential failures must not cause blind retries of write operations.

### D11. Discovery and proxying preserve the authentication boundary

- The canonical issuer/resource comes from trusted SonarQube configuration, not incoming Host or Forwarded headers. Nonlocal public URLs require HTTPS.
- Discovery uses `/.well-known/oauth-authorization-server` and `/.well-known/oauth-protected-resource/mcp`; authorization and token issuance use `/oauth2/authorize` and `/oauth2/token`.
- Unauthenticated MCP requests receive a Bearer discovery challenge. Authentication exclusions remain limited to intended routes and their security handlers.
- The private MCP listener is inaccessible to external clients. The proxy replaces A with B, strips browser cookies and alternative credentials, forwards an explicit header allowlist, and disables credential-bearing backend redirects. Cross-host traffic must be protected.
- Existing bearer PATs retain their separate validation and forwarding path, without OAuth records. OAuth scopes and the one-week lifetime apply to OAuth credentials.
- Review HTTPS proxies, context paths, route normalization, Origin/CORS behavior, sensitive-response caching, and request-size/rate limits.

### D12. Production validation covers persistence and Data Center concurrency

- Verify single-use code redemption under concurrent requests on different nodes. Shared JDBC persistence alone does not establish atomic redemption.
- Exercise revocation, client updates, restarts/failover, expiry boundaries, cleanup, and cache isolation across nodes with consistent configuration.
- Validate persistence on H2, PostgreSQL, Oracle, and SQL Server.
- Supported MCP clients must demonstrate discovery, callbacks, consent, read/write enforcement, reauthorization after expiry, and recovery after revocation.
- Record security sign-off and the disposition of findings under SONAR-33254.

## Out of scope

The scope is hosted MCP OAuth. Refresh tokens, Dynamic Client Registration, unrestricted runtime CIMD discovery, fine-grained/per-project scopes, and local/stdio MCP OAuth are excluded.
