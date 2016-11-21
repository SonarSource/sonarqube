/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 /* @flow */
export const STATUSES = {
  ALL: '__ALL__',
  ALL_EXCEPT_PENDING: '__ALL_EXCEPT_PENDING__',
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS',
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED',
  CANCELED: 'CANCELED'
};

export const ALL_TYPES = 'ALL_TYPES';

export const CURRENTS = {
  ALL: '__ALL__',
  ONLY_CURRENTS: 'CURRENTS'
};

export const DATE = {
  ANY: 'ANY',
  TODAY: 'TODAY',
  CUSTOM: 'CUSTOM'
};

export const DEFAULT_FILTERS = {
  status: STATUSES.ALL_EXCEPT_PENDING,
  taskType: ALL_TYPES,
  currents: CURRENTS.ALL,
  minSubmittedAt: '',
  maxExecutedAt: '',
  query: ''
};

export const DATE_FORMAT = 'YYYY-MM-DD';

export const DEBOUNCE_DELAY = 250;
