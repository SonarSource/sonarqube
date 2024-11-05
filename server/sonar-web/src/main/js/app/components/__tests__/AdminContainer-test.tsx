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
import * as React from 'react';
import { Route, useOutletContext } from 'react-router-dom';
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { getSystemStatus, waitSystemUPStatus } from '../../../api/system';
import { mockAppState } from '../../../helpers/testMocks';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { AdminPagesContext } from '../../../types/admin';
import { AdminContainer, AdminContainerProps } from '../AdminContainer';
import AdminContext from '../AdminContext';

jest.mock('../../../helpers/l10nBundle', () => {
  const bundle = jest.requireActual('../../../helpers/l10nBundle');
  return {
    ...bundle,
    getIntl: () => ({ formatMessage: jest.fn() }),
  };
});

jest.mock('../../../api/navigation', () => ({
  getSettingsNavigation: jest
    .fn()
    .mockResolvedValue({ extensions: [{ key: 'asd', name: 'asdf' }] }),
}));

jest.mock('../../../api/plugins', () => ({
  getPendingPlugins: jest.fn().mockResolvedValue({
    installing: [{ key: '1', name: 'installing' }],
    updating: [
      { key: '2', name: 'updating' },
      { key: '2b', name: 'update this too' },
    ],
    removing: [{ key: '3', name: 'removing' }],
  }),
}));

jest.mock('../../../api/system', () => ({
  getSystemStatus: jest.fn().mockResolvedValue({ status: 'DOWN' }),
  waitSystemUPStatus: jest.fn().mockResolvedValue({ status: 'RESTARTING' }),
}));

const originalLocation = window.location;
const reload = jest.fn();

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: { reload },
  });
});

afterAll(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation,
  });
});

it('should render nav and provide context to children', async () => {
  const user = userEvent.setup();
  renderAdminContainer();

  expect(await ui.navHeader.find()).toBeInTheDocument();

  expect(ui.pagesList.byRole('listitem').getAll()).toHaveLength(1);
  expect(ui.pagesList.byText('asdf').get()).toBeInTheDocument();

  expect(ui.pluginsInstallingList.byRole('listitem').getAll()).toHaveLength(1);
  expect(ui.pluginsInstallingList.byText('installing').get()).toBeInTheDocument();

  expect(ui.pluginsUpdatingList.byRole('listitem').getAll()).toHaveLength(2);
  expect(ui.pluginsUpdatingList.byText('updating').get()).toBeInTheDocument();
  expect(ui.pluginsUpdatingList.byText('update this too').get()).toBeInTheDocument();

  expect(ui.pluginsRemovingList.byRole('listitem').getAll()).toHaveLength(1);
  expect(ui.pluginsRemovingList.byText('removing').get()).toBeInTheDocument();

  expect(byText('DOWN').get()).toBeInTheDocument();

  // Renders plugins notification
  expect(ui.pluginsNotification.get()).toBeInTheDocument();

  // Trigger a status update
  jest.mocked(getSystemStatus).mockResolvedValueOnce({ id: '', version: '', status: 'RESTARTING' });
  jest.mocked(waitSystemUPStatus).mockResolvedValueOnce({ id: '', version: '', status: 'UP' });
  await user.click(ui.fetchStatusButton.get());

  expect(await byText('UP').find()).toBeInTheDocument();
  expect(reload).toHaveBeenCalled();
});

function renderAdminContainer(props: Partial<AdminContainerProps> = {}) {
  return renderAppRoutes('admin', () => (
    <Route
      path="admin"
      element={
        <AdminContainer
          appState={mockAppState({
            canAdmin: true,
          })}
          {...props}
        />
      }
    >
      <Route index element={<TestChildComponent />} />
    </Route>
  ));
}

function TestChildComponent() {
  const { adminPages } = useOutletContext<AdminPagesContext>();

  const { fetchPendingPlugins, fetchSystemStatus, pendingPlugins, systemStatus } =
    React.useContext(AdminContext);

  return (
    <div>
      <div id="component-nav-portal" />
      <ul aria-label="pages">
        {adminPages.map((page) => (
          <li key={page.key}>{page.name}</li>
        ))}
      </ul>

      <ul aria-label="plugins - installing">
        {pendingPlugins.installing.map((p) => (
          <li key={p.key}>{p.name}</li>
        ))}
      </ul>
      <ul aria-label="plugins - removing">
        {pendingPlugins.removing.map((p) => (
          <li key={p.key}>{p.name}</li>
        ))}
      </ul>
      <ul aria-label="plugins - updating">
        {pendingPlugins.updating.map((p) => (
          <li key={p.key}>{p.name}</li>
        ))}
      </ul>
      <button type="button" onClick={fetchPendingPlugins}>
        fetch plugins
      </button>

      {systemStatus}
      <button type="button" onClick={fetchSystemStatus}>
        fetch status
      </button>
    </div>
  );
}

const ui = {
  navHeader: byRole('heading', { name: 'layout.settings' }),
  pagesList: byLabelText('pages'),
  pluginsNotification: byText('marketplace.instance_needs_to_be_restarted_to'),
  pluginsInstallingList: byLabelText('plugins - installing'),
  pluginsUpdatingList: byLabelText('plugins - updating'),
  pluginsRemovingList: byLabelText('plugins - removing'),

  fetchPluginsButton: byRole('button', { name: 'fetch plugins' }),
  fetchStatusButton: byRole('button', { name: 'fetch status' }),
};
