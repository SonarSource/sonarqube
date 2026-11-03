# Auto-approval criteria

A PR is safe to auto-approve when both of the following are true:

- It was opened by the Remediation Agent's production bot, `sonarqube-agent[bot]`.
- Gitar's own whole-PR risk assessment is **Low risk**. If no risk assessment is available, this
  criterion is not met.
- The PR does not modify any file under `.gitar/` or `.github/`. PRs touching these paths are never
  auto-approved, regardless of author or risk rating.

Do not auto-approve a PR from any other author, or a `sonarqube-agent[bot]` PR at any other risk
rating — leave those to a human/org default.
