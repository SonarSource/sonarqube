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
import { Dispatch } from 'react-redux';
import { getEditionsForVersion, getEditionsForLastVersion } from './utils';
import { Edition, EditionStatus, getEditionStatus, getEditionsList } from '../../api/marketplace';

interface LoadEditionsAction {
  type: 'LOAD_EDITIONS';
  loading: boolean;
}

interface SetEditionsAction {
  type: 'SET_EDITIONS';
  editions: Edition[];
  readOnly: boolean;
}

interface SetEditionStatusAction {
  type: 'SET_EDITION_STATUS';
  status: EditionStatus;
}

export type Action = LoadEditionsAction | SetEditionsAction | SetEditionStatusAction;

export function loadEditions(loading = true): LoadEditionsAction {
  return { type: 'LOAD_EDITIONS', loading };
}

export function setEditions(editions: Edition[], readOnly?: boolean): SetEditionsAction {
  return { type: 'SET_EDITIONS', editions, readOnly: !!readOnly };
}

let editionTimer: number | undefined;
export const setEditionStatus = (status: EditionStatus) => (dispatch: Dispatch<Action>) => {
  dispatch({ type: 'SET_EDITION_STATUS', status });
  if (editionTimer) {
    window.clearTimeout(editionTimer);
    editionTimer = undefined;
  }
  if (status.installationStatus === 'AUTOMATIC_IN_PROGRESS') {
    editionTimer = window.setTimeout(() => {
      getEditionStatus().then(status => setEditionStatus(status)(dispatch), () => {});
      editionTimer = undefined;
    }, 2000);
  }
};

export const fetchEditions = (url: string, version: string) => (dispatch: Dispatch<Action>) => {
  dispatch(loadEditions(true));
  getEditionsList(url).then(
    editionsPerVersion => {
      const editions = getEditionsForVersion(editionsPerVersion, version);
      if (editions) {
        dispatch(setEditions(editions));
      } else {
        dispatch(setEditions(getEditionsForLastVersion(editionsPerVersion), true));
      }
    },
    () => dispatch(loadEditions(false))
  );
};
