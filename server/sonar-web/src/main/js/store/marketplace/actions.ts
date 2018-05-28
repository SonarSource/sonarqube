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
import { getEditionStatus } from '../../api/marketplace';
import { getPendingPlugins, PluginPendingResult } from '../../api/plugins';

interface SetPendingPluginsAction {
  type: 'SET_PENDING_PLUGINS';
  pending: PluginPendingResult;
}

interface SetCurrentEditionAction {
  type: 'SET_CURRENT_EDITION';
  currentEdition?: string;
}

export type Action = SetCurrentEditionAction | SetPendingPluginsAction;

export function setPendingPlugins(pending: PluginPendingResult): SetPendingPluginsAction {
  return { type: 'SET_PENDING_PLUGINS', pending };
}

export const setCurrentEdition = (currentEdition?: string) => (dispatch: Dispatch<Action>) => {
  dispatch({ type: 'SET_CURRENT_EDITION', currentEdition });
};

export const fetchCurrentEdition = () => (dispatch: Dispatch<Action>) => {
  getEditionStatus().then(
    editionStatus => dispatch(setCurrentEdition(editionStatus.currentEditionKey)),
    () => {}
  );
};

export const fetchPendingPlugins = () => (dispatch: Dispatch<Action>) => {
  getPendingPlugins().then(
    pending => {
      dispatch(setPendingPlugins(pending));
    },
    () => {}
  );
};
