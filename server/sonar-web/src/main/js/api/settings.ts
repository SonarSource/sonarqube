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
import { omitBy } from 'lodash';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { isCategoryDefinition } from '../apps/settings/utils';
import { post, postJSON, RequestData } from '../helpers/request';
import {
  ExtendedSettingDefinition,
  SettingDefinition,
  SettingType,
  SettingValue,
  SettingValueResponse,
} from '../types/settings';

export function getDefinitions(component?: string): Promise<ExtendedSettingDefinition[]> {
  return getJSON('/api/settings/list_definitions', { component }).then(
    (r) => r.definitions,
    throwGlobalError,
  );
}

export function getValue(
  data: { key: string; component?: string } & BranchParameters,
): Promise<SettingValue> {
  return getValues({ keys: [data.key], component: data.component }).then(([result]) => result);
}

export function getValues(
  data: { keys: string[]; component?: string } & BranchParameters,
): Promise<SettingValue[]> {
  return getJSON('/api/settings/values', {
    keys: data.keys.join(','),
    component: data.component,
  }).then((r: SettingValueResponse) => [
    ...r.settings,
    ...r.setSecuredSettings.map((key) => ({ key })),
  ]);
}

export function getAllValues(
  data: { component?: string } & BranchParameters = {},
): Promise<SettingValue[]> {
  return getJSON('/api/settings/values', data).then((r: SettingValueResponse) => [
    ...r.settings,
    ...r.setSecuredSettings.map((key) => ({ key })),
  ]);
}

export function setSettingValue(
  definition: SettingDefinition,
  value: any,
  component?: string,
): Promise<void> {
  const { key } = definition;
  const data: RequestData = { key, component };

  if (definition.type === SettingType.PROPERTY_SET) {
    data.fieldValues = value
      .map((fields: any) => omitBy(fields, (value) => value == null))
      .map(JSON.stringify);
  } else if (isCategoryDefinition(definition) && definition.multiValues) {
    data.values = value;
  } else {
    data.value = value;
  }

  return post('/api/settings/set', data);
}

export function setSimpleSettingValue(
  data: { component?: string; value?: string; values?: string[]; key: string } & BranchParameters,
): Promise<void | Response> {
  return post('/api/settings/set', data).catch(throwGlobalError);
}

export function resetSettingValue(
  data: { keys: string; component?: string } & BranchParameters,
): Promise<void> {
  return post('/api/settings/reset', data);
}

export function sendTestEmail(to: string, subject: string, message: string): Promise<void> {
  return post('/api/emails/send', { to, subject, message });
}

export function checkSecretKey(): Promise<{ secretKeyAvailable: boolean }> {
  return getJSON('/api/settings/check_secret_key').catch(throwGlobalError);
}

export function generateSecretKey(): Promise<{ secretKey: string }> {
  return getJSON('/api/settings/generate_secret_key').catch(throwGlobalError);
}

export function encryptValue(value: string): Promise<{ encryptedValue: string }> {
  return postJSON('/api/settings/encrypt', { value }).catch(throwGlobalError);
}

export function getLoginMessage(): Promise<{ message: string }> {
  return getJSON('/api/settings/login_message').catch(throwGlobalError);
}
