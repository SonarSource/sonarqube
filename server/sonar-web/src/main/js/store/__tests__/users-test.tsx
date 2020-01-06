/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
/* eslint-disable sonarjs/no-duplicate-string */
import { mockCurrentUser, mockLoggedInUser, mockUser } from '../../helpers/testMocks';
import reducer, {
  getCurrentUser,
  getCurrentUserSetting,
  getUserByLogin,
  getUsersByLogins,
  receiveCurrentUser,
  setCurrentUserSettingAction,
  setHomePageAction,
  skipOnboardingAction,
  State
} from '../users';

describe('reducer and actions', () => {
  it('should allow to receive the current user', () => {
    const initialState: State = createState();

    const currentUser = mockCurrentUser();
    const newState = reducer(initialState, receiveCurrentUser(currentUser));
    expect(newState).toEqual(createState({ currentUser }));
  });

  it('should allow to skip the onboarding tutorials', () => {
    const currentUser = mockLoggedInUser({ showOnboardingTutorial: true });
    const initialState: State = createState({ currentUser });

    const newState = reducer(initialState, skipOnboardingAction());
    expect(newState).toEqual(
      createState({ currentUser: { ...currentUser, showOnboardingTutorial: false } })
    );
  });

  it('should allow to set the homepage', () => {
    const homepage: T.HomePage = { type: 'PROJECTS' };
    const currentUser = mockLoggedInUser({ homepage: undefined });
    const initialState: State = createState({ currentUser });

    const newState = reducer(initialState, setHomePageAction(homepage));
    expect(newState).toEqual(
      createState({ currentUser: { ...currentUser, homepage } as T.LoggedInUser })
    );
  });

  it('should allow to set a user setting', () => {
    const setting1: T.CurrentUserSetting = { key: 'notifications.optOut', value: '1' };
    const setting2: T.CurrentUserSetting = { key: 'notifications.readDate', value: '2' };
    const setting3: T.CurrentUserSetting = { key: 'notifications.optOut', value: '2' };
    const currentUser = mockLoggedInUser();
    const initialState: State = createState({ currentUser });

    const newState = reducer(initialState, setCurrentUserSettingAction(setting1));
    expect(newState).toEqual(
      createState({ currentUser: { ...currentUser, settings: [setting1] } as T.LoggedInUser })
    );

    const newerState = reducer(newState, setCurrentUserSettingAction(setting2));
    expect(newerState).toEqual(
      createState({
        currentUser: { ...currentUser, settings: [setting1, setting2] } as T.LoggedInUser
      })
    );

    const newestState = reducer(newerState, setCurrentUserSettingAction(setting3));
    expect(newestState).toEqual(
      createState({
        currentUser: { ...currentUser, settings: [setting3, setting2] } as T.LoggedInUser
      })
    );
  });
});

describe('getters', () => {
  const currentUser = mockLoggedInUser({ settings: [{ key: 'notifications.optOut', value: '1' }] });
  const jane = mockUser({ login: 'jane', name: 'Jane Doe' });
  const john = mockUser({ login: 'john' });
  const state = createState({ currentUser, usersByLogin: { jane, john } });

  test('getCurrentUser', () => {
    expect(getCurrentUser(state)).toBe(currentUser);
  });

  test('getCurrentUserSetting', () => {
    expect(getCurrentUserSetting(state, 'notifications.optOut')).toBe('1');
    expect(getCurrentUserSetting(state, 'notifications.readDate')).toBeUndefined();
  });

  test('getUserByLogin', () => {
    expect(getUserByLogin(state, 'jane')).toBe(jane);
    expect(getUserByLogin(state, 'steve')).toBeUndefined();
  });

  test('getUsersByLogins', () => {
    expect(getUsersByLogins(state, ['jane', 'john'])).toEqual([jane, john]);
  });
});

function createState(overrides: Partial<State> = {}): State {
  return { usersByLogin: {}, userLogins: [], currentUser: mockCurrentUser(), ...overrides };
}
