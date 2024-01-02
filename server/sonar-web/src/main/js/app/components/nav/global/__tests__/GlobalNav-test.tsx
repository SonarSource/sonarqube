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
import React from 'react';
import { mockAppState, mockCurrentUser, mockLocation } from '../../../../../helpers/testMocks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import GlobalNav from '../GlobalNav';

it('render global navigation correctly for anonymous user', () => {
  renderGlobalNav({ appState: mockAppState() });
  expect(screen.getByText('projects.page')).toBeInTheDocument();
  expect(screen.getByText('issues.page')).toBeInTheDocument();
  expect(screen.getByText('coding_rules.page')).toBeInTheDocument();
  expect(screen.getByText('quality_profiles.page')).toBeInTheDocument();
  expect(screen.getByText('quality_gates.page')).toBeInTheDocument();
  expect(screen.getByText('layout.login')).toBeInTheDocument();
});

it('render global navigation correctly for logged in user', () => {
  renderGlobalNav({ currentUser: mockCurrentUser({ isLoggedIn: true }) });
  expect(screen.getByText('projects.page')).toBeInTheDocument();
  expect(screen.queryByText('layout.login')).not.toBeInTheDocument();
});

it('render the logo correctly', () => {
  renderGlobalNav({
    appState: mockAppState({
      settings: {
        'sonar.lf.logoUrl': 'http://sonarsource.com/test.svg',
      },
    }),
  });
  const image = screen.getByAltText('layout.nav.home_logo_alt');
  expect(image).toHaveAttribute('src', 'http://sonarsource.com/test.svg');
});

function renderGlobalNav({ appState = mockAppState(), currentUser = mockCurrentUser() }) {
  renderApp('/', <GlobalNav location={mockLocation()} />, {
    appState,
    currentUser,
  });
}
