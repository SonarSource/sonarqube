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

import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole, byText } from 'testing-library-selector';
import UsersServiceMock from '../../../api/mocks/UsersServiceMock';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import UsersApp from '../UsersApp';

jest.mock('../../../api/users');
jest.mock('../../../api/system');

const handler = new UsersServiceMock();

const ui = {
  createUserButton: byRole('button', { name: 'users.create_user' }),
  infoManageMode: byText(/users\.page\.managed_description/),
  description: byText('users.page.description'),
  allFilter: byRole('button', { name: 'all' }),
  managedFilter: byRole('button', { name: 'users.managed' }),
  localFilter: byRole('button', { name: 'users.local' }),
  aliceRow: byRole('row', { name: 'AM Alice Merveille alice.merveille never' }),
  aliceRowWithLocalBadge: byRole('row', {
    name: 'AM Alice Merveille alice.merveille users.local never',
  }),
  bobRow: byRole('row', { name: 'BM Bob Marley bob.marley never' }),
};

describe('in non managed mode', () => {
  beforeEach(() => {
    handler.setIsManaged(false);
  });

  it('should allow the creation of user', async () => {
    renderUsersApp();

    expect(await ui.description.find()).toBeInTheDocument();
    expect(ui.createUserButton.get()).toBeEnabled();
  });

  it('should render all users', async () => {
    renderUsersApp();

    expect(ui.aliceRowWithLocalBadge.query()).not.toBeInTheDocument();
    expect(await ui.aliceRow.find()).toBeInTheDocument();
    expect(await ui.bobRow.find()).toBeInTheDocument();
  });
});

describe('in manage mode', () => {
  beforeEach(() => {
    handler.setIsManaged(true);
  });

  it('should not be able to create a user"', async () => {
    renderUsersApp();
    expect(await ui.createUserButton.get()).toBeDisabled();
    expect(await ui.infoManageMode.find()).toBeInTheDocument();
  });

  it('should render list of all users', async () => {
    renderUsersApp();

    expect(await ui.allFilter.find()).toBeInTheDocument();

    expect(ui.aliceRowWithLocalBadge.get()).toBeInTheDocument();
    expect(ui.bobRow.get()).toBeInTheDocument();
  });

  it('should render list of managed users', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    // The click downs't work without this line
    expect(await ui.managedFilter.find()).toBeInTheDocument();
    await user.click(await ui.managedFilter.get());

    expect(ui.aliceRowWithLocalBadge.query()).not.toBeInTheDocument();
    expect(ui.bobRow.get()).toBeInTheDocument();
  });

  it('should render list of local users', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    // The click downs't work without this line
    expect(await ui.localFilter.find()).toBeInTheDocument();
    await user.click(await ui.localFilter.get());

    expect(ui.aliceRowWithLocalBadge.get()).toBeInTheDocument();
    expect(ui.bobRow.query()).not.toBeInTheDocument();
  });
});

function renderUsersApp() {
  return renderApp('admin/users', <UsersApp />);
}
