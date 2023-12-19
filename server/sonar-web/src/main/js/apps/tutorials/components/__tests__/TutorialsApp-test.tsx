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
import SettingsServiceMock from '../../../../api/mocks/SettingsServiceMock';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import handleRequiredAuthentication from '../../../../helpers/handleRequiredAuthentication';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole } from '../../../../helpers/testSelector';
import { Permissions } from '../../../../types/permissions';
import routes from '../../routes';

jest.mock('../../../../helpers/handleRequiredAuthentication', () => jest.fn());

let settingsMock: SettingsServiceMock;
let tokenMock: UserTokensMock;

beforeAll(() => {
  settingsMock = new SettingsServiceMock();
  tokenMock = new UserTokensMock();
});

afterEach(() => {
  tokenMock.reset();
  settingsMock.reset();
});

beforeEach(jest.clearAllMocks);

const ui = {
  loading: byLabelText('loading'),
  localScanButton: byRole('heading', { name: 'onboarding.tutorial.choose_method' }),
};

it('renders tutorials page', async () => {
  renderTutorialsApp(mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }));
  expect(ui.loading.get()).toBeInTheDocument();
  expect(await ui.localScanButton.find()).toBeInTheDocument();
});

it('should redirect if user is not logged in', () => {
  renderTutorialsApp();
  expect(handleRequiredAuthentication).toHaveBeenCalled();
  expect(ui.loading.query()).not.toBeInTheDocument();
  expect(ui.localScanButton.query()).not.toBeInTheDocument();
});

function renderTutorialsApp(currentUser = mockCurrentUser()) {
  return renderAppWithComponentContext('tutorials', routes, {
    currentUser,
  });
}
