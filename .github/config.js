module.exports = {
    onboardingConfig: {
      extends: ["config:base"],
    },
    platform: "github",
    onboarding: false,
    includeForks: false,
    branchPrefix: "renovate/",
    gitAuthor: "renovate bot <111297361+hashicorp-vault-sonar-prod[bot]@users.noreply.github.com>",
    username: "hashicorp-vault-sonar-prod[bot]",
    baseBranches: ["master"],
    repositories: [
      "SonarSource/sonar-enterprise",
    ],
  };
