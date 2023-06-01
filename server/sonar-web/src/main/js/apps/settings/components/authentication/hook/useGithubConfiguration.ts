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
import { isEmpty, some } from 'lodash';
import { useCallback, useContext, useState } from 'react';
import { resetSettingValue, setSettingValue } from '../../../../../api/settings';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { Feature } from '../../../../../types/features';
import { ExtendedSettingDefinition } from '../../../../../types/settings';
import {
  useCheckGitHubConfigQuery,
  useGithubStatusQuery,
  useToggleGithubProvisioningMutation,
} from '../queries/identity-provider';
import useConfiguration from './useConfiguration';

export const GITHUB_ENABLED_FIELD = 'sonar.auth.github.enabled';
export const GITHUB_APP_ID_FIELD = 'sonar.auth.github.appId';
export const GITHUB_API_URL_FIELD = 'sonar.auth.github.apiUrl';
export const GITHUB_CLIENT_ID_FIELD = 'sonar.auth.github.clientId.secured';
export const GITHUB_JIT_FIELDS = [
  'sonar.auth.github.allowUsersToSignUp',
  'sonar.auth.github.groupsSync',
];
export const OPTIONAL_FIELDS = [
  GITHUB_ENABLED_FIELD,
  ...GITHUB_JIT_FIELDS,
  'sonar.auth.github.organizations',
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
  const { values, isValueChange, setNewValue, reload: reloadConfig } = config;

  const hasGithubProvisioning = useContext(AvailableFeaturesContext).includes(
    Feature.GithubProvisioning
  );
  const { data: githubProvisioningStatus } = useGithubStatusQuery();
  const toggleGithubProvisioning = useToggleGithubProvisioningMutation();
  const [newGithubProvisioningStatus, setNewGithubProvisioningStatus] = useState<boolean>();
  const hasGithubProvisioningConfigChange =
    some(GITHUB_JIT_FIELDS, isValueChange) ||
    (newGithubProvisioningStatus !== undefined &&
      newGithubProvisioningStatus !== githubProvisioningStatus);

  const resetJitSetting = () => {
    GITHUB_JIT_FIELDS.forEach((s) => setNewValue(s));
  };

  const enabled = values[GITHUB_ENABLED_FIELD]?.value === 'true';
  const { refetch } = useCheckGitHubConfigQuery(enabled);
  const appId = values[GITHUB_APP_ID_FIELD]?.value as string;
  const url = values[GITHUB_API_URL_FIELD]?.value;
  const clientIdIsNotSet = values[GITHUB_CLIENT_ID_FIELD]?.isNotSet;

  const reload = useCallback(async () => {
    await reloadConfig();
    // Temporary solution that will be solved once we migrate to react-query
    refetch();
  }, [reloadConfig]);

  const changeProvisioning = async () => {
    if (newGithubProvisioningStatus !== githubProvisioningStatus) {
      await toggleGithubProvisioning.mutateAsync(!!newGithubProvisioningStatus);
    }
    await saveGroup();
  };

  const saveGroup = async () => {
    await Promise.all(
      GITHUB_JIT_FIELDS.map(async (settingKey) => {
        const value = values[settingKey];
        if (value.newValue !== undefined) {
          if (isEmpty(value.newValue) && typeof value.newValue !== 'boolean') {
            await resetSettingValue({ keys: value.definition.key });
          } else {
            await setSettingValue(value.definition, value.newValue);
          }
        }
      })
    );
    await reload();
  };

  const toggleEnable = async () => {
    const value = values[GITHUB_ENABLED_FIELD];
    await setSettingValue(value.definition, !enabled);
    await reload();
  };

  const hasLegacyConfiguration = appId === undefined && !clientIdIsNotSet;

  return {
    ...config,
    reload,
    url,
    enabled,
    appId,
    hasGithubProvisioning,
    githubProvisioningStatus,
    newGithubProvisioningStatus,
    setNewGithubProvisioningStatus,
    hasGithubProvisioningConfigChange,
    changeProvisioning,
    saveGroup,
    resetJitSetting,
    toggleEnable,
    hasLegacyConfiguration,
  };
}
