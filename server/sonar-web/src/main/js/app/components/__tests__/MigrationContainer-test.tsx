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
import * as React from 'react';
import { Route } from 'react-router-dom';
import { getSystemStatus } from '../../../helpers/system';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { byText } from '../../../helpers/testSelector';
import MigrationContainer from '../MigrationContainer';

jest.mock('../../../helpers/system', () => ({
  getSystemStatus: jest.fn(),
}));

const originalLocation = window.location;

beforeAll(() => {
  const location = {
    pathname: '/projects',
    search: '?query=toto',
    hash: '#hash',
  };
  Object.defineProperty(window, 'location', {
    writable: true,
    value: location,
  });
});

afterAll(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation,
  });
});

it('should render correctly if system is up', () => {
  (getSystemStatus as jest.Mock).mockReturnValueOnce('UP');

  renderMigrationContainer();

  expect(byText('children').get()).toBeInTheDocument();
});

it('should render correctly if system is starting', () => {
  (getSystemStatus as jest.Mock).mockReturnValueOnce('STARTING');

  renderMigrationContainer();

  expect(
    byText('/maintenance?return_to=%2Fprojects%3Fquery%3Dtoto%23hash').get(),
  ).toBeInTheDocument();
});

function renderMigrationContainer() {
  return renderAppRoutes('/', () => (
    <Route element={<MigrationContainer />}>
      <Route index element={<div>children</div>} />
    </Route>
  ));
}
