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
export const RECEIVE_HOLDERS_SUCCESS = 'permissions/RECEIVE_HOLDERS_SUCCESS';
export const GRANT_PERMISSION_TO_GROUP = 'permissions/GRANT_PERMISSION_TO_GROUP';
export const REVOKE_PERMISSION_FROM_GROUP = 'permissions/REVOKE_PERMISSION_FROM_GROUP';
export const GRANT_PERMISSION_TO_USER = 'permissions/GRANT_PERMISSION_TO_USER';
export const REVOKE_PERMISSION_TO_USER = 'permissions/REVOKE_PERMISSION_TO_USER';
export const UPDATE_FILTER = 'permissions/UPDATE_FILTER';
export const UPDATE_QUERY = 'permissions/UPDATE_QUERY';
export const SELECT_PERMISSION = 'permissions/SELECT_PERMISSION';
export const REQUEST_HOLDERS = 'permissions/REQUEST_HOLDERS';
export const ERROR = 'permissions/ERROR';

export const raiseError = message => ({
  type: ERROR,
  message
});
