module.exports = {
    onboardingConfig: {
      extends: ["config:base"],
    },
    platform: "github",
    onboarding: false,
    includeForks: false,
    branchPrefix: "renovate/",
    gitAuthor: "sonarqubetech-sonarenterprise <78919706+sonarqubetech-sonarenterprise@users.noreply.github.com>",
    baseBranches: ["master"],
    repositories: [
      "SonarSource/sonar-enterprise",
    ],
  };
