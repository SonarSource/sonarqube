/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getJSON, post } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function setUserSetting(data: T.CurrentUserSettingData) {
  return post('/api/user_settings/set', data)
    .catch(() => Promise.resolve()) // TODO Remove mock.
    .catch(throwGlobalError);
}

export function listUserSettings(): Promise<{ userSettings: T.CurrentUserSettingData[] }> {
  return getJSON('/api/user_settings/list')
    .catch(() => {
      // TODO Remove mock.
      return {
        userSettings: [
          { key: 'notificationsReadDate', value: '2018-12-01T12:07:19+0000' },
          { key: 'notificationsOptOut', value: 'false' }
        ]
      };
    })
    .catch(throwGlobalError);
}
