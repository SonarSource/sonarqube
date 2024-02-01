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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { addGlobalSuccessMessage } from 'design-system';
import { getValue, getValues, resetSettingValue, setSettingValue } from '../api/settings';
import { translate } from '../helpers/l10n';
import { ExtendedSettingDefinition } from '../types/settings';

type SettingValue = string | boolean | string[];

export function useGetValuesQuery(keys: string[]) {
  return useQuery(['settings', 'values', keys] as const, ({ queryKey: [_a, _b, keys] }) => {
    return getValues({ keys });
  });
}

export function useGetValueQuery(key: string, component?: string) {
  return useQuery(['settings', 'details', key] as const, ({ queryKey: [_a, _b, key] }) => {
    return getValue({ key, component }).then((v) => v ?? null);
  });
}

export function useResetSettingsMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ keys, component }: { keys: string[]; component?: string }) =>
      resetSettingValue({ keys: keys.join(','), component }),
    onSuccess: (_, { keys }) => {
      keys.forEach((key) => {
        queryClient.invalidateQueries(['settings', 'details', key]);
      });
      queryClient.invalidateQueries(['settings', 'values']);
    },
  });
}

export function useSaveValuesMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (
      values: {
        newValue?: SettingValue;
        definition: ExtendedSettingDefinition;
      }[],
    ) => {
      return Promise.all(
        values
          .filter((v) => v.newValue !== undefined)
          .map(async ({ newValue, definition }) => {
            try {
              if (isDefaultValue(newValue as string | boolean | string[], definition)) {
                await resetSettingValue({ keys: definition.key });
              } else {
                await setSettingValue(definition, newValue);
              }
              return { key: definition.key, success: true };
            } catch (error) {
              return { key: definition.key, success: false };
            }
          }),
      );
    },
    onSuccess: (data) => {
      if (data.length > 0) {
        data.forEach(({ key }) => {
          queryClient.invalidateQueries(['settings', 'details', key]);
        });
        queryClient.invalidateQueries(['settings', 'values']);
        addGlobalSuccessMessage(translate('settings.authentication.form.settings.save_success'));
      }
    },
  });
}

export function useSaveValueMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      newValue,
      definition,
      component,
    }: {
      newValue: SettingValue;
      definition: ExtendedSettingDefinition;
      component?: string;
    }) => {
      if (isDefaultValue(newValue, definition)) {
        return resetSettingValue({ keys: definition.key, component });
      }
      return setSettingValue(definition, newValue, component);
    },
    onSuccess: (_, { definition }) => {
      queryClient.invalidateQueries(['settings', 'details', definition.key]);
      queryClient.invalidateQueries(['settings', 'values']);
      addGlobalSuccessMessage(translate('settings.authentication.form.settings.save_success'));
    },
  });
}

function isDefaultValue(value: SettingValue, definition: ExtendedSettingDefinition) {
  const defaultValue = definition.defaultValue ?? '';
  if (definition.multiValues) {
    return defaultValue === (value as string[]).join(',');
  }

  return defaultValue === String(value);
}
