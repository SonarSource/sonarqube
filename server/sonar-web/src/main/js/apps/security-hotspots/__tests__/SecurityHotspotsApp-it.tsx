/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { Route } from 'react-router-dom';
import selectEvent from 'react-select-event';
import { byRole, byTestId, byText } from 'testing-library-selector';
import SecurityHotspotServiceMock from '../../../api/mocks/SecurityHotspotServiceMock';
import { getSecurityHotspots, setSecurityHotspotStatus } from '../../../api/security-hotspots';
import { searchUsers } from '../../../api/users';
import { mockMainBranch } from '../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import SecurityHotspotsApp from '../SecurityHotspotsApp';

jest.mock('../../../api/measures');
jest.mock('../../../api/security-hotspots');
jest.mock('../../../api/rules');
jest.mock('../../../api/components');
jest.mock('../../../helpers/security-standard');
jest.mock('../../../api/users');

const ui = {
  inputAssignee: byRole('searchbox', { name: 'hotspots.assignee.select_user' }),
  selectStatusButton: byRole('button', {
    name: 'hotspots.status.select_status',
  }),
  editAssigneeButton: byRole('button', {
    name: 'hotspots.assignee.change_user',
  }),
  filterAssigneeToMe: byRole('button', {
    name: 'hotspot.filters.assignee.assigned_to_me',
  }),
  filterSeeAll: byRole('button', { name: 'hotspot.filters.assignee.all' }),
  filterByStatus: byRole('combobox', { name: 'hotspot.filters.status' }),
  filterByPeriod: byRole('combobox', { name: 'hotspot.filters.period' }),
  noHotspotForFilter: byText('hotspots.no_hotspots_for_filters.title'),
  selectStatus: byRole('button', { name: 'hotspots.status.select_status' }),
  toReviewStatus: byText('hotspots.status_option.TO_REVIEW'),
  changeStatus: byRole('button', { name: 'hotspots.status.change_status' }),
  hotspotTitle: (name: string | RegExp) => byRole('heading', { name }),
  hotspotStatus: byRole('heading', { name: 'status: hotspots.status_option.FIXED' }),
  hotpostListTitle: byRole('heading', { name: 'hotspots.list_title.TO_REVIEW.2' }),
  activeAssignee: byTestId('assignee-name'),
  successGlobalMessage: byRole('status'),
  currentUserSelectionItem: byText('foo'),
  panel: byTestId('security-hotspot-test'),
};

let handler: SecurityHotspotServiceMock;

beforeEach(() => {
  handler = new SecurityHotspotServiceMock();
});

afterEach(() => {
  handler.reset();
});

it('should be able to self-assign a hotspot', async () => {
  const user = userEvent.setup();
  renderSecurityHotspotsApp();

  expect(await ui.activeAssignee.find()).toHaveTextContent('John Doe');

  await user.click(ui.editAssigneeButton.get());
  await user.click(ui.currentUserSelectionItem.get());

  expect(ui.successGlobalMessage.get()).toHaveTextContent(`hotspots.assign.success.foo`);
  expect(ui.activeAssignee.get()).toHaveTextContent('foo');
});

it('should be able to search for a user on the assignee', async () => {
  const user = userEvent.setup();
  renderSecurityHotspotsApp();

  await user.click(await ui.editAssigneeButton.find());
  await user.click(ui.inputAssignee.get());

  await user.keyboard('User');

  expect(searchUsers).toHaveBeenLastCalledWith({ q: 'User' });
  await user.keyboard('{ArrowDown}{Enter}');
  expect(ui.successGlobalMessage.get()).toHaveTextContent(`hotspots.assign.success.User John`);
});

it('should be able to filter the hotspot list', async () => {
  const user = userEvent.setup();
  renderSecurityHotspotsApp();

  expect(await ui.hotpostListTitle.find()).toBeInTheDocument();

  await user.click(ui.filterAssigneeToMe.get());
  expect(ui.noHotspotForFilter.get()).toBeInTheDocument();
  await selectEvent.select(ui.filterByStatus.get(), ['hotspot.filters.status.to_review']);

  expect(getSecurityHotspots).toHaveBeenLastCalledWith({
    inNewCodePeriod: false,
    onlyMine: true,
    p: 1,
    projectKey: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
    ps: 500,
    resolution: undefined,
    status: 'TO_REVIEW',
  });

  await selectEvent.select(ui.filterByPeriod.get(), ['hotspot.filters.period.since_leak_period']);

  expect(getSecurityHotspots).toHaveBeenLastCalledWith({
    inNewCodePeriod: true,
    onlyMine: true,
    p: 1,
    projectKey: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
    ps: 500,
    resolution: undefined,
    status: 'TO_REVIEW',
  });

  await user.click(ui.filterSeeAll.get());

  expect(ui.hotpostListTitle.get()).toBeInTheDocument();
});

it('should be able to navigate the hotspot list with keyboard', async () => {
  const user = userEvent.setup();
  renderSecurityHotspotsApp();

  await user.keyboard('{ArrowDown}');
  expect(await ui.hotspotTitle(/'2' is a magic number./).find()).toBeInTheDocument();
  await user.keyboard('{ArrowUp}');
  expect(await ui.hotspotTitle(/'3' is a magic number./).find()).toBeInTheDocument();
});

it('should be able to change the status of a hotspot', async () => {
  const user = userEvent.setup();
  const comment = 'COMMENT-TEXT';

  renderSecurityHotspotsApp();

  expect(await ui.selectStatus.find()).toBeInTheDocument();

  await user.click(ui.selectStatus.get());
  await user.click(ui.toReviewStatus.get());

  await user.click(screen.getByRole('textbox', { name: 'hotspots.status.add_comment' }));
  await user.keyboard(comment);

  await user.click(ui.changeStatus.get());

  expect(setSecurityHotspotStatus).toHaveBeenLastCalledWith('test-1', {
    comment: 'COMMENT-TEXT',
    resolution: undefined,
    status: 'TO_REVIEW',
  });

  expect(ui.hotspotStatus.get()).toBeInTheDocument();
});

it('should remember the comment when toggling change status panel for the same security hotspot', async () => {
  const user = userEvent.setup();
  renderSecurityHotspotsApp();

  await user.click(await ui.selectStatusButton.find());

  const comment = 'This is a comment';

  const commentSection = within(ui.panel.get()).getByRole('textbox');
  await user.click(commentSection);
  await user.keyboard(comment);

  // Close the panel
  await user.keyboard('{Escape}');
  // Check panel is closed
  expect(ui.panel.query()).not.toBeInTheDocument();

  await user.click(ui.selectStatusButton.get());

  expect(await screen.findByText(comment)).toBeInTheDocument();
});

function renderSecurityHotspotsApp(navigateTo?: string) {
  renderAppWithComponentContext(
    'security_hotspots',
    () => <Route path="security_hotspots" element={<SecurityHotspotsApp />} />,
    {
      navigateTo,
      currentUser: mockLoggedInUser({
        login: 'foo',
        name: 'foo',
      }),
    },
    {
      branchLike: mockMainBranch(),
      onBranchesChange: jest.fn(),
      onComponentChange: jest.fn(),
      component: mockComponent({
        key: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
        name: 'benflix',
      }),
    }
  );
}
