module.exports = {
    onboardingConfig: {
      extends: ["config:base"],
    },
    platform: "github",
    onboarding: false,
    includeForks: false,
    branchPrefix: "renovate/",
    gitAuthor: "infra.sonarqube+github@sonarsource.com",
    baseBranches: ["master"],
    repositories: [
      "SonarSource/sonar-enterprise",
    ],
  };
