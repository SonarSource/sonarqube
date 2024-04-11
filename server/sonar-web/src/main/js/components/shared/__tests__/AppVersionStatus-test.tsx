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
import AppStateContextProvider from '../../../app/components/app-state/AppStateContextProvider';
import AppVersionStatus from '../../../components/shared/AppVersionStatus';
import { mockAppState } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';

jest.mock('../../../helpers/dates', () => ({
  ...jest.requireActual('../../../helpers/dates'),
  now: () => new Date('2022-01-01'),
}));

it('should not render inactive version if it has not reached EOL', () => {
  renderAppVersionStatus(mockAppState({ versionEOL: '2022-01-02' }));

  expect(
    screen.queryByRole('link', { name: /footer.version.status.inactive/ })
  ).not.toBeInTheDocument();
});

it('should render inactive version if it has reached EOL', () => {
  renderAppVersionStatus(mockAppState({ versionEOL: '2021-12-30' }));

  expect(screen.getByRole('link', { name: /footer.version.status.inactive/ })).toBeInTheDocument();
});

const renderAppVersionStatus = (appState = mockAppState()) => {
  return renderComponent(
    <AppStateContextProvider appState={appState}>
      <AppVersionStatus />
    </AppStateContextProvider>
  );
};
