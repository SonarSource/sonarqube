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
import { getJSON, post, postJSON } from '../helpers/request';
import { TYPE_PROPERTY_SET } from '../apps/settings/constants';

export function getDefinitions(componentKey) {
  const url = '/api/settings/list_definitions';
  const data = {};
  if (componentKey) {
    data.component = componentKey;
  }
  return getJSON(url, data).then(r => r.definitions);
}

export function getValues(keys, componentKey) {
  const url = '/api/settings/values';
  const data = { keys };
  if (componentKey) {
    data.component = componentKey;
  }
  return postJSON(url, data).then(r => r.settings);
}

export function setSettingValue(definition, value, componentKey) {
  const url = '/api/settings/set';

  const { key } = definition;
  const data = { key };

  if (definition.multiValues) {
    data.values = value;
  } else if (definition.type === TYPE_PROPERTY_SET) {
    data.fieldValues = value
      .map(fields => omitBy(fields, value => value == null))
      .map(JSON.stringify);
  } else {
    data.value = value;
  }

  if (componentKey) {
    data.component = componentKey;
  }

  return post(url, data);
}

export function resetSettingValue(key, componentKey) {
  const url = '/api/settings/reset';
  const data = { keys: key };
  if (componentKey) {
    data.component = componentKey;
  }
  return post(url, data);
}

export function sendTestEmail(to, subject, message) {
  const url = '/api/emails/send';
  const data = { to, subject, message };
  return post(url, data);
}

export function checkSecretKey() {
  return getJSON('/api/settings/check_secret_key');
}

export function generateSecretKey() {
  return postJSON('/api/settings/generate_secret_key');
}

export function encryptValue(value) {
  return postJSON('/api/settings/encrypt', { value });
}

export function getServerId() {
  return getJSON('/api/server_id/show');
}

export function generateServerId(organization, ip) {
  return postJSON('/api/server_id/generate', { organization, ip });
}
