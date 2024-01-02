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
import * as React from 'react';
import { mockAppState, mockCurrentUser } from '../../../../../helpers/testMocks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import GlobalNavMenu from '../GlobalNavMenu';

it('should work with extensions', () => {
  const appState = mockAppState({
    globalPages: [{ key: 'foo', name: 'Foo' }],
    qualifiers: ['TRK'],
  });

  const currentUser = mockCurrentUser({
    isLoggedIn: false,
    dismissedNotices: {},
  });
  renderGlobalNavMenu({ appState, currentUser });
  expect(screen.getByText('more')).toBeInTheDocument();
});

it('should show administration menu if the user has the rights', () => {
  const appState = mockAppState({
    canAdmin: true,
    globalPages: [],
    qualifiers: ['TRK'],
  });
  const currentUser = mockCurrentUser({
    isLoggedIn: false,
    dismissedNotices: {},
  });

  renderGlobalNavMenu({ appState, currentUser });
  expect(screen.getByText('layout.settings')).toBeInTheDocument();
});

function renderGlobalNavMenu({
  appState = mockAppState(),
  currentUser = mockCurrentUser(),
  location = { pathname: '' },
}) {
  renderApp('/', <GlobalNavMenu currentUser={currentUser} location={location} />, {
    appState,
  });
}
