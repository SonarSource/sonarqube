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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byLabelText } from '~sonar-aligned/helpers/testSelector';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { mockUserBase } from '../../../../helpers/mocks/users';
import { mockCurrentUser, mockIssue, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { CurrentUser } from '../../../../types/users';
import AssigneeSelect, { AssigneeSelectProps, MIN_QUERY_LENGTH } from '../AssigneeSelect';

jest.mock('../../utils', () => ({
  searchAssignees: jest.fn().mockResolvedValue({
    results: [
      mockUserBase({
        active: true,
        avatar: 'avatar1',
        login: 'toto@toto',
        name: 'toto',
      }),
      mockUserBase({
        active: false,
        avatar: 'avatar2',
        login: 'tata@tata',
        name: 'tata',
      }),
      mockUserBase({
        active: true,
        avatar: 'avatar3',
        login: 'titi@titi',
      }),
    ],
  }),
}));

const ui = {
  combobox: byLabelText('issue_bulk_change.assignee.change'),
  searchbox: byLabelText('search.search_for_users'),
};

it('should show correct suggestions when there is assignable issue for the current user', async () => {
  const user = userEvent.setup();
  renderAssigneeSelect(
    {
      issues: [mockIssue(false, { assignee: 'someone' })],
    },
    mockLoggedInUser({ name: 'Skywalker' }),
  );

  await user.click(ui.combobox.get());
  expect(await screen.findByText('Skywalker')).toBeInTheDocument();
});

it('should show correct suggestions when all issues are already assigned to current user', async () => {
  const user = userEvent.setup();
  renderAssigneeSelect(
    {
      issues: [mockIssue(false, { assignee: 'luke' })],
    },
    mockLoggedInUser({ login: 'luke', name: 'Skywalker' }),
  );

  await user.click(ui.combobox.get());
  expect(screen.queryByText('Skywalker')).not.toBeInTheDocument();
});

it('should show correct suggestions when there is no assigneable issue', async () => {
  const user = userEvent.setup();
  renderAssigneeSelect({}, mockLoggedInUser({ name: 'Skywalker' }));

  await user.click(ui.combobox.get());
  expect(screen.queryByText('Skywalker')).not.toBeInTheDocument();
});

it('should handle assignee search correctly', async () => {
  const user = userEvent.setup();
  renderAssigneeSelect();

  // Minimum MIN_QUERY_LENGTH charachters to trigger search
  await user.click(ui.combobox.get());
  await user.type(ui.searchbox.get(), 'a');

  expect(await screen.findByText(`select2.tooShort.${MIN_QUERY_LENGTH}`)).toBeInTheDocument();

  // Trigger search
  await user.click(ui.combobox.get());
  await user.type(ui.searchbox.get(), 'someone');

  expect(await screen.findByText('toto')).toBeInTheDocument();
  expect(await screen.findByText('user.x_deleted.tata')).toBeInTheDocument();
  expect(await screen.findByText('user.x_deleted.titi@titi')).toBeInTheDocument();
});

it('should handle assignee selection', async () => {
  const onAssigneeSelect = jest.fn();
  const user = userEvent.setup();
  renderAssigneeSelect({ onAssigneeSelect });

  await user.click(ui.combobox.get());
  await user.type(ui.searchbox.get(), 'tot');

  // Do not select assignee until suggestion is selected
  expect(onAssigneeSelect).not.toHaveBeenCalled();

  // Select assignee when suggestion is selected
  await user.click(screen.getByLabelText('toto'));
  expect(onAssigneeSelect).toHaveBeenCalledTimes(1);
});

function renderAssigneeSelect(
  overrides: Partial<AssigneeSelectProps> = {},
  currentUser: CurrentUser = mockCurrentUser(),
) {
  return renderComponent(
    <CurrentUserContextProvider currentUser={currentUser}>
      <AssigneeSelect inputId="id" issues={[]} onAssigneeSelect={jest.fn()} {...overrides} />
    </CurrentUserContextProvider>,
  );
}
