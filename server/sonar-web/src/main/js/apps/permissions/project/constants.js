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
export const PERMISSIONS_ORDER_FOR_PROJECT = ['user', 'codeviewer', 'issueadmin', 'admin', 'scan'];

export const PERMISSIONS_ORDER_FOR_VIEW = ['user', 'admin'];

export const PERMISSIONS_ORDER_FOR_DEV = ['user', 'admin'];

export const PERMISSIONS_ORDER_BY_QUALIFIER = {
  TRK: PERMISSIONS_ORDER_FOR_PROJECT,
  VW: PERMISSIONS_ORDER_FOR_VIEW,
  SVW: PERMISSIONS_ORDER_FOR_VIEW,
  APP: PERMISSIONS_ORDER_FOR_VIEW,
  DEV: PERMISSIONS_ORDER_FOR_DEV
};
