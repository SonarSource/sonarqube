# SONAR-33238 — Persist OAuth clients, authorizations, and consent

## Scope

Integrate Spring JDBC repositories and SonarQube database migrations for OAuth clients, authorizations, and consent. Verify installation, upgrade, and restart persistence; provide static client registration and validate client-ID capacity.

- Ticket: https://sonarsource.atlassian.net/browse/SONAR-33238
- Parent: SONAR-33234 — OAuth authentication for MCP clients (SQS)
- Reference POC: https://github.com/SonarSource/sonar-enterprise/pull/19437
- POC branch: feature/ap/poc-mcp-oauth-spring
- Required base: feature/ap/SONAR-33237-mcp-oauth-discovery
- Delivery: an additional stacked PR directly above SONAR-33237.

## Agreed decisions

- The user will finish SONAR-33237 separately. Do not commit or publish its changes on their behalf.
- Do not register any demo client or provide demo client-ID/callback defaults. Static registration requires explicit administrator configuration.
- Support client IDs longer than the POC's 100-character limit. Determine a suitable capacity and database-compatible storage/indexing during implementation.
- Report a mismatch when explicit configuration differs from a stored registration; do not silently overwrite the registration.
- Keep browser authorization/token issuance and CIMD imports in their separate tickets.

## Delivery

- Implemented on `feature/ap/SONAR-33238-oauth-persistence`, commit `d710cf30594833270aaabe990da470eb775f578e`.
- Draft PR: https://github.com/SonarSource/sonar-enterprise/pull/19480
- Base PR: https://github.com/SonarSource/sonar-enterprise/pull/19479 at `0dffcde83a3514d87413ab0cd0c558dd27f634da`.
- GitHub stack #19481 contains #19479 followed by #19480; created with `gh stack link 19479 19480 --remote origin`.
- No Jira comments or status changes were made.

## Implementation and validation

- Spring JDBC client, authorization, and consent repositories use SonarQube's existing data source and hosted-MCP enablement.
- Migrations 202606020–202606022 create all three tables. Schema inventory and generated schema updated.
- Explicit static registration properties: `sonar.mcp.oauth.clientId` and `sonar.mcp.oauth.redirectUri`, with no defaults.
- Client IDs support 512 printable ASCII characters without spaces. Callback URLs allow HTTPS or loopback HTTP with validation.
- Startup registration implements SonarQube Startable, reuses matching registrations, rejects mismatches, and handles concurrent matching inserts.
- Token values use 512-character text columns to allow Oracle equality predicates, with code/access-token indexes; serialized metadata uses large-text columns.
- Preserves POC client policy: public clients, PKCE/consent, authorization code, mcp:access, 10-minute access tokens, 60-second codes. Issuance and lifecycle changes remain separate work.
- 101 MCP tests, 5 migration/version tests, and 1 schema inventory test passed. File-backed H2 restart and concurrent startup tested.
- Strict Java license checks, schema dump consistency, and Enterprise obfuscation passed with ./gradlew and Java 21.
- Packaged configuration factory method names inspected and preserved. Obfuscator emitted unresolved-external-dependency warnings.
- PostgreSQL, Oracle, and SQL Server were not executed locally; timestamp dialect definitions are unit tested. No deployed-server smoke test performed.
- SSH push failed; push succeeded using an explicit per-command HTTPS push URL and the authenticated GitHub CLI credential helper. No persistent remote changes needed.

## Implementation references

- POC McpOAuthComponents wires Spring JDBC repositories to SonarQube's database.
- POC McpOAuthClientRegistrar creates a demo client by default and imports CIMD clients; extract only the static-registration behavior needed for this ticket and remove demo defaults.
- POC migrations CreateOAuthRegisteredClientsTable, CreateOAuthAuthorizationsTable, CreateOAuthAuthorizationConsentsTable, and OAuthJdbcColumnDefs are under server/sonar-db-migration.
- The POC indexes client_id uniquely with a 100-character column. Longer IDs require deliberate cross-database capacity/indexing checks.
- Use ./gradlew for builds and tests, follow the user's branch naming convention, and preserve a trailing empty line in Java files.

## Separate epoch-millisecond fix (October 28, 2026)

- User requested a separate commit for review comment 4404542255.
- Local commit: cbe63ca9b73 — SONAR-33238 Store OAuth dates as epoch milliseconds.
- Replaces timestamp migration columns with BigIntegerColumnDef, adds Spring mapper adapters for reads/writes, updates schema/docs, and adds cross-time-zone persistence tests including nullable dates and epoch zero.
- Wires the new persistence suite into DbAwareTest, run-db-unit-test.sh, and ci-setup-variables.yml path filters.
- Passed 105 MCP tests, 5 migration tests, 1 schema test, strict license checks, schema consistency, database vendor coverage check (requires --no-configure-on-demand), and Enterprise obfuscation.
- SQL Server was not available locally; vendor execution remains for CI.
- Push REJECTED: current GitHub OAuth token lacks workflow scope required to update .github/workflows/ci-setup-variables.yml. gh auth status confirms scopes admin:public_key, gist, read:org, repo only. SSH authentication previously failed.
- Remote PR remains at d710cf30594833270aaabe990da470eb775f578e. Do not claim the separate fix is published.
- Prepared revised PR body: /private/tmp/sonar33238-epoch-pr.md. Apply only after successful push.
- Next step: user grants workflow permission via gh auth refresh -h github.com -s workflow (or provides working authorized SSH authentication), then push the existing commit over HTTPS using gh auth git-credential, update PR body, and verify remote head.

## Push retry completed

- User requested retry after the authentication block.
- Pushed cbe63ca9b737ccb91e1bc24a52c2cf4aeea77b15 successfully and verified it is PR #19480's head.
- PR base remains feature/ap/SONAR-33237-mcp-oauth-discovery.
- Updated the PR description with epoch-millisecond storage and the 105 MCP / 5 migration / 1 schema test validation.

## Quality-gate follow-up (October 29, 2026)

- User asked to inspect the quality gate and fix worthwhile findings.
- SonarQube analysis is on Next: org.sonarsource.sonarqube:sonarqube-private, PR 19480. CLI/MCP are authenticated to SonarQube Cloud instead; unauthenticated Next API access returned 403. Retrieved exact gate failure and all five annotations through GitHub check 123068629963.
- Gate failed on 5 new issues: S1186 intentional empty stop, S4449 nullable data source, S2699 missing explicit assertion, S103 long line, S8924 nonstatic Mockito call.
- Fixed all five in f6f35dc2ad7ba4cd044364f833b351ec30cab21a and pushed. Added resource-ownership explanation to stop, requireNonNull data source, explicit no-exception assertion plus repository interaction checks, line wrapping, and static doThrow import.
- 105 MCP tests and strict license check passed via ./gradlew on Java 21.
- New CI run: 40844779607. Awaiting fresh SonarQube verdict for this exact head. No issue statuses were changed or suppressed.
- Previous head cbe63ca9b73 passed PostgreSQL 15/18 and SQL Server 2017/2025 jobs; Oracle was skipped.

### Fresh quality gate confirmed

SonarQube Code Analysis check 123352838163 on f6f35dc2ad7ba4cd044364f833b351ec30cab21a completed successfully: Quality Gate passed, 0 new issues, 0 accepted issues, 0 security hotspots, 0 dependency risks, 95.9% new-code coverage, and 0.0% new-code duplication. CI build and JUnit also passed. All five findings are cleared by the code changes.
