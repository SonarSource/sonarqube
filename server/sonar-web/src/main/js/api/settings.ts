/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { getJSON, RequestData, post, postJSON } from '../helpers/request';
import { TYPE_PROPERTY_SET } from '../apps/settings/constants';

export function getDefinitions(componentKey: string): Promise<any> {
  const data: RequestData = {};
  if (componentKey) {
    data.component = componentKey;
  }
  return getJSON('/api/settings/list_definitions', data).then(r => r.definitions);
}

export function getValues(keys: string, componentKey: string): Promise<any> {
  const data: RequestData = { keys };
  if (componentKey) {
    data.component = componentKey;
  }
  return getJSON('/api/settings/values', data).then(r => r.settings);
}

export function setSettingValue(definition: any, value: any, componentKey: string): Promise<void> {
  const { key } = definition;
  const data: RequestData = { key };

  if (definition.multiValues) {
    data.values = value;
  } else if (definition.type === TYPE_PROPERTY_SET) {
    data.fieldValues = value
      .map((fields: any) => omitBy(fields, value => value == null))
      .map(JSON.stringify);
  } else {
    data.value = value;
  }

  if (componentKey) {
    data.component = componentKey;
  }

  return post('/api/settings/set', data);
}

export function resetSettingValue(key: string, componentKey: string): Promise<void> {
  const data: RequestData = { keys: key };
  if (componentKey) {
    data.component = componentKey;
  }
  return post('/api/settings/reset', data);
}

export function sendTestEmail(to: string, subject: string, message: string): Promise<void> {
  return post('/api/emails/send', { to, subject, message });
}

export function checkSecretKey(): Promise<any> {
  return getJSON('/api/settings/check_secret_key');
}

export function generateSecretKey(): Promise<any> {
  return postJSON('/api/settings/generate_secret_key');
}

export function encryptValue(value: string): Promise<any> {
  return postJSON('/api/settings/encrypt', { value });
}

export function getServerId(): Promise<any> {
  return getJSON('/api/server_id/show');
}

export function generateServerId(organization: string, ip: string): Promise<any> {
  return postJSON('/api/server_id/generate', { organization, ip });
}
