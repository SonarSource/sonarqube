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
import { some } from 'lodash';
import { useContext, useState } from 'react';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import {
  useGithubProvisioningEnabledQuery,
  useGithubRolesMappingMutation,
  useToggleGithubProvisioningMutation,
} from '../../../../../queries/identity-provider/github';
import { useSaveValueMutation, useSaveValuesMutation } from '../../../../../queries/settings';
import { Feature } from '../../../../../types/features';
import { GitHubMapping } from '../../../../../types/provisioning';
import { ExtendedSettingDefinition } from '../../../../../types/settings';
import useConfiguration, { SettingValue } from './useConfiguration';

export const GITHUB_ENABLED_FIELD = 'sonar.auth.github.enabled';
export const GITHUB_APP_ID_FIELD = 'sonar.auth.github.appId';
export const GITHUB_API_URL_FIELD = 'sonar.auth.github.apiUrl';
export const GITHUB_CLIENT_ID_FIELD = 'sonar.auth.github.clientId.secured';
export const GITHUB_ALLOW_TO_SIGN_UP_FIELD = 'sonar.auth.github.allowUsersToSignUp';
export const GITHUB_ORGANIZATIONS_LIST = 'sonar.auth.github.organizations';
export const GITHUB_JIT_FIELDS = [GITHUB_ALLOW_TO_SIGN_UP_FIELD];
export const GITHUB_PROVISIONING_FIELDS = ['provisioning.github.project.visibility.enabled'];

export const GITHUB_ADDITIONAL_FIELDS = [...GITHUB_JIT_FIELDS, ...GITHUB_PROVISIONING_FIELDS];
export const OPTIONAL_FIELDS = [
  GITHUB_ENABLED_FIELD,
  ...GITHUB_ADDITIONAL_FIELDS,
  GITHUB_ORGANIZATIONS_LIST,
  'sonar.auth.github.groupsSync',
];

export interface SamlSettingValue {
  definition: ExtendedSettingDefinition;
  isNotSet: boolean;
  key: string;
  mandatory: boolean;
  newValue?: string | boolean;
  value?: string;
}

export const isOrganizationListEmpty = (values: Record<string, SettingValue>) =>
  values[GITHUB_ORGANIZATIONS_LIST]?.newValue
    ? (values[GITHUB_ORGANIZATIONS_LIST]?.newValue as string[]).length === 0
    : !values[GITHUB_ORGANIZATIONS_LIST]?.value ||
      values[GITHUB_ORGANIZATIONS_LIST]?.value.length === 0;
export const isAllowToSignUpEnabled = (values: Record<string, SettingValue>) =>
  values[GITHUB_ALLOW_TO_SIGN_UP_FIELD]?.value === 'true'
    ? values[GITHUB_ALLOW_TO_SIGN_UP_FIELD]?.newValue === undefined
    : values[GITHUB_ALLOW_TO_SIGN_UP_FIELD]?.newValue === true;

export default function useGithubConfiguration(definitions: ExtendedSettingDefinition[]) {
  const config = useConfiguration(definitions, OPTIONAL_FIELDS);
  const { values, isValueChange, setNewValue } = config;

  const hasGithubProvisioning = useContext(AvailableFeaturesContext).includes(
    Feature.GithubProvisioning,
  );
  const { data: githubProvisioningStatus } = useGithubProvisioningEnabledQuery();
  const toggleGithubProvisioning = useToggleGithubProvisioningMutation();
  const [newGithubProvisioningStatus, setNewGithubProvisioningStatus] = useState<boolean>();
  const [rolesMapping, setRolesMapping] = useState<GitHubMapping[] | null>(null);
  const hasGithubProvisioningTypeChange =
    newGithubProvisioningStatus !== undefined &&
    newGithubProvisioningStatus !== githubProvisioningStatus;
  const hasGithubProvisioningConfigChange =
    some(GITHUB_ADDITIONAL_FIELDS, isValueChange) ||
    hasGithubProvisioningTypeChange ||
    rolesMapping;

  const resetJitSetting = () => {
    GITHUB_ADDITIONAL_FIELDS.forEach((s) => setNewValue(s));
  };

  const { mutate: saveSetting } = useSaveValueMutation();
  const { mutate: saveSettings } = useSaveValuesMutation();
  const { mutateAsync: updateMapping } = useGithubRolesMappingMutation();

  const enabled = values[GITHUB_ENABLED_FIELD]?.value === 'true';
  const appId = values[GITHUB_APP_ID_FIELD]?.value as string;
  const url = values[GITHUB_API_URL_FIELD]?.value;
  const clientIdIsNotSet = values[GITHUB_CLIENT_ID_FIELD]?.isNotSet;

  const changeProvisioning = async () => {
    if (hasGithubProvisioningTypeChange) {
      await toggleGithubProvisioning.mutateAsync(!!newGithubProvisioningStatus);
    }
    applyAdditionalOptions();
  };

  const applyAdditionalOptions = () => {
    const newValues = GITHUB_ADDITIONAL_FIELDS.map((settingKey) => values[settingKey]);
    saveSettings(newValues);
    if ((newGithubProvisioningStatus ?? githubProvisioningStatus) && rolesMapping) {
      updateMapping(rolesMapping)
        .then(() => {
          setRolesMapping(null);
        })
        .catch(() => {});
    }
  };

  const toggleEnable = () => {
    const value = values[GITHUB_ENABLED_FIELD];
    saveSetting({ newValue: !enabled, definition: value.definition });
  };

  const hasLegacyConfiguration = appId === undefined && !clientIdIsNotSet;

  const setProvisioningType = (value: boolean | undefined) => {
    setRolesMapping(null);
    GITHUB_ADDITIONAL_FIELDS.forEach((field) => setNewValue(field, undefined));
    setNewGithubProvisioningStatus(value);
  };

  return {
    ...config,
    url,
    enabled,
    appId,
    hasGithubProvisioning,
    githubProvisioningStatus,
    newGithubProvisioningStatus,
    setProvisioningType,
    hasGithubProvisioningTypeChange,
    hasGithubProvisioningConfigChange,
    changeProvisioning,
    resetJitSetting,
    toggleEnable,
    rolesMapping,
    setRolesMapping,
    applyAdditionalOptions,
    hasLegacyConfiguration,
  };
}
