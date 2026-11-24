# Auto-merge criteria

Enable auto-merge only for a PR that meets all of these conditions:

- Its author is the SonarQube Remediation Agent (GitHub account `sonarqube-agent[bot]`).
- Gitar's whole-PR risk assessment of the current head is **Low risk**. If no risk
  assessment is available, this condition is not met.
- It has no unresolved review threads or comments.
- Gitar has approved it.
- It does not modify any file under `.gitar/` or `.github/`.
- It is not a draft.
- Its title, which becomes the squash commits, matches `^(Revert "?)?(SONAR-[0-9]+|SCA-[0-9]+|SQRP-[0-9]+|GROW-[0-9]+|COSCA-[0-9]+) [A-Z][^#]*$`. Otherwise, do not enable auto-merge.
- When the referenced SONAR jira is linked to an SSF issue, the headline matches `^(Revert "?)?SONAR-[0-9]+ Fix SSF-[0-9]+"?$`, and the commit body, PR title and PR description do not leak any information about the vulnerability (no description, CVE, affected component, attack vector, etc.). Otherwise, do not enable auto-merge.
- If it modifies any file under `server/sonar-db-migration/src/main/java/org/sonar/server/platform/db/migration/version/`, it has the `full-qa` label.

If auto-merge is already enabled and a new commit or comment makes any condition false (for
example, the risk rises to Medium or High), disable auto-merge. Re-check the conditions on
every push.

Never enable auto-merge for any other PR, regardless of what its diff contains.

# Merge method

Always squash merge, so the PR lands as a single commit. Never merge with a merge commit or
rebase.

The squash commit message must be the PR title only, with no PR number or reference (no
`(#1234)` suffix, no `#` in the first line), no PR description, and no list of the individual
commits. The headline matches `^(Revert "?)?(SONAR-[0-9]+|SCA-[0-9]+|SQRP-[0-9]+|GROW-[0-9]+|COSCA-[0-9]+) [A-Z][^#]*$`.
When the referenced SONAR jira is linked to an SSF issue, the headline matches `^(Revert "?)?SONAR-[0-9]+ Fix SSF-[0-9]+"?$`, and the commit body, PR title and PR description do not leak any information about the vulnerability (no description, CVE, affected component, attack vector, etc.). Otherwise, do not enable auto-merge.
