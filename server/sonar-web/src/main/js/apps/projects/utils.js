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
// @flow
import { translate } from '../../helpers/l10n';

const LOCALSTORAGE_KEY = 'sonarqube.projects.default';
const LOCALSTORAGE_FAVORITE = 'favorite';
const LOCALSTORAGE_ALL = 'all';

export const isFavoriteSet = (): boolean => {
  const setting = window.localStorage.getItem(LOCALSTORAGE_KEY);
  return setting === LOCALSTORAGE_FAVORITE;
};

export const isAllSet = (): boolean => {
  const setting = window.localStorage.getItem(LOCALSTORAGE_KEY);
  return setting === LOCALSTORAGE_ALL;
};

const save = (value: string) => {
  try {
    window.localStorage.setItem(LOCALSTORAGE_KEY, value);
  } catch (e) {
    // usually that means the storage is full
    // just do nothing in this case
  }
};

export const saveAll = () => save(LOCALSTORAGE_ALL);

export const saveFavorite = () => save(LOCALSTORAGE_FAVORITE);

export const VISUALIZATIONS = [
  'quality',
  'bugs',
  'vulnerabilities',
  'code_smells',
  'uncovered_lines',
  'duplicated_blocks'
];

export const localizeSorting = (sort?: string) => {
  return translate('projects.sort', sort || 'name');
};
