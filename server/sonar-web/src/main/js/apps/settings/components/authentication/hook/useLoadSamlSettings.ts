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
import { every, isEmpty, keyBy } from 'lodash';
import React from 'react';
import { fetchIsScimEnabled, getValues } from '../../../../../api/settings';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { Feature } from '../../../../../types/features';
import { ExtendedSettingDefinition } from '../../../../../types/settings';
import { Dict } from '../../../../../types/types';

const SAML = 'saml';

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

export interface SamlSettingValue {
  key: string;
  mandatory: boolean;
  isNotSet: boolean;
  value?: string;
  newValue?: string | boolean;
  definition: ExtendedSettingDefinition;
}

export default function useSamlConfiguration(definitions: ExtendedSettingDefinition[]) {
  const [loading, setLoading] = React.useState(true);
  const [scimStatus, setScimStatus] = React.useState<boolean>(false);
  const [values, setValues] = React.useState<Dict<SamlSettingValue>>({});
  const [newScimStatus, setNewScimStatus] = React.useState<boolean>();
  const hasScim = React.useContext(AvailableFeaturesContext).includes(Feature.Scim);

  const onReload = React.useCallback(async () => {
    const samlDefinition = definitions.filter((def) => def.subCategory === SAML);
    const keys = samlDefinition.map((definition) => definition.key);

    setLoading(true);

    try {
      const values = await getValues({
        keys,
      });

      setValues(
        keyBy(
          samlDefinition.map((definition) => ({
            key: definition.key,
            value: values.find((v) => v.key === definition.key)?.value,
            mandatory: !OPTIONAL_FIELDS.includes(definition.key),
            isNotSet: values.find((v) => v.key === definition.key) === undefined,
            definition,
          })),
          'key'
        )
      );

      if (hasScim) {
        setScimStatus(await fetchIsScimEnabled());
      }
    } finally {
      setLoading(false);
    }
  }, [...definitions]);

  React.useEffect(() => {
    // eslint-disable-next-line @typescript-eslint/no-floating-promises
    (async () => {
      await onReload();
    })();
  }, [...definitions]);

  const setNewValue = (key: string, newValue?: string | boolean) => {
    const newValues = {
      ...values,
      [key]: {
        key,
        newValue,
        mandatory: values[key]?.mandatory,
        isNotSet: values[key]?.isNotSet,
        value: values[key]?.value,
        definition: values[key]?.definition,
      },
    };
    setValues(newValues);
  };

  const canBeSave = every(
    Object.values(values).filter((v) => v.mandatory),
    (v) =>
      (v.newValue !== undefined && !isEmpty(v.newValue)) ||
      (!v.isNotSet && v.newValue === undefined)
  );

  const hasConfiguration = every(
    Object.values(values).filter((v) => v.mandatory),
    (v) => !v.isNotSet
  );

  const name = values[SAML_PROVIDER_NAME]?.value;
  const url = values[SAML_LOGIN_URL]?.value;
  const samlEnabled = values[SAML_ENABLED_FIELD]?.value === 'true';
  const groupValue = values[SAML_GROUP_NAME];

  const setNewGroupSetting = (value?: string) => {
    setNewValue(SAML_GROUP_NAME, value);
  };

  const hasScimConfigChange =
    newScimStatus !== undefined &&
    groupValue &&
    (newScimStatus !== scimStatus ||
      (groupValue.newValue !== undefined && (groupValue.value ?? '') !== groupValue.newValue));

  return {
    hasScim,
    scimStatus,
    loading,
    samlEnabled,
    name,
    url,
    groupValue,
    hasConfiguration,
    canBeSave,
    values,
    setNewValue,
    onReload,
    hasScimConfigChange,
    newScimStatus,
    setNewScimStatus,
    setNewGroupSetting,
  };
}
