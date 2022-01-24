module.exports = {
    onboardingConfig: {
      extends: ["config:base"],
    },
    platform: "github",
    onboarding: false,
    includeForks: false,
    branchPrefix: "renovate/",
    gitAuthor: "sonarqubetech-sonarenterprise",
    baseBranches: ["master"],
    repositories: [
      "SonarSource/sonar-enterprise",
    ],
  };
