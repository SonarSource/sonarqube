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
import { shallow } from 'enzyme';
import * as React from 'react';
import {
  getAvailablePlugins,
  getInstalledPlugins,
  getInstalledPluginsWithUpdates,
  getPluginUpdates,
} from '../../../api/plugins';
import { getValue, setSimpleSettingValue } from '../../../api/settings';
import { mockLocation, mockRouter } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { EditionKey } from '../../../types/editions';
import { RiskConsent } from '../../../types/plugins';
import { SettingsKey } from '../../../types/settings';
import { App } from '../App';

jest.mock('../../../api/plugins', () => {
  const plugin = jest.requireActual('../../../helpers/mocks/plugins').mockPlugin();

  return {
    getAvailablePlugins: jest.fn().mockResolvedValue({ plugins: [plugin] }),
    getInstalledPlugins: jest.fn().mockResolvedValue([]),
    getInstalledPluginsWithUpdates: jest.fn().mockResolvedValue([]),
    getPluginUpdates: jest.fn().mockResolvedValue([]),
  };
});

jest.mock('../../../api/settings', () => ({
  getValue: jest.fn().mockResolvedValue({}),
  setSimpleSettingValue: jest.fn().mockResolvedValue(true),
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('loaded');

  wrapper.setProps({ currentEdition: EditionKey.community, standaloneMode: true });
  wrapper.setState({ riskConsent: RiskConsent.Accepted });

  expect(wrapper).toMatchSnapshot('not readonly');
});

it('should handle accepting the risk', async () => {
  (getValue as jest.Mock)
    .mockResolvedValueOnce({ value: RiskConsent.NotAccepted })
    .mockResolvedValueOnce({ value: RiskConsent.Accepted });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(getValue).toHaveBeenCalledWith({ key: SettingsKey.PluginRiskConsent });

  wrapper.instance().acknowledgeRisk();

  await new Promise(setImmediate);

  expect(setSimpleSettingValue).toHaveBeenCalled();
  expect(getValue).toHaveBeenCalledWith({ key: SettingsKey.PluginRiskConsent });
  expect(wrapper.state().riskConsent).toBe(RiskConsent.Accepted);
});

it('should fetch plugin info', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(getInstalledPluginsWithUpdates).toHaveBeenCalled();
  expect(getAvailablePlugins).toHaveBeenCalled();

  wrapper.setProps({ location: mockLocation({ query: { filter: 'updates' } }) });
  await waitAndUpdate(wrapper);
  expect(getPluginUpdates).toHaveBeenCalled();

  wrapper.setProps({ location: mockLocation({ query: { filter: 'installed' } }) });
  await waitAndUpdate(wrapper);
  expect(getInstalledPlugins).toHaveBeenCalled();
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      currentEdition={EditionKey.developer}
      fetchPendingPlugins={jest.fn()}
      location={mockLocation()}
      pendingPlugins={{
        installing: [],
        updating: [],
        removing: [],
      }}
      router={mockRouter()}
      updateCenterActive={false}
      {...props}
    />
  );
}
