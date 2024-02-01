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
import { UseMutationResult } from '@tanstack/react-query';
import { every, isEmpty, keyBy, update } from 'lodash';
import { useCallback, useEffect, useState } from 'react';
import { useGetValuesQuery, useResetSettingsMutation } from '../../../../../queries/settings';
import { ExtendedSettingDefinition, SettingType } from '../../../../../types/settings';
import { Dict } from '../../../../../types/types';

export type SettingValue =
  | {
      key: string;
      multiValues: false;
      mandatory: boolean;
      isNotSet: boolean;
      value?: string;
      newValue?: string | boolean;
      definition: ExtendedSettingDefinition;
    }
  | {
      key: string;
      multiValues: true;
      mandatory: boolean;
      isNotSet: boolean;
      value?: string[];
      newValue?: string[];
      definition: ExtendedSettingDefinition;
    };

export default function useConfiguration(
  definitions: ExtendedSettingDefinition[],
  optionalFields: string[],
) {
  const keys = definitions.map((definition) => definition.key);
  const [values, setValues] = useState<Dict<SettingValue>>({});

  const { isLoading, data } = useGetValuesQuery(keys);

  useEffect(() => {
    if (data !== undefined) {
      setValues(
        keyBy(
          definitions.map((definition) => {
            const value = data.find((v) => v.key === definition.key);
            const multiValues = definition.multiValues ?? false;
            if (multiValues) {
              return {
                key: definition.key,
                multiValues,
                value: value?.values,
                mandatory: !optionalFields.includes(definition.key),
                isNotSet: value === undefined,
                definition,
              };
            }
            return {
              key: definition.key,
              multiValues,
              value: value?.value,
              mandatory: !optionalFields.includes(definition.key),
              isNotSet: value === undefined,
              definition,
            };
          }),
          'key',
        ),
      );
    }
  }, [data, definitions]);

  const setNewValue = (key: string, newValue?: string | boolean | string[]) => {
    const value = values[key];
    setValues((values) => {
      const newValues = { ...values };
      if (value.multiValues) {
        newValues[key] = { ...value, newValue: newValue as string[] | undefined };
      } else {
        newValues[key] = { ...value, newValue: newValue as string | boolean | undefined };
      }
      return newValues;
    });
  };

  const canBeSave = every(
    Object.values(values).filter((v) => v.mandatory),
    (v) =>
      (v.newValue !== undefined && !isEmpty(v.newValue)) ||
      (!v.isNotSet && v.newValue === undefined),
  );

  const hasConfiguration = every(
    Object.values(values).filter((v) => v.mandatory),
    (v) => !v.isNotSet,
  );

  const deleteMutation = update(
    useResetSettingsMutation(),
    'mutate',
    (mutate) => () => mutate({ keys: Object.keys(values) }),
  ) as Omit<UseMutationResult<void, unknown, void, unknown>, 'mutateAsync'>;

  const isValueChange = useCallback(
    (setting: string) => {
      const value = values[setting];
      if (!value) {
        return false;
      }
      if (value.definition.type === SettingType.BOOLEAN) {
        return value.newValue !== undefined && (value.value === 'true') !== value.newValue;
      }
      return value.newValue !== undefined && (value.value ?? '') !== value.newValue;
    },
    [values],
  );

  return {
    values,
    setNewValue,
    canBeSave,
    isLoading,
    hasConfiguration,
    isValueChange,
    deleteMutation,
  };
}
