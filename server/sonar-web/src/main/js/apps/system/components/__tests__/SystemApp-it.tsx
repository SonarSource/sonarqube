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
import userEvent from '@testing-library/user-event';
import { first } from 'lodash';
import SystemServiceMock from '../../../../api/mocks/SystemServiceMock';
import { renderAppRoutes } from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import routes from '../../routes';
import { LogsLevels } from '../../utils';

const systemMock = new SystemServiceMock();

afterEach(() => {
  systemMock.reset();
});

describe('System Info Standalone', () => {
  it('renders correctly', async () => {
    const { user, ui } = getPageObjects();
    renderSystemApp();
    await ui.appIsLoaded();

    expect(ui.copyIdInformation.get()).toHaveAttribute(
      'data-clipboard-text',
      expect.stringContaining(`Server ID: asd564-asd54a-5dsfg45`),
    );

    expect(ui.sectionButton('System').get()).toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'High Availability' })).not.toBeInTheDocument();
    await user.click(ui.sectionButton('System').get());
    expect(screen.getByRole('cell', { name: 'High Availability' })).toBeInTheDocument();
  });

  it('can change logs level', async () => {
    const { user, ui } = getPageObjects();
    renderSystemApp();
    expect(await ui.pageHeading.find()).toBeInTheDocument();

    await user.click(ui.changeLogLevelButton.get());
    expect(ui.logLevelWarning.queryAll()).toHaveLength(0);
    await user.click(ui.logLevelsRadioButton(LogsLevels.DEBUG).get());
    expect(ui.logLevelWarning.get()).toBeInTheDocument();

    await user.click(ui.saveButton.get());
    expect(ui.logLevelWarningShort.queryAll()).toHaveLength(2);
  });

  it('can download logs & system info', async () => {
    const { user, ui } = getPageObjects();
    renderSystemApp();
    expect(await ui.pageHeading.find()).toBeInTheDocument();

    await user.click(ui.downloadLogsButton.get());
    [
      'Main Process',
      'Compute Engine',
      'Search Engine',
      'Web Server',
      'Access Logs',
      'Deprecation Logs',
    ].forEach((name) => {
      expect(screen.getByRole('menuitem', { name })).toBeInTheDocument();
    });
    expect(ui.downloadSystemInfoButton.get()).toBeInTheDocument();
  });
});

describe('System Info Cluster', () => {
  it('renders correctly', async () => {
    systemMock.setIsCluster(true);
    const { user, ui } = getPageObjects();
    renderSystemApp();
    await ui.appIsLoaded();

    expect(await ui.pageHeading.find()).toBeInTheDocument();

    expect(ui.downloadLogsButton.query()).not.toBeInTheDocument();
    expect(ui.downloadSystemInfoButton.get()).toBeInTheDocument();

    expect(ui.copyIdInformation.get()).toHaveAttribute(
      'data-clipboard-text',
      expect.stringContaining(`Server ID: asd564-asd54a-5dsfg45`),
    );

    // Renders health checks
    expect(ui.healthCauseWarning.get()).toBeInTheDocument();

    // Renders App node
    expect(first(ui.sectionButton('server1.example.com').getAll())).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Web Logging' })).not.toBeInTheDocument();
    await user.click(first(ui.sectionButton('server1.example.com').getAll()) as HTMLElement);
    expect(screen.getByRole('heading', { name: 'Web Logging' })).toBeInTheDocument();
  });
});

function renderSystemApp() {
  return renderAppRoutes('system', routes);
}

function getPageObjects() {
  const user = userEvent.setup();

  const ui = {
    pageHeading: byRole('heading', { name: 'system_info.page' }),
    downloadLogsButton: byRole('button', { name: 'system.download_logs' }),
    downloadSystemInfoButton: byRole('link', { name: 'system.download_system_info' }),
    copyIdInformation: byRole('button', { name: 'system.copy_id_info' }),
    sectionButton: (name: string) => byRole('button', { name }),
    changeLogLevelButton: byRole('button', { name: 'system.logs_level.change' }),
    logLevelsRadioButton: (name: LogsLevels) => byRole('radio', { name }),
    logLevelWarning: byText('system.log_level.warning'),
    logLevelWarningShort: byText('system.log_level.warning.short'),
    healthCauseWarning: byText('Friendly warning'),
    saveButton: byRole('button', { name: 'save' }),
  };

  async function appIsLoaded() {
    expect(await ui.pageHeading.find()).toBeInTheDocument();
  }

  return {
    ui: { ...ui, appIsLoaded },
    user,
  };
}
