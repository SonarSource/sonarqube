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
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { Route } from 'react-router-dom';
import { byRole, byTestId, byText } from 'testing-library-selector';
import SecurityHotspotServiceMock from '../../../api/mocks/SecurityHotspotServiceMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import SecurityHotspotsApp from '../SecurityHotspotsApp';

jest.mock('../../../api/measures');
jest.mock('../../../api/security-hotspots');
jest.mock('../../../api/rules');
jest.mock('../../../api/components');

const ui = {
  selectStatusButton: byRole('button', {
    name: 'hotspots.status.select_status',
  }),
  editAssigneeButton: byRole('button', {
    name: 'hotspots.assignee.change_user',
  }),
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

it('should self-assign hotspot', async () => {
  const user = userEvent.setup();
  renderSecurityHotspotsApp();

  expect(await ui.activeAssignee.find()).toHaveTextContent('John Doe');

  await user.click(await ui.editAssigneeButton.find());
  await user.click(ui.currentUserSelectionItem.get());

  expect(ui.successGlobalMessage.get()).toHaveTextContent(`hotspots.assign.success.foo`);
  expect(ui.activeAssignee.get()).toHaveTextContent('foo');
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

  await user.click(await ui.selectStatusButton.find());

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
      branchLikes: [],
      onBranchesChange: jest.fn(),
      onComponentChange: jest.fn(),
      component: mockComponent({
        key: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
        name: 'benflix',
      }),
    }
  );
}
