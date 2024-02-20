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
import userEvent from '@testing-library/user-event';
import WebApiServiceMock from '../../../api/mocks/WebApiServiceMock';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../helpers/testSelector';
import routes from '../routes';

jest.mock('../../../components/common/ScreenPositionHelper');

const webApiHandler = new WebApiServiceMock();

jest.mock('../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  removeSideBarClass: jest.fn(),
}));

beforeAll(() => {
  webApiHandler.reset();
});

it('should allow to browse the api', async () => {
  const user = userEvent.setup();
  renderWebApi();

  expect(await ui.sidebarHeader.find()).toBeInTheDocument();
  expect(await ui.domainMenuItems.findAll()).toHaveLength(1);

  await user.click(ui.domainMenuItemLink('foo/bar').get());

  expect(await ui.domainHeader('foo/bar').find()).toBeInTheDocument();
  expect(await byText('get normal memos').find()).toBeInTheDocument();
  expect(byText('Action 1').get()).toBeInTheDocument();
  expect(byText('deprecated action').query()).not.toBeInTheDocument();

  // Search to filter domains
  await user.type(ui.searchInput.get(), 'memo');

  expect(await ui.domainMenuItems.findAll()).toHaveLength(1);

  // Open the domain again
  await user.click(ui.domainMenuItemLink('foo/bar').get());

  expect(await byText('get normal memos').find()).toBeInTheDocument();
  expect(byText('Action 1').query()).not.toBeInTheDocument();
  expect(byText('deprecated action').query()).not.toBeInTheDocument();

  await user.clear(ui.searchInput.get());

  // Show internal
  await user.click(ui.showInternalCheckbox.get());

  expect(await ui.domainMenuItems.findAll()).toHaveLength(2);

  await user.click(ui.domainMenuItemLink('internal/thing1 internal').get());
  expect(await byText('get internal memos').find()).toBeInTheDocument();
  expect(byText('get normal memos').query()).not.toBeInTheDocument();
  expect(byText('Action 1').query()).not.toBeInTheDocument();

  // Show deprecated
  await user.click(ui.showDeprecatedCheckbox.get());
  await user.click(ui.domainMenuItemLink('foo/bar').get());

  expect(await byText('deprecated action').find()).toBeInTheDocument();
  expect(byText('get normal memos').get()).toBeInTheDocument();
  expect(byText('Action 1').get()).toBeInTheDocument();
});

function renderWebApi(navigateTo?: string) {
  return renderAppRoutes('web_api', routes, { navigateTo });
}

const ui = {
  domainMenuItems: byRole('navigation').byRole('link'),
  domainMenuItemLink: (name: string) => byRole('navigation').byRole('link', { name }),
  domainHeader: (name: string) => byRole('heading', { level: 2, name }),
  sidebarHeader: byRole('heading', { name: 'api_documentation.page' }),
  searchInput: byLabelText('api_documentation.search'),
  showInternalCheckbox: byRole('checkbox', { name: 'api_documentation.show_internal' }),
  showDeprecatedCheckbox: byRole('checkbox', { name: 'api_documentation.show_deprecated' }),
};
