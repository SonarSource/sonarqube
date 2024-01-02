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
import { Props as ReactSelectAsyncProps } from 'react-select/async';
import { SearchSelect } from '../../../../components/controls/Select';
import Avatar from '../../../../components/ui/Avatar';
import { mockCurrentUser, mockIssue, mockLoggedInUser } from '../../../../helpers/testMocks';
import { searchAssignees } from '../../utils';
import AssigneeSelect, {
  AssigneeOption,
  AssigneeSelectProps,
  MIN_QUERY_LENGTH,
} from '../AssigneeSelect';

jest.mock('../../utils', () => ({
  searchAssignees: jest.fn().mockResolvedValue({
    results: [
      {
        active: true,
        avatar: '##avatar1',
        login: 'toto@toto',
        name: 'toto',
      },
      {
        active: false,
        avatar: '##avatar2',
        login: 'tata@tata',
        name: 'tata',
      },
      {
        active: true,
        avatar: '##avatar3',
        login: 'titi@titi',
      },
    ],
  }),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ currentUser: mockLoggedInUser(), issues: [mockIssue()] })).toMatchSnapshot(
    'logged in & assignable issues'
  );
  expect(shallowRender({ currentUser: mockLoggedInUser() })).toMatchSnapshot(
    'logged in & no assignable issues'
  );
  expect(shallowRender({ issues: [mockIssue(false, { assignee: 'someone' })] })).toMatchSnapshot(
    'unassignable issues'
  );
});

it('should render options correctly', () => {
  const wrapper = shallowRender();

  expect(
    shallow(
      wrapper.instance().renderAssignee({
        avatar: '##avatar1',
        value: 'toto@toto',
        label: 'toto',
      })
    )
      .find(Avatar)
      .exists()
  ).toBe(true);

  expect(
    shallow(
      wrapper.instance().renderAssignee({
        value: 'toto@toto',
        label: 'toto',
      })
    )
      .find(Avatar)
      .exists()
  ).toBe(false);
});

it('should render noOptionsMessage correctly', () => {
  const wrapper = shallowRender();
  expect(
    wrapper.find<ReactSelectAsyncProps<AssigneeOption, false>>(SearchSelect).props()
      .noOptionsMessage!({ inputValue: 'a' })
  ).toBe(`select2.tooShort.${MIN_QUERY_LENGTH}`);

  expect(
    wrapper.find<ReactSelectAsyncProps<AssigneeOption, false>>(SearchSelect).props()
      .noOptionsMessage!({ inputValue: 'droids' })
  ).toBe('select2.noMatches');
});

it('should handle assignee search', async () => {
  const onAssigneeSelect = jest.fn();
  const wrapper = shallowRender({ onAssigneeSelect });

  wrapper.instance().handleAssigneeSearch('a', jest.fn());
  expect(searchAssignees).not.toHaveBeenCalled();

  const result = await new Promise((resolve: (opts: AssigneeOption[]) => void) => {
    wrapper.instance().handleAssigneeSearch('someone', resolve);
  });

  expect(result).toEqual([
    {
      avatar: '##avatar1',
      value: 'toto@toto',
      label: 'toto',
    },
    {
      avatar: '##avatar2',
      value: 'tata@tata',
      label: 'user.x_deleted.tata',
    },
    {
      avatar: '##avatar3',
      value: 'titi@titi',
      label: 'user.x_deleted.titi@titi',
    },
  ]);
});

function shallowRender(overrides: Partial<AssigneeSelectProps> = {}) {
  return shallow<AssigneeSelect>(
    <AssigneeSelect
      inputId="id"
      currentUser={mockCurrentUser()}
      issues={[]}
      onAssigneeSelect={jest.fn()}
      {...overrides}
    />
  );
}
