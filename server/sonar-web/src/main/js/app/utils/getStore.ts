/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Store } from 'redux';
import { setAppState } from '../../store/appState';
import rootReducer, { Store as State } from '../../store/rootReducer';
import { receiveCurrentUser } from '../../store/users';
import configureStore from '../../store/utils/configureStore';

let store: Store<State, any>;

const createStore = (currentUser?: T.CurrentUser, appState?: T.AppState) => {
  store = configureStore(rootReducer);
  if (currentUser) {
    store.dispatch(receiveCurrentUser(currentUser));
  }
  if (appState) {
    store.dispatch(setAppState(appState));
  }
  return store;
};

export default (currentUser?: T.CurrentUser, appState?: T.AppState) =>
  store ? store : createStore(currentUser, appState);
