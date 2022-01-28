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
import { mockCurrentUser, mockLoggedInUser, mockUser } from '../../helpers/testMocks';
import { isLoggedIn } from '../../helpers/users';
import { CurrentUserSetting, HomePage, LoggedInUser } from '../../types/types';
import reducer, {
  getCurrentUser,
  getCurrentUserSetting,
  getUserByLogin,
  getUsersByLogins,
  receiveCurrentUser,
  setCurrentUserSettingAction,
  setHomePageAction,
  setSonarlintAd,
  State
} from '../users';

describe('reducer and actions', () => {
  it('should allow to receive the current user', () => {
    const initialState: State = createState();

    const currentUser = mockCurrentUser();
    const newState = reducer(initialState, receiveCurrentUser(currentUser));
    expect(newState).toEqual(createState({ currentUser }));
  });

  it('should allow to set the homepage', () => {
    const homepage: HomePage = { type: 'PROJECTS' };
    const currentUser = mockLoggedInUser({ homepage: undefined });
    const initialState: State = createState({ currentUser });

    const newState = reducer(initialState, setHomePageAction(homepage));
    expect(newState).toEqual(
      createState({ currentUser: { ...currentUser, homepage } as LoggedInUser })
    );
  });

  it('should allow to set a user setting', () => {
    const setting1: CurrentUserSetting = { key: 'notifications.optOut', value: '1' };
    const setting2: CurrentUserSetting = { key: 'notifications.readDate', value: '2' };
    const setting3: CurrentUserSetting = { key: 'notifications.optOut', value: '2' };
    const currentUser = mockLoggedInUser();
    const initialState: State = createState({ currentUser });

    const newState = reducer(initialState, setCurrentUserSettingAction(setting1));
    expect(newState).toEqual(
      createState({ currentUser: { ...currentUser, settings: [setting1] } as LoggedInUser })
    );

    const newerState = reducer(newState, setCurrentUserSettingAction(setting2));
    expect(newerState).toEqual(
      createState({
        currentUser: { ...currentUser, settings: [setting1, setting2] } as LoggedInUser
      })
    );

    const newestState = reducer(newerState, setCurrentUserSettingAction(setting3));
    expect(newestState).toEqual(
      createState({
        currentUser: { ...currentUser, settings: [setting3, setting2] } as LoggedInUser
      })
    );
  });

  it('should allow to set the sonarLintAdSeen flag', () => {
    const currentUser = mockLoggedInUser();
    const initialState: State = createState({ currentUser });

    const newState = reducer(initialState, setSonarlintAd());
    expect(isLoggedIn(newState.currentUser) && newState.currentUser.sonarLintAdSeen).toBe(true);
  });
});

describe('getters', () => {
  const currentUser = mockLoggedInUser({ settings: [{ key: 'notifications.optOut', value: '1' }] });
  const jane = mockUser({ login: 'jane', name: 'Jane Doe' });
  const john = mockUser({ login: 'john' });
  const state = createState({ currentUser, usersByLogin: { jane, john } });

  it('getCurrentUser', () => {
    expect(getCurrentUser(state)).toBe(currentUser);
  });

  it('getCurrentUserSetting', () => {
    expect(getCurrentUserSetting(state, 'notifications.optOut')).toBe('1');
    expect(getCurrentUserSetting(state, 'notifications.readDate')).toBeUndefined();
  });

  it('getUserByLogin', () => {
    expect(getUserByLogin(state, 'jane')).toBe(jane);
    expect(getUserByLogin(state, 'steve')).toBeUndefined();
  });

  it('getUsersByLogins', () => {
    expect(getUsersByLogins(state, ['jane', 'john'])).toEqual([jane, john]);
  });
});

function createState(overrides: Partial<State> = {}): State {
  return { usersByLogin: {}, userLogins: [], currentUser: mockCurrentUser(), ...overrides };
}
