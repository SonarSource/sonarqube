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
import { translate } from '../../../../helpers/l10n';
import { GitHubConfigurationResponse } from '../../../../types/dop-translation';
import { SettingType } from '../../../../types/settings';

export enum GitHubAuthFormFields {
  AllowedOrganizations = 'allowedOrganizations',
  ApiUrl = 'apiUrl',
  ApplicationId = 'applicationId',
  ClientId = 'clientId',
  ClientSecret = 'clientSecret',
  PrivateKey = 'privateKey',
  SynchronizeGroups = 'synchronizeGroups',
  WebUrl = 'webUrl',
}

export const GITHUB_AUTH_FORM_FIELDS_ORDER = [
  GitHubAuthFormFields.ClientId,
  GitHubAuthFormFields.ClientSecret,
  GitHubAuthFormFields.ApplicationId,
  GitHubAuthFormFields.PrivateKey,
  GitHubAuthFormFields.SynchronizeGroups,
  GitHubAuthFormFields.ApiUrl,
  GitHubAuthFormFields.WebUrl,
  GitHubAuthFormFields.AllowedOrganizations,
];
const DEFAULT_API_URL = 'https://api.github.com/';
const DEFAULT_WEB_URL = 'https://github.com/';
export const FORM_ID = 'github-configuration-form';

export const GITHUB_PROVISIONING_FIELDS_DEFINITIONS = {
  allowUsersToSignUp: {
    description: translate('settings.authentication.github.form.allowUsersToSignUp.description'),
    key: 'allowUsersToSignUp',
    name: translate('settings.authentication.github.form.allowUsersToSignUp.name'),
    required: false,
    secured: false,
    type: SettingType.BOOLEAN,
  },
  projectVisibility: {
    description: translate('settings.authentication.github.form.projectVisibility.description'),
    key: 'projectVisibility',
    name: translate('settings.authentication.github.form.projectVisibility.name'),
    required: false,
    secured: false,
    type: SettingType.BOOLEAN,
  },
};

export function getInitialGitHubFormData(gitHubConfiguration?: GitHubConfigurationResponse) {
  return {
    [GitHubAuthFormFields.AllowedOrganizations]: {
      definition: {
        description: translate(
          'settings.authentication.github.form.allowedOrganizations.description',
        ),
        key: GitHubAuthFormFields.AllowedOrganizations,
        multiValues: true,
        name: translate('settings.authentication.github.form.allowedOrganizations.name'),
        secured: false,
      },
      required: false,
      value: gitHubConfiguration?.allowedOrganizations ?? [],
    },
    [GitHubAuthFormFields.ApiUrl]: {
      definition: {
        description: translate('settings.authentication.github.form.apiUrl.description'),
        key: GitHubAuthFormFields.ApiUrl,
        name: translate('settings.authentication.github.form.apiUrl.name'),
        secured: false,
      },
      required: true,
      value: gitHubConfiguration?.apiUrl ?? DEFAULT_API_URL,
    },
    [GitHubAuthFormFields.ApplicationId]: {
      definition: {
        description: translate('settings.authentication.github.form.applicationId.description'),
        key: GitHubAuthFormFields.ApplicationId,
        name: translate('settings.authentication.github.form.applicationId.name'),
        secured: false,
      },
      required: true,
      value: gitHubConfiguration?.applicationId ?? '',
    },
    [GitHubAuthFormFields.ClientId]: {
      definition: {
        description: translate('settings.authentication.github.form.clientId.description'),
        key: GitHubAuthFormFields.ClientId,
        name: translate('settings.authentication.github.form.clientId.name'),
        secured: true,
      },
      required: true,
      value: '',
    },
    [GitHubAuthFormFields.ClientSecret]: {
      definition: {
        description: translate('settings.authentication.github.form.clientSecret.description'),
        key: GitHubAuthFormFields.ClientSecret,
        name: translate('settings.authentication.github.form.clientSecret.name'),
        secured: true,
      },
      required: true,
      value: '',
    },
    [GitHubAuthFormFields.PrivateKey]: {
      definition: {
        description: translate('settings.authentication.github.form.privateKey.description'),
        key: GitHubAuthFormFields.PrivateKey,
        name: translate('settings.authentication.github.form.privateKey.name'),
        secured: true,
        type: SettingType.TEXT,
      },
      required: true,
      value: '',
    },
    [GitHubAuthFormFields.SynchronizeGroups]: {
      definition: {
        description: translate('settings.authentication.github.form.synchronizeGroups.description'),
        key: GitHubAuthFormFields.SynchronizeGroups,
        name: translate('settings.authentication.github.form.synchronizeGroups.name'),
        secured: false,
        type: SettingType.BOOLEAN,
      },
      required: false,
      value: gitHubConfiguration?.synchronizeGroups ?? false,
    },
    [GitHubAuthFormFields.WebUrl]: {
      definition: {
        description: translate('settings.authentication.github.form.webUrl.description'),
        key: GitHubAuthFormFields.WebUrl,
        name: translate('settings.authentication.github.form.webUrl.name'),
        secured: false,
      },
      required: true,
      value: gitHubConfiguration?.webUrl ?? DEFAULT_WEB_URL,
    },
  };
}
