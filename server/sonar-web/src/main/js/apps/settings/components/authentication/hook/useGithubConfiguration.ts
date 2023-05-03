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
import { useCallback, useContext, useEffect, useState } from 'react';
import { fetchIsGithubProvisioningEnabled } from '../../../../../api/settings';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { Feature } from '../../../../../types/features';
import { ExtendedSettingDefinition } from '../../../../../types/settings';
import useConfiguration from './useConfiguration';

export const GITHUB_ENABLED_FIELD = 'sonar.auth.github.enabled';
export const GITHUB_APP_ID_FIELD = 'sonar.auth.github.appId';
export const GITHUB_API_URL_FIELD = 'sonar.auth.github.apiUrl';
export const GITHUB_JIT_FIELDS = [
  'sonar.auth.github.organizations',
  'sonar.auth.github.allowUsersToSignUp',
  'sonar.auth.github.groupsSync',
  'sonar.auth.github.organizations',
];
export const OPTIONAL_FIELDS = [GITHUB_ENABLED_FIELD, ...GITHUB_JIT_FIELDS];

export interface SamlSettingValue {
  key: string;
  mandatory: boolean;
  isNotSet: boolean;
  value?: string;
  newValue?: string | boolean;
  definition: ExtendedSettingDefinition;
}

export default function useGithubConfiguration(
  definitions: ExtendedSettingDefinition[],
  onReload: () => void
) {
  const config = useConfiguration(definitions, OPTIONAL_FIELDS);
  const { values, isValueChange, setNewValue, reload: reloadConfig } = config;
  const hasGithubProvisioning = useContext(AvailableFeaturesContext).includes(
    Feature.GithubProvisioning
  );
  const [githubProvisioningStatus, setGithubProvisioningStatus] = useState(false);
  const [newGithubProvisioningStatus, setNewGithubProvisioningStatus] = useState<boolean>();
  const hasGithubProvisioningConfigChange =
    some(GITHUB_JIT_FIELDS, isValueChange) ||
    (newGithubProvisioningStatus !== undefined &&
      newGithubProvisioningStatus !== githubProvisioningStatus);

  const resetJitSetting = () => {
    GITHUB_JIT_FIELDS.forEach((s) => setNewValue(s));
  };

  useEffect(() => {
    (async () => {
      if (hasGithubProvisioning) {
        setGithubProvisioningStatus(await fetchIsGithubProvisioningEnabled());
      }
    })();
  }, [hasGithubProvisioning]);

  const enabled = values[GITHUB_ENABLED_FIELD]?.value === 'true';
  const appId = values[GITHUB_APP_ID_FIELD]?.value;
  const url = values[GITHUB_API_URL_FIELD]?.value;

  const reload = useCallback(async () => {
    await reloadConfig();
    setGithubProvisioningStatus(await fetchIsGithubProvisioningEnabled());
    onReload();
  }, [reloadConfig, onReload]);

  return {
    ...config,
    reload,
    url,
    enabled,
    appId,
    hasGithubProvisioning,
    setGithubProvisioningStatus,
    githubProvisioningStatus,
    newGithubProvisioningStatus,
    setNewGithubProvisioningStatus,
    hasGithubProvisioningConfigChange,
    resetJitSetting,
  };
}
