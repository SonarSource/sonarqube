/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { combineReducers } from 'redux';
import settingsApp, * as fromSettingsApp from '../apps/settings/store/rootReducer';
import { BranchLike } from '../types/branch-like';
import { CurrentUserSettingNames } from '../types/types';
import branches, * as fromBranches from './branches';
import globalMessages, * as fromGlobalMessages from './globalMessages';
import users, * as fromUsers from './users';

export type Store = {
  branches: fromBranches.State;
  globalMessages: fromGlobalMessages.State;
  users: fromUsers.State;

  // apps
  settingsApp: any;
};

export default combineReducers<Store>({
  branches,
  globalMessages,
  users,

  // apps
  settingsApp
});

export function getGlobalMessages(state: Store) {
  return fromGlobalMessages.getGlobalMessages(state.globalMessages);
}

export function getCurrentUserSetting(state: Store, key: CurrentUserSettingNames) {
  return fromUsers.getCurrentUserSetting(state.users, key);
}

export function getCurrentUser(state: Store) {
  return fromUsers.getCurrentUser(state.users);
}

export function getGlobalSettingValue(state: Store, key: string) {
  return fromSettingsApp.getValue(state.settingsApp, key);
}

export function getBranchStatusByBranchLike(
  state: Store,
  component: string,
  branchLike: BranchLike
) {
  return fromBranches.getBranchStatusByBranchLike(state.branches, component, branchLike);
}
