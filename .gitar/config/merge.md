# Auto-merge criteria

Enable auto-merge only for a PR that meets all of these conditions:

- Its author is the SonarQube Remediation Agent (GitHub account `sonarqube-agent[bot]`).
- Gitar's own whole-PR risk assessment of the latest commit is **Low risk**. If no risk
  assessment is available, this condition is not met.
- It has no unresolved review threads or comments.
- Gitar has approved it.
- It does not modify any file under `.gitar/` or `.github/`.
- It is not a draft.
- Its title, which becomes the squash commit message, matches
  `^(SONAR|SCA|SQRP|GROW|COSCA)-[0-9]+ [A-Z][^#]*$`. Otherwise, do not enable auto-merge.

If auto-merge is already enabled and a new commit or comment makes any condition false (for
example, the risk rises to Medium or High), disable auto-merge. Re-check the conditions on
every push.

Never enable auto-merge for any other PR, regardless of what its diff contains.

# Merge method

Always squash merge, so the PR lands as a single commit. Never merge with a merge commit or
rebase.

The squash commit message must be the PR title only, with no PR number or reference (no
`(#1234)` suffix, no `#` in the first line), no PR description, and no list of the individual
commits. The repository rejects commit messages that do not match
`<JIRA-KEY> <Capitalized message>`.
