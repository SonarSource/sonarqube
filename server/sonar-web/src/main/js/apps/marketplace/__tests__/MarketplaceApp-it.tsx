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
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React, { useState } from 'react';
import { NavigationServiceMock } from '../../../api/mocks/NavigationServiceMock';
import PluginsServiceMock from '../../../api/mocks/PluginsServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import AdminContext from '../../../app/components/AdminContext';
import { mockAppState } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../helpers/testSelector';
import { AppState } from '../../../types/appstate';
import { EditionKey } from '../../../types/editions';
import { PendingPluginResult } from '../../../types/plugins';
import { GlobalSettingKeys } from '../../../types/settings';
import MarketplaceAppContainer from '../MarketplaceAppContainer';

const handler = new PluginsServiceMock();
const settingsHandler = new SettingsServiceMock();
const navigationHandler = new NavigationServiceMock();

const ui = {
  title: byRole('heading', { name: 'marketplace.page.plugins' }),
  deTitle: byRole('heading', { name: 'SonarQube logo Developer Edition' }),
  eeTitle: byRole('heading', { name: 'SonarQube logo Enterprise Edition' }),
  dceTitle: byRole('heading', { name: 'SonarQube logo Data Center Edition' }),
  pluginRow: byRole('table', { name: 'marketplace.page.plugins' }).byRole('row'),
  filterAll: byRole('radio', { name: 'marketplace.all' }),
  filterInstalled: byRole('radio', { name: 'marketplace.installed' }),
  filterWithUpdates: byRole('radio', { name: 'marketplace.updates_only' }),
  search: byRole('searchbox', { name: 'marketplace.search' }),
  clearSearch: byRole('button', { name: 'clear' }),
  noPluginsText: byText('marketplace.plugin_list.no_plugins', { exact: false }),
  acceptTerms: byRole('checkbox', { name: 'marketplace.i_accept_the' }),
  installButton: byRole('button', { name: 'marketplace.install' }),
  uninstallButton: byRole('button', { name: 'marketplace.uninstall' }),
  updateButton: byRole('button', { name: /marketplace.update_to_x/ }),
  installPending: byText('marketplace.install_pending'),
  uninstallPending: byText('marketplace.uninstall_pending'),
  updatePending: byText('marketplace.update_pending'),
  riskConsentMessage: byText('marketplace.risk_consent.installation'),
  riskConsentButton: byRole('button', { name: 'marketplace.risk_consent.action' }),
  releaseDetailsButton: byRole('button', { name: /marketplace.show_plugin_changelog/ }),
  homePageLink: byRole('link', { name: 'marketplace.homepage' }),
  issueTrackerLink: byRole('link', { name: 'marketplace.issue_tracker' }),
  releaseNotesLink: byRole('link', { name: 'marketplace.release_notes' }),
  organizationLink: byRole('link', { name: 'SonarSource' }),
  bundledAvailable: byText('marketplace.available_under_commercial_license'),
};

beforeEach(() => {
  handler.reset();
  settingsHandler.reset();
  navigationHandler.reset();
});

it('should show editions', async () => {
  renderMarketplaceApp();
  expect(await ui.title.find()).toBeInTheDocument();
  expect(ui.deTitle.get()).toBeInTheDocument();
  expect(ui.eeTitle.get()).toBeInTheDocument();
  expect(ui.dceTitle.get()).toBeInTheDocument();
});

it('should show and filter the list', async () => {
  renderMarketplaceApp();
  expect(await ui.pluginRow.findAll()).toHaveLength(6);
  expect(ui.filterAll.get()).toHaveAttribute('aria-current', 'true');
  await userEvent.click(ui.filterInstalled.get());
  expect(await ui.pluginRow.findAll()).toHaveLength(3);
  await userEvent.click(ui.filterWithUpdates.get());
  expect(await ui.pluginRow.findAll()).toHaveLength(2);
  await userEvent.click(ui.filterAll.get());
  expect(await ui.pluginRow.findAll()).toHaveLength(6);
  await userEvent.type(ui.search.get(), 'Mocked Plugin');
  expect(await ui.pluginRow.findAll()).toHaveLength(1);
  await userEvent.clear(ui.search.get());
  expect(await ui.pluginRow.findAll()).toHaveLength(6);
  await userEvent.type(ui.search.get(), 'Test');
  expect(await ui.pluginRow.findAll()).toHaveLength(4);
  await userEvent.clear(ui.search.get());
  await userEvent.type(ui.search.get(), 'Languages');
  expect(await ui.pluginRow.findAll()).toHaveLength(1);
  await userEvent.clear(ui.search.get());
  await userEvent.type(ui.search.get(), 'cantfindtheplugin');
  expect(ui.pluginRow.query()).not.toBeInTheDocument();
  expect(ui.noPluginsText.get()).toBeInTheDocument();
});

it('should install, uninstall, update', async () => {
  const user = userEvent.setup();
  renderMarketplaceApp();
  const rows = await ui.pluginRow.findAll();
  expect(rows).toHaveLength(6);
  expect(ui.installButton.query()).not.toBeInTheDocument();
  expect(ui.uninstallButton.query()).not.toBeInTheDocument();
  expect(ui.updateButton.query()).not.toBeInTheDocument();
  expect(ui.riskConsentMessage.get()).toBeInTheDocument();
  expect(ui.riskConsentButton.get()).toBeInTheDocument();
  await user.click(ui.riskConsentButton.get());
  expect(ui.riskConsentMessage.query()).not.toBeInTheDocument();

  expect(rows[0]).toHaveTextContent('ATest_install');
  expect(await ui.uninstallButton.find(rows[0])).toBeInTheDocument();
  expect(ui.installButton.query(rows[0])).not.toBeInTheDocument();
  expect(ui.updateButton.query(rows[0])).not.toBeInTheDocument();
  expect(ui.uninstallPending.query(rows[0])).not.toBeInTheDocument();
  await user.click(ui.uninstallButton.get(rows[0]));
  expect(await ui.uninstallPending.find(rows[0])).toBeInTheDocument();
  expect(ui.uninstallButton.query(rows[0])).not.toBeInTheDocument();

  expect(rows[1]).toHaveTextContent('BTest_update');
  expect(ui.installButton.query(rows[1])).not.toBeInTheDocument();
  expect(ui.uninstallButton.query(rows[1])).not.toBeInTheDocument();
  expect(ui.updateButton.get(rows[1])).toBeInTheDocument();
  expect(ui.updateButton.get(rows[1])).toHaveTextContent('1.3.0');
  expect(ui.updatePending.query(rows[1])).not.toBeInTheDocument();
  await act(() => user.click(ui.updateButton.get(rows[1])));
  expect(await ui.updatePending.find(rows[1])).toBeInTheDocument();
  expect(ui.updateButton.query(rows[1])).not.toBeInTheDocument();

  expect(rows[2]).toHaveTextContent('CFoo');
  expect(ui.installButton.get(rows[2])).toBeInTheDocument();
  expect(ui.uninstallButton.query(rows[2])).not.toBeInTheDocument();
  expect(ui.updateButton.query(rows[2])).not.toBeInTheDocument();
  expect(ui.installPending.query(rows[2])).not.toBeInTheDocument();
  await act(() => user.click(ui.installButton.get(rows[2])));
  expect(await ui.installPending.find(rows[2])).toBeInTheDocument();
  expect(ui.installButton.query(rows[2])).not.toBeInTheDocument();

  expect(rows[3]).toHaveTextContent('DTest');
  expect(ui.installButton.query(rows[2])).not.toBeInTheDocument();
  expect(ui.bundledAvailable.get(rows[3])).toBeInTheDocument();

  expect(rows[4]).toHaveTextContent('Sonar Foo');
  expect(ui.installButton.get(rows[4])).toBeInTheDocument();
  expect(ui.installButton.get(rows[4])).toBeDisabled();
  expect(ui.acceptTerms.get(rows[4])).toBeInTheDocument();
  expect(ui.acceptTerms.get(rows[4])).not.toBeChecked();
  await user.click(ui.acceptTerms.get(rows[4]));
  expect(ui.installButton.get(rows[4])).toBeEnabled();
  await act(() => user.click(ui.installButton.get(rows[4])));
  expect(await ui.installPending.find(rows[4])).toBeInTheDocument();
  expect(ui.installButton.query(rows[4])).not.toBeInTheDocument();

  expect(rows[5]).toHaveTextContent('ZTest');
  expect(ui.installButton.query(rows[5])).not.toBeInTheDocument();
  expect(ui.uninstallButton.query(rows[5])).toBeInTheDocument();
  expect(ui.updateButton.get(rows[5])).toBeInTheDocument();
  expect(ui.updateButton.get(rows[5])).toHaveTextContent('1.2.0');
  expect(ui.updatePending.query(rows[5])).not.toBeInTheDocument();
  await act(() => user.click(ui.updateButton.get(rows[5])));
  expect(await ui.updatePending.find(rows[5])).toBeInTheDocument();
  expect(ui.uninstallButton.query(rows[5])).not.toBeInTheDocument();
  expect(ui.updateButton.query(rows[5])).not.toBeInTheDocument();
});

it('should show details on the row', async () => {
  const user = userEvent.setup();
  renderMarketplaceApp();
  const rows = await ui.pluginRow.findAll();
  const row = rows[2];
  expect(row).toHaveTextContent('CFooLanguagesDescription1.0.0release description');
  expect(ui.homePageLink.get(row)).toBeInTheDocument();
  expect(ui.issueTrackerLink.get(row)).toBeInTheDocument();
  expect(ui.organizationLink.get(row)).toBeInTheDocument();
  expect(ui.releaseDetailsButton.get(row)).toBeInTheDocument();
  expect(ui.releaseNotesLink.query(row)).not.toBeInTheDocument();
  await user.click(ui.releaseDetailsButton.get(row));
  expect(ui.releaseNotesLink.get(row)).toBeInTheDocument();

  expect(ui.homePageLink.query(rows[0])).not.toBeInTheDocument();
  expect(ui.issueTrackerLink.query(rows[0])).not.toBeInTheDocument();
  expect(ui.organizationLink.query(rows[0])).not.toBeInTheDocument();

  const rowWithMultipleUpdates = rows[1];
  expect(rowWithMultipleUpdates).toHaveTextContent(
    '1.2.0marketplace._installedmarketplace.updates:1.3.0',
  );
  await user.click(ui.releaseDetailsButton.get(rowWithMultipleUpdates));
  expect(ui.releaseNotesLink.getAll(rowWithMultipleUpdates)).toHaveLength(2);
});

it.each([
  [EditionKey.developer, { de: false, ee: true, dce: true }],
  [EditionKey.enterprise, { de: false, ee: false, dce: true }],
  [EditionKey.datacenter, { de: false, ee: false, dce: false }],
])(
  'should not allow installations on editions higher than community',
  async (edition, showTitles) => {
    renderMarketplaceApp({ edition });
    const rows = await ui.pluginRow.findAll();
    expect(rows).toHaveLength(6);
    expect(ui.updateButton.query()).not.toBeInTheDocument();
    expect(ui.installButton.query()).not.toBeInTheDocument();
    expect(ui.uninstallButton.query()).not.toBeInTheDocument();

    Object.entries(showTitles).forEach(
      ([key, value]: [key: 'de' | 'ee' | 'dce', value: boolean]) => {
        // eslint-disable-next-line jest/no-conditional-in-test
        if (value) {
          // eslint-disable-next-line jest/no-conditional-expect
          expect(ui[`${key}Title`].get()).toBeInTheDocument();
        } else {
          // eslint-disable-next-line jest/no-conditional-expect
          expect(ui[`${key}Title`].query()).not.toBeInTheDocument();
        }
      },
    );

    expect(ui.riskConsentMessage.query()).not.toBeInTheDocument();
  },
);

describe('accessibility', () => {
  it('should be accessible', async () => {
    const user = userEvent.setup();
    renderMarketplaceApp();
    const row = (await ui.pluginRow.findAll())[1];
    await expect(document.body).toHaveNoA11yViolations();
    await act(() => user.click(ui.riskConsentButton.get()));
    await act(() => user.click(ui.releaseDetailsButton.get(row)));
    await expect(document.body).toHaveNoA11yViolations();
  });
});

function renderMarketplaceApp(appStateOverrides: Partial<AppState> = {}) {
  function Wrapper() {
    const [pendingPlugins, setPendingPlugins] = useState<PendingPluginResult>({
      installing: [],
      removing: [],
      updating: [],
    });
    const fetchPendingPlugins = async () => {
      setPendingPlugins(await handler.handleGetPendingPlugins());
    };
    return (
      <AdminContext.Provider
        value={{
          fetchSystemStatus: () => {},
          fetchPendingPlugins,
          pendingPlugins,
          systemStatus: null as any,
        }}
      >
        <MarketplaceAppContainer />
      </AdminContext.Provider>
    );
  }

  return renderApp('admin/marketplace', <Wrapper />, {
    appState: mockAppState({
      edition: EditionKey.community,
      standalone: true,
      settings: {
        [GlobalSettingKeys.UpdatecenterActivated]: 'true',
      },
      ...appStateOverrides,
    }),
  });
}
