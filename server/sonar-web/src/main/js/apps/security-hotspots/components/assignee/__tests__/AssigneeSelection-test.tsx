/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import { searchUsers } from '../../../../../api/users';
import { KeyboardKeys } from '../../../../../helpers/keycodes';
import { mockLoggedInUser, mockUser } from '../../../../../helpers/testMocks';
import { mockEvent, waitAndUpdate } from '../../../../../helpers/testUtils';
import { UserActive } from '../../../../../types/users';
import AssigneeSelection from '../AssigneeSelection';

jest.mock('../../../../../api/users', () => ({
  searchUsers: jest.fn().mockResolvedValue([]),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle keydown', () => {
  const suggestedUsers = [
    mockUser({ login: '1' }) as UserActive,
    mockUser({ login: '2' }) as UserActive,
    mockUser({ login: '3' }) as UserActive,
  ];

  const onSelect = jest.fn();
  const wrapper = shallowRender({ onSelect });

  wrapper.instance().handleKeyDown(mockKeyboardEvent(KeyboardKeys.UpArrow));
  expect(wrapper.state().highlighted).toEqual({ login: '', name: 'unassigned' });

  wrapper.setState({ suggestedUsers });

  // press down to highlight the first
  wrapper.instance().handleKeyDown(mockKeyboardEvent(KeyboardKeys.DownArrow));
  expect(wrapper.state().highlighted).toBe(suggestedUsers[0]);

  // press up to loop around to last
  wrapper.instance().handleKeyDown(mockKeyboardEvent(KeyboardKeys.UpArrow));
  expect(wrapper.state().highlighted).toBe(suggestedUsers[2]);

  // press down to loop around to first
  wrapper.instance().handleKeyDown(mockKeyboardEvent(KeyboardKeys.DownArrow));
  expect(wrapper.state().highlighted).toBe(suggestedUsers[0]);

  // press down highlight the next
  wrapper.instance().handleKeyDown(mockKeyboardEvent(KeyboardKeys.DownArrow));
  expect(wrapper.state().highlighted).toBe(suggestedUsers[1]);

  // press enter to select the highlighted user
  wrapper.instance().handleKeyDown(mockKeyboardEvent(KeyboardKeys.Enter));
  expect(onSelect).toHaveBeenCalledWith(suggestedUsers[1]);
});

it('should handle search', async () => {
  const users = [mockUser({ login: '1' }), mockUser({ login: '2' }), mockUser({ login: '3' })];
  (searchUsers as jest.Mock).mockResolvedValueOnce({ users });

  const onSelect = jest.fn();

  const wrapper = shallowRender({ onSelect });
  expect(wrapper.state().suggestedUsers.length).toBe(1);
  wrapper.instance().handleSearch('j');

  expect(searchUsers).not.toHaveBeenCalled();

  wrapper.instance().handleSearch('jo');
  expect(wrapper.state().loading).toBe(true);
  expect(searchUsers).toHaveBeenCalledWith({ q: 'jo' });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().suggestedUsers).toHaveLength(4);

  jest.clearAllMocks();

  wrapper.instance().handleSearch('');
  expect(searchUsers).not.toHaveBeenCalled();
  expect(wrapper.state().suggestedUsers.length).toBe(1);
});

it('should allow current user selection', async () => {
  const loggedInUser = mockLoggedInUser();
  const users = [mockUser({ login: '1' }), mockUser({ login: '2' }), mockUser({ login: '3' })];
  (searchUsers as jest.Mock).mockResolvedValueOnce({ users });

  const wrapper = shallowRender({ allowCurrentUserSelection: true, loggedInUser });
  expect(wrapper.state().suggestedUsers[0]).toBe(loggedInUser);

  wrapper.instance().handleSearch('jo');
  await waitAndUpdate(wrapper);
  expect(wrapper.state().suggestedUsers).toHaveLength(4);

  wrapper.instance().handleSearch('');
  expect(wrapper.state().suggestedUsers[0]).toBe(loggedInUser);
});

function mockKeyboardEvent(key: KeyboardKeys): React.KeyboardEvent {
  return mockEvent({ nativeEvent: mockEvent({ key }) });
}

function shallowRender(props?: Partial<AssigneeSelection['props']>) {
  return shallow<AssigneeSelection>(
    <AssigneeSelection
      allowCurrentUserSelection={false}
      loggedInUser={mockLoggedInUser()}
      onSelect={jest.fn()}
      {...props}
    />
  );
}
