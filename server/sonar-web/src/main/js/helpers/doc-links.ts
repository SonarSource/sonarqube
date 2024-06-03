/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import { AlmKeys } from '../types/alm-settings';

export const DOC_URL = 'https://docs.sonarsource.com/sonarqube/latest';

export enum DocLink {
  AccountTokens = '/user-guide/user-account/generating-and-using-tokens/',
  ActiveVersions = '/setup-and-upgrade/upgrade-the-server/active-versions/',
  AlmAzureIntegration = '/devops-platform-integration/azure-devops-integration/',
  AlmBitBucketCloudAuth = '/instance-administration/authentication/bitbucket-cloud/',
  AlmBitBucketCloudIntegration = '/devops-platform-integration/bitbucket-integration/bitbucket-cloud-integration/',
  AlmBitBucketCloudSettings = '/instance-administration/authentication/bitbucket-cloud/#setting-your-authentication-settings-in-sonarqube',
  AlmBitBucketServerIntegration = '/devops-platform-integration/bitbucket-integration/bitbucket-server-integration/',
  AlmGitHubAuth = '/instance-administration/authentication/github/',
  AlmGitHubIntegration = '/devops-platform-integration/github-integration/',
  AlmGitHubMonorepoWorkfileExample = '/devops-platform-integration/github-integration/monorepo/#workflow-file-example',
  AlmGitLabAuth = '/instance-administration/authentication/gitlab/',
  AlmGitLabAuthProvisioningMethod = '/instance-administration/authentication/gitlab/#choosing-the-provisioning-method',
  AlmGitLabIntegration = '/devops-platform-integration/gitlab-integration/',
  AlmSamlAuth = '/instance-administration/authentication/saml/overview/',
  AlmSamlScimAuth = '/instance-administration/authentication/saml/scim/overview/',
  AnalysisScope = '/project-administration/analysis-scope/',
  AuthOverview = '/instance-administration/authentication/overview/',
  BackgroundTasks = '/analyzing-source-code/background-tasks/',
  BranchAnalysis = '/analyzing-source-code/branches/branch-analysis/',
  CaYC = '/user-guide/clean-as-you-code/',
  CFamily = '/analyzing-source-code/languages/c-family/',
  CFamilyAnalysisCache = '/analyzing-source-code/languages/c-family/#analysis-cache',
  CIAnalysisSetup = '/analyzing-source-code/ci-integration/overview/',
  CIJenkins = '/analyzing-source-code/ci-integration/jenkins-integration/',
  CleanCodeIntroduction = '/user-guide/clean-code/introduction/',
  CodeAnalysis = '/user-guide/clean-code/code-analysis/',
  InactiveBranches = '/analyzing-source-code/branches/branch-analysis/#inactive-branches',
  InstanceAdminEncryption = '/instance-administration/security/#settings-encryption',
  InstanceAdminLicense = '/instance-administration/license-administration/',
  InstanceAdminLoC = '/instance-administration/monitoring/lines-of-code/',
  InstanceAdminMarketplace = '/instance-administration/marketplace/',
  InstanceAdminPluginVersionMatrix = '/instance-administration/plugin-version-matrix/',
  InstanceAdminQualityProfiles = '/instance-administration/quality-profiles/',
  InstanceAdminQualityProfilesPrioritizingRules = '/instance-administration/quality-profiles/#prioritizing-rules',
  InstanceAdminReindexation = '/instance-administration/reindexing/',
  InstanceAdminSecurity = '/instance-administration/security/',
  IssueResolutions = '/user-guide/issues/#resolutions-deprecated',
  Issues = '/user-guide/issues',
  IssueStatuses = '/user-guide/issues/#statuses',
  MainBranchAnalysis = '/analyzing-source-code/branches/branch-analysis/#main-branch',
  MetricDefinitions = '/user-guide/metric-definitions/',
  Monorepos = '/project-administration/monorepos/',
  NewCodeDefinition = '/project-administration/clean-as-you-code-settings/defining-new-code/',
  NewCodeDefinitionOptions = '/project-administration/clean-as-you-code-settings/defining-new-code/#new-code-definition-options',
  Portfolios = '/user-guide/portfolios/',
  PullRequestAnalysis = '/analyzing-source-code/pull-request-analysis',
  QualityGates = '/user-guide/quality-gates/',
  Root = '/',
  RulesOverview = '/user-guide/rules/overview',
  SecurityHotspots = '/user-guide/security-hotspots/',
  SecurityReports = '/user-guide/security-reports/',
  ServerUpgradeRoadmap = '/setup-and-upgrade/upgrade-the-server/roadmap/',
  SonarLintConnectedMode = '/user-guide/sonarlint-connected-mode/',
  SonarScanner = '/analyzing-source-code/scanners/sonarscanner/',
  SonarScannerDotNet = '/analyzing-source-code/scanners/sonarscanner-for-dotnet/',
  SonarScannerGradle = '/analyzing-source-code/scanners/sonarscanner-for-gradle/',
  SonarScannerMaven = '/analyzing-source-code/scanners/sonarscanner-for-maven/',
  Webhooks = '/project-administration/webhooks/',
}

export const DocTitle = {
  [DocLink.BackgroundTasks]: 'About Background Tasks',
  [DocLink.CaYC]: 'Clean as You Code',
  [DocLink.CIAnalysisSetup]: 'Set up CI analysis',
  [DocLink.InstanceAdminQualityProfiles]: 'About Quality Profiles',
  [DocLink.MetricDefinitions]: 'Metric Definitions',
  [DocLink.NewCodeDefinition]: 'Defining New Code',
  [DocLink.PullRequestAnalysis]: 'Analyzing Pull Requests',
  [DocLink.SecurityReports]: 'About Security Reports',
  [DocLink.SonarLintConnectedMode]: 'SonarLint Connected Mode',
  [DocLink.Webhooks]: 'About Webhooks',
};

export type DocTitleKey = keyof typeof DocTitle;

const asDocSections = <T>(element: { [K in keyof T]: DocTitleKey[] }) => element;

export const DocSection = asDocSections({
  component_measures: [DocLink.CaYC, DocLink.MetricDefinitions],
  overview: [
    DocLink.PullRequestAnalysis,
    DocLink.CIAnalysisSetup,
    DocLink.CaYC,
    DocLink.SonarLintConnectedMode,
  ],
  pull_requests: [DocLink.CaYC, DocLink.PullRequestAnalysis, DocLink.SonarLintConnectedMode],
});

export type DocSectionKey = keyof typeof DocSection;

export const AlmAuthDocLinkKeys = {
  [AlmKeys.BitbucketServer]: DocLink.AlmBitBucketCloudAuth,
  [AlmKeys.GitHub]: DocLink.AlmGitHubAuth,
  [AlmKeys.GitLab]: DocLink.AlmGitLabAuth,
  saml: DocLink.AlmSamlAuth,
};
