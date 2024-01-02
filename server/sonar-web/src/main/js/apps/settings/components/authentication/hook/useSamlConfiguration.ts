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
import React from 'react';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { useScimStatusQuery } from '../../../../../queries/identity-provider/scim';
import { Feature } from '../../../../../types/features';
import { ExtendedSettingDefinition } from '../../../../../types/settings';
import useConfiguration from './useConfiguration';

export const SAML_ENABLED_FIELD = 'sonar.auth.saml.enabled';
export const SAML_GROUP_NAME = 'sonar.auth.saml.group.name';
export const SAML_SCIM_DEPRECATED = 'sonar.scim.enabled';
const SAML_PROVIDER_NAME = 'sonar.auth.saml.providerName';
const SAML_LOGIN_URL = 'sonar.auth.saml.loginUrl';

const OPTIONAL_FIELDS = [
  'sonar.auth.saml.sp.certificate.secured',
  'sonar.auth.saml.sp.privateKey.secured',
  'sonar.auth.saml.signature.enabled',
  'sonar.auth.saml.user.email',
  'sonar.auth.saml.group.name',
  SAML_SCIM_DEPRECATED,
];

export default function useSamlConfiguration(definitions: ExtendedSettingDefinition[]) {
  const [newScimStatus, setNewScimStatus] = React.useState<boolean>();
  const hasScim = React.useContext(AvailableFeaturesContext).includes(Feature.Scim);
  const config = useConfiguration(definitions, OPTIONAL_FIELDS);
  const { values, setNewValue, isValueChange } = config;

  const { data: scimStatus } = useScimStatusQuery();

  const name = values[SAML_PROVIDER_NAME]?.value;
  const url = values[SAML_LOGIN_URL]?.value;
  const samlEnabled = values[SAML_ENABLED_FIELD]?.value === 'true';
  const groupValue = values[SAML_GROUP_NAME];

  const setNewGroupSetting = (value?: string) => {
    setNewValue(SAML_GROUP_NAME, value);
  };

  const hasScimTypeChange = newScimStatus !== undefined && newScimStatus !== scimStatus;
  const hasScimConfigChange = isValueChange(SAML_GROUP_NAME) || hasScimTypeChange;

  return {
    ...config,
    hasScim,
    scimStatus,
    samlEnabled,
    name,
    url,
    groupValue,
    values,
    setNewValue,
    hasScimTypeChange,
    hasScimConfigChange,
    newScimStatus,
    setNewScimStatus,
    setNewGroupSetting,
  };
}
