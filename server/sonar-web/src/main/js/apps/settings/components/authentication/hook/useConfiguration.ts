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
import { getValues, resetSettingValue } from '../../../../../api/settings';
import { ExtendedSettingDefinition } from '../../../../../types/settings';
import { Dict } from '../../../../../types/types';

export interface SettingValue {
  key: string;
  mandatory: boolean;
  isNotSet: boolean;
  value?: string;
  newValue?: string | boolean;
  definition: ExtendedSettingDefinition;
}

export default function useConfiguration(
  definitions: ExtendedSettingDefinition[],
  optionalFields: string[]
) {
  const [loading, setLoading] = React.useState(true);
  const [values, setValues] = React.useState<Dict<SettingValue>>({});

  const reload = React.useCallback(async () => {
    const keys = definitions.map((definition) => definition.key);

    setLoading(true);

    try {
      const values = await getValues({
        keys,
      });

      setValues(
        keyBy(
          definitions.map((definition) => ({
            key: definition.key,
            value: values.find((v) => v.key === definition.key)?.value,
            mandatory: !optionalFields.includes(definition.key),
            isNotSet: values.find((v) => v.key === definition.key) === undefined,
            definition,
          })),
          'key'
        )
      );
    } finally {
      setLoading(false);
    }
  }, [...definitions]);

  React.useEffect(() => {
    (async () => {
      await reload();
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

  const deleteConfiguration = React.useCallback(async () => {
    await resetSettingValue({ keys: Object.keys(values).join(',') });
    await reload();
  }, [reload, values]);

  const isValueChange = React.useCallback(
    (setting: string) => {
      const value = values[setting];
      return value && value.newValue !== undefined && (value.value ?? '') !== value.newValue;
    },
    [values]
  );

  return {
    values,
    reload,
    setNewValue,
    canBeSave,
    loading,
    hasConfiguration,
    isValueChange,
    deleteConfiguration,
  };
}
