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

import { AlmKeys } from '../../types/alm-settings';
import { ExtendedSettingDefinition } from '../../types/settings';
import { Dict } from '../../types/types';

export const ALM_INTEGRATION_CATEGORY = 'almintegration';
export const AI_CODE_FIX_CATEGORY = 'ai_codefix';
export const AUTHENTICATION_CATEGORY = 'authentication';
export const ANALYSIS_SCOPE_CATEGORY = 'exclusions';
export const LANGUAGES_CATEGORY = 'languages';
export const NEW_CODE_PERIOD_CATEGORY = 'new_code_period';
export const PULL_REQUEST_DECORATION_BINDING_CATEGORY = 'pull_request_decoration_binding';
export const EMAIL_NOTIFICATION_CATEGORY = 'email_notification';
export const MODE_CATEGORY = 'mode';
export const CUSTOMER_CATEGORY = 'customer';

export const CATEGORY_OVERRIDES: Dict<string> = {
  abap: LANGUAGES_CATEGORY,
  ansible: LANGUAGES_CATEGORY,
  apex: LANGUAGES_CATEGORY,
  azureresourcemanager: LANGUAGES_CATEGORY,
  'c / c++ / objective-c': LANGUAGES_CATEGORY,
  'c#': LANGUAGES_CATEGORY,
  cloudformation: LANGUAGES_CATEGORY,
  cobol: LANGUAGES_CATEGORY,
  css: LANGUAGES_CATEGORY,
  dart: LANGUAGES_CATEGORY,
  docker: LANGUAGES_CATEGORY,
  flex: LANGUAGES_CATEGORY,
  go: LANGUAGES_CATEGORY,
  html: LANGUAGES_CATEGORY,
  java: LANGUAGES_CATEGORY,
  javascript: LANGUAGES_CATEGORY,
  'javascript / typescript': LANGUAGES_CATEGORY,
  jcl: LANGUAGES_CATEGORY,
  json: LANGUAGES_CATEGORY,
  kotlin: LANGUAGES_CATEGORY,
  kubernetes: LANGUAGES_CATEGORY,
  php: LANGUAGES_CATEGORY,
  'pl/i': LANGUAGES_CATEGORY,
  'pl/sql': LANGUAGES_CATEGORY,
  python: LANGUAGES_CATEGORY,
  rpg: LANGUAGES_CATEGORY,
  ruby: LANGUAGES_CATEGORY,
  secrets: LANGUAGES_CATEGORY,
  scala: LANGUAGES_CATEGORY,
  swift: LANGUAGES_CATEGORY,
  't-sql': LANGUAGES_CATEGORY,
  terraform: LANGUAGES_CATEGORY,
  typescript: LANGUAGES_CATEGORY,
  'vb.net': LANGUAGES_CATEGORY,
  'visual basic': LANGUAGES_CATEGORY,
  xml: LANGUAGES_CATEGORY,
  yaml: LANGUAGES_CATEGORY,
};

export const SUB_CATEGORY_EXCLUSIONS: Record<string, string[]> = {
  general: ['email'],
};

// As per Bitbucket Cloud's documentation, Workspace ID's can only contain lowercase letters,
// numbers, dashes, and underscores.
export const BITBUCKET_CLOUD_WORKSPACE_ID_FORMAT = /^[a-z0-9\-_]+$/;

export const ADDITIONAL_PROJECT_SETTING_DEFINITIONS: ExtendedSettingDefinition[] = [
  {
    name: 'DevOps Platform Integration',
    description: `
      Display your Quality Gate status directly in your DevOps Platform.
      Each DevOps Platform instance must be configured globally first, and given a unique name. Pick the instance your project is hosted on.
      `,
    category: 'pull_request_decoration_binding',
    key: ``,
    fields: [],
    options: [],
    subCategory: '',
  },
];

export const ADDITIONAL_SETTING_DEFINITIONS: ExtendedSettingDefinition[] = [
  {
    name: 'Default New Code behavior',
    description: `
        The New Code definition is used to compare measures and track new issues.
        This setting is the default for all projects. A specific New Code definition can be configured at project level.
      `,
    category: 'new_code_period',
    key: `sonar.new_code_period`,
    fields: [],
    options: [],
    subCategory: '',
  },
  {
    name: 'Azure DevOps integration',
    description: `azure devops integration configuration
      Configuration name
      Give your configuration a clear and succinct name.
      This name will be used at project level to identify the correct configured Azure instance for a project.
      Azure DevOps URL
      For Azure DevOps Server, provide the full collection URL:
      https://ado.your-company.com/your_collection

      For Azure DevOps Services, provide the full organization URL:
      https://dev.azure.com/your_organization
      Personal Access Token
      SonarQube needs a Personal Access Token to report the Quality Gate status on Pull Requests in Azure DevOps.
      To create this token, we recommend using a dedicated Azure DevOps account with administration permissions.
      The token itself needs Code > Read & Write permission.
    `,
    category: 'almintegration',
    key: `sonar.almintegration.${AlmKeys.Azure}`,
    fields: [],
    options: [],
    subCategory: '',
  },
  {
    name: 'Bitbucket integration',
    description: `bitbucket server cloud integration configuration
      Configuration name
      Give your configuration a clear and succinct name.
      This name will be used at project level to identify the correct configured Bitbucket instance for a project.
      Bitbucket Server URL
      Example: https://bitbucket-server.your-company.com
      Personal Access Token
      SonarQube needs a Personal Access Token to report the Quality Gate status on Pull Requests in Bitbucket Server.
      To create this token, we recommend using a dedicated Bitbucket Server account with administration permissions.
      The token itself needs Read permission.
      Workspace ID
      The workspace ID is part of your bitbucket cloud URL https://bitbucket.org/{workspace}/{repository}
      SonarQube needs you to create an OAuth consumer in your Bitbucket Cloud workspace settings
      to report the Quality Gate status on Pull Requests.
      It needs to be a private consumer with Pull Requests: Read permission.
      An OAuth callback URL is required by Bitbucket Cloud but not used by SonarQube so any URL works.
      OAuth Key
      Bitbucket automatically creates an OAuth key when you create your OAuth consumer.
      You can find it in your Bitbucket Cloud workspace settings under OAuth consumers.
      OAuth Secret
      Bitbucket automatically creates an OAuth secret when you create your OAuth consumer.
      You can find it in your Bitbucket Cloud workspace settings under OAuth consumers.
    `,
    category: 'almintegration',
    key: `sonar.almintegration.${AlmKeys.BitbucketServer}`,
    fields: [],
    options: [],
    subCategory: '',
  },
  {
    name: 'GitHub integration',
    description: `github integration configuration
      Configuration name
      Give your configuration a clear and succinct name.
      This name will be used at project level to identify the correct configured GitHub App for a project.
      GitHub API URL
      Example for Github Enterprise:
      https://github.company.com/api/v3
      If using GitHub.com:
      https://api.github.com/
      You need to install a GitHub App with specific settings and permissions to enable
      Pull Request Decoration on your Organization or Repository.
      GitHub App ID
      The App ID is found on your GitHub App's page on GitHub at Settings > Developer Settings > GitHub Apps
      Client ID
      The Client ID is found on your GitHub App's page.
      Client Secret
      The Client secret is found on your GitHub App's page.
      Private Key
      Your GitHub App's private key. You can generate a .pem file from your GitHub App's page under Private keys.
      Copy and paste the whole contents of the file here.
    `,
    category: 'almintegration',
    key: `sonar.almintegration.${AlmKeys.GitHub}`,
    fields: [],
    options: [],
    subCategory: '',
  },
  {
    name: 'Gitlab integration',
    description: `gitlab integration configuration
      Configuration name
      Give your configuration a clear and succinct name.
      This name will be used at project level to identify the correct configured GitLab instance for a project.
      GitLab API URL
      Provide the GitLab API URL. For example:
      https://gitlab.com/api/v4
      Personal Access Token
      SonarQube needs a Personal Access Token to report the Quality Gate status on Merge Requests in GitLab.
      To create this token,
      we recommend using a dedicated GitLab account with Reporter permission to all target projects.
      The token itself needs the api scope.
    `,
    category: 'almintegration',
    key: `sonar.almintegration.${AlmKeys.GitLab}`,
    fields: [],
    options: [],
    subCategory: '',
  },
];
