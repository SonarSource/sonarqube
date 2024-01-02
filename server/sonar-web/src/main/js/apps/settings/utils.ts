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
import { InputSizeKeys } from 'design-system';
import { sortBy } from 'lodash';
import { Path } from 'react-router-dom';
import { hasMessage, translate } from '../../helpers/l10n';
import { getGlobalSettingsUrl, getProjectSettingsUrl } from '../../helpers/urls';
import { AlmKeys } from '../../types/alm-settings';
import {
  DefinitionV2,
  ExtendedSettingDefinition,
  Setting,
  SettingDefinition,
  SettingType,
  SettingValue,
  SettingWithCategory,
} from '../../types/settings';
import { Component, Dict } from '../../types/types';

export const DEFAULT_CATEGORY = 'general';

export type DefaultSpecializedInputProps = DefaultInputProps & {
  className?: string;
  autoComplete?: string;
  isDefault: boolean;
  name: string;
  type?: string;
};

export interface DefaultInputProps {
  autoFocus?: boolean;
  isEditing?: boolean;
  isInvalid?: boolean;
  size?: InputSizeKeys;
  hasValueChanged?: boolean;
  onCancel?: () => void;
  onChange: (value: any) => void;
  onEditing?: () => void;
  onSave?: () => void;
  setting: Setting;
  value: any;
}

export function getPropertyName(definition: SettingDefinition | DefinitionV2) {
  const key = `property.${definition.key}.name`;
  if (hasMessage(key)) {
    return translate(key);
  }

  return definition.name ?? definition.key;
}

export function getPropertyDescription(definition: SettingDefinition | DefinitionV2) {
  const key = `property.${definition.key}.description`;
  return hasMessage(key) ? translate(key) : definition.description;
}

export function getCategoryName(category: string) {
  const key = `property.category.${category}`;
  return hasMessage(key) ? translate(key) : category;
}

export function getSubCategoryName(category: string, subCategory: string) {
  const key = `property.category.${category}.${subCategory}`;
  return hasMessage(key) ? translate(key) : getCategoryName(subCategory);
}

export function getSubCategoryDescription(category: string, subCategory: string) {
  const key = `property.category.${category}.${subCategory}.description`;
  return hasMessage(key) ? translate(key) : null;
}

export function getUniqueName(definition: SettingDefinition, index?: string) {
  const indexSuffix = index ? `[${index}]` : '';
  return `settings[${definition.key}]${indexSuffix}`;
}

export function getSettingValue(definition: SettingDefinition, settingValue?: SettingValue) {
  const { fieldValues, value, values } = settingValue || {};
  if (definition.type === SettingType.PROPERTY_SET) {
    return fieldValues;
  } else if (isCategoryDefinition(definition) && definition.multiValues) {
    return values;
  } else if (definition.type === SettingType.FORMATTED_TEXT) {
    return values ? values[0] : undefined;
  }
  return value;
}

export function combineDefinitionAndSettingValue(
  definition: ExtendedSettingDefinition,
  value?: SettingValue,
): SettingWithCategory {
  const hasValue = value !== undefined && value.inherited !== true;
  return {
    key: definition.key,
    hasValue,
    ...value,
    definition,
  };
}

export function getDefaultCategory(categories: string[]) {
  if (categories.includes(DEFAULT_CATEGORY)) {
    return DEFAULT_CATEGORY;
  }
  const sortedCategories = sortBy(categories, (category) =>
    getCategoryName(category).toLowerCase(),
  );
  return sortedCategories[0];
}

export function isEmptyValue(definition: SettingDefinition, value: any) {
  if (value == null) {
    return true;
  } else if (definition.type === 'BOOLEAN') {
    return false;
  }

  return value.length === 0;
}

export function isURLKind(definition: SettingDefinition) {
  return [
    'sonar.core.serverBaseURL',
    'sonar.auth.github.apiUrl',
    'sonar.auth.github.webUrl',
    'sonar.auth.gitlab.url',
    'sonar.lf.gravatarServerUrl',
    'sonar.lf.logoUrl',
    'sonar.auth.saml.loginUrl',
  ].includes(definition.key);
}

export function isSecuredDefinition(item: SettingDefinition | DefinitionV2): boolean {
  return 'secured' in item ? item.secured : item.key.endsWith('.secured');
}

export function isCategoryDefinition(item: SettingDefinition): item is ExtendedSettingDefinition {
  return Boolean((item as any).fields);
}

export function getEmptyValue(item: SettingDefinition | ExtendedSettingDefinition): any {
  if (isCategoryDefinition(item)) {
    if (item.type === SettingType.PROPERTY_SET) {
      const value: Dict<string> = {};
      item.fields.forEach((field) => (value[field.key] = getEmptyValue(field)));
      return [value];
    }

    if (item.multiValues) {
      return [getEmptyValue({ ...item, multiValues: false })];
    }
  }

  if (item.type === 'BOOLEAN' || item.type === 'SINGLE_SELECT_LIST') {
    return null;
  }
  return '';
}

export function isDefaultOrInherited(setting?: Pick<SettingValue, 'inherited'>) {
  return Boolean(setting && setting.inherited);
}

export function getDefaultValue(setting: Setting) {
  const { definition, parentFieldValues, parentValue, parentValues } = setting;

  if (definition.type === 'PASSWORD') {
    return translate('settings.default.password');
  }

  if (definition.type === 'BOOLEAN' && parentValue) {
    const isTrue = parentValue === 'true';
    return isTrue ? translate('settings.boolean.true') : translate('settings.boolean.false');
  }

  if (
    isCategoryDefinition(definition) &&
    definition.multiValues &&
    parentValues &&
    parentValues.length > 0
  ) {
    return parentValues.join(', ');
  }

  if (
    definition.type === SettingType.PROPERTY_SET &&
    parentFieldValues &&
    parentFieldValues.length > 0
  ) {
    return translate('settings.default.complex_value');
  }

  if (parentValue == null) {
    return isCategoryDefinition(definition) && definition.defaultValue
      ? definition.defaultValue
      : translate('settings.default.no_value');
  }

  return parentValue;
}

export function isRealSettingKey(key: string) {
  return ![
    'sonar.new_code_period',
    `sonar.almintegration.${AlmKeys.Azure}`,
    `sonar.almintegration.${AlmKeys.BitbucketServer}`,
    `sonar.almintegration.${AlmKeys.GitHub}`,
    `sonar.almintegration.${AlmKeys.GitLab}`,
  ].includes(key);
}

export function buildSettingLink(
  definition: ExtendedSettingDefinition,
  component?: Component,
): Partial<Path> {
  const { category, key } = definition;

  if (component !== undefined) {
    return {
      ...getProjectSettingsUrl(component.key, category),
      hash: `#${escape(key)}`,
    };
  }

  const query: Dict<string> = {};

  if (key.startsWith('sonar.auth.gitlab')) {
    query.tab = 'gitlab';
  } else if (key.startsWith('sonar.auth.github')) {
    query.tab = 'github';
  } else if (key.startsWith('sonar.auth.bitbucket')) {
    query.tab = 'bitbucket';
  } else if (key.startsWith('sonar.almintegration')) {
    query.alm = key.split('.').pop() || '';
  }

  return {
    ...getGlobalSettingsUrl(category, query),
    hash: `#${escape(key)}`,
  };
}

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
