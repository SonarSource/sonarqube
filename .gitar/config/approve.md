# Auto-approval criteria

A PR is safe to auto-approve when all of the following are true:

- Gitar's whole-PR risk assessment of the current head is **Low risk**. If no risk assessment is available, this
  criterion is not met.
- The PR does not modify any file under `.gitar/` or `.github/`. PRs touching these paths are never
  auto-approved, regardless of author or risk rating.
- If it modifies any file under `server/sonar-db-migration/src/main/java/org/sonar/server/platform/db/migration/version/`, it has the `full-qa` label.
- When the referenced SONAR jira is linked to an SSF issue, the headline matches `^(Revert "?)?SONAR-[0-9]+ Fix SSF-[0-9]+"?$`, and the commit body, PR title and PR description do not leak any information about the vulnerability (no description, CVE, affected component, attack vector, etc.). Otherwise, the criterion is not met.

Do not auto-approve a PR at any other risk rating — leave those to a human/org default.
