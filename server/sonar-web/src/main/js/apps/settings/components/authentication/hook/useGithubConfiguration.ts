/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  useToggleGithubProvisioningMutation,
} from '../../../../../queries/identity-provider';
import { useSaveValueMutation, useSaveValuesMutation } from '../../../../../queries/settings';
import { Feature } from '../../../../../types/features';
import { ExtendedSettingDefinition } from '../../../../../types/settings';
import useConfiguration from './useConfiguration';

export const GITHUB_ENABLED_FIELD = 'sonar.auth.github.enabled';
export const GITHUB_APP_ID_FIELD = 'sonar.auth.github.appId';
export const GITHUB_API_URL_FIELD = 'sonar.auth.github.apiUrl';
export const GITHUB_CLIENT_ID_FIELD = 'sonar.auth.github.clientId.secured';
export const GITHUB_JIT_FIELDS = ['sonar.auth.github.allowUsersToSignUp'];
export const OPTIONAL_FIELDS = [
  GITHUB_ENABLED_FIELD,
  ...GITHUB_JIT_FIELDS,
  'sonar.auth.github.organizations',
  'sonar.auth.github.groupsSync',
  'provisioning.github.project.visibility.enabled',
];

export interface SamlSettingValue {
  key: string;
  mandatory: boolean;
  isNotSet: boolean;
  value?: string;
  newValue?: string | boolean;
  definition: ExtendedSettingDefinition;
}

export default function useGithubConfiguration(definitions: ExtendedSettingDefinition[]) {
  const config = useConfiguration(definitions, OPTIONAL_FIELDS);
  const { values, isValueChange, setNewValue } = config;

  const hasGithubProvisioning = useContext(AvailableFeaturesContext).includes(
    Feature.GithubProvisioning,
  );
  const { data: githubProvisioningStatus } = useGithubProvisioningEnabledQuery();
  const toggleGithubProvisioning = useToggleGithubProvisioningMutation();
  const [newGithubProvisioningStatus, setNewGithubProvisioningStatus] = useState<boolean>();
  const hasGithubProvisioningTypeChange =
    newGithubProvisioningStatus !== undefined &&
    newGithubProvisioningStatus !== githubProvisioningStatus;
  const hasGithubProvisioningConfigChange =
    some(GITHUB_JIT_FIELDS, isValueChange) || hasGithubProvisioningTypeChange;

  const resetJitSetting = () => {
    GITHUB_JIT_FIELDS.forEach((s) => setNewValue(s));
  };

  const { mutate: saveSetting } = useSaveValueMutation();
  const { mutate: saveSettings } = useSaveValuesMutation();

  const enabled = values[GITHUB_ENABLED_FIELD]?.value === 'true';
  const appId = values[GITHUB_APP_ID_FIELD]?.value as string;
  const url = values[GITHUB_API_URL_FIELD]?.value;
  const clientIdIsNotSet = values[GITHUB_CLIENT_ID_FIELD]?.isNotSet;

  const changeProvisioning = async () => {
    if (hasGithubProvisioningTypeChange) {
      await toggleGithubProvisioning.mutateAsync(!!newGithubProvisioningStatus);
    }
    if (!newGithubProvisioningStatus || !githubProvisioningStatus) {
      saveGroup();
    }
  };

  const saveGroup = () => {
    const newValues = GITHUB_JIT_FIELDS.map((settingKey) => values[settingKey]);
    saveSettings(newValues);
  };

  const toggleEnable = () => {
    const value = values[GITHUB_ENABLED_FIELD];
    saveSetting({ newValue: !enabled, definition: value.definition });
  };

  const hasLegacyConfiguration = appId === undefined && !clientIdIsNotSet;

  return {
    ...config,
    url,
    enabled,
    appId,
    hasGithubProvisioning,
    githubProvisioningStatus,
    newGithubProvisioningStatus,
    setNewGithubProvisioningStatus,
    hasGithubProvisioningTypeChange,
    hasGithubProvisioningConfigChange,
    changeProvisioning,
    saveGroup,
    resetJitSetting,
    toggleEnable,
    hasLegacyConfiguration,
  };
}
