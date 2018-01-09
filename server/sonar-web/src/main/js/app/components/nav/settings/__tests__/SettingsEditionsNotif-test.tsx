/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
/* eslint-disable import/order */
import * as React from 'react';
import { mount, shallow } from 'enzyme';
import { click } from '../../../../../helpers/testUtils';
import SettingsEditionsNotif from '../SettingsEditionsNotif';

jest.mock('../../../../../api/marketplace', () => ({
  dismissErrorMessage: jest.fn(() => Promise.resolve())
}));

const dismissMsg = require('../../../../../api/marketplace').dismissErrorMessage as jest.Mock<any>;

beforeEach(() => {
  dismissMsg.mockClear();
});

it('should display an in progress notif', () => {
  const wrapper = shallow(
    <SettingsEditionsNotif
      editionStatus={{ installationStatus: 'AUTOMATIC_IN_PROGRESS' }}
      preventRestart={false}
      setEditionStatus={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should display a ready notification', () => {
  const wrapper = shallow(
    <SettingsEditionsNotif
      editionStatus={{ installationStatus: 'AUTOMATIC_READY' }}
      preventRestart={false}
      setEditionStatus={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should display a manual installation notification', () => {
  const wrapper = shallow(
    <SettingsEditionsNotif
      editionStatus={{ installationStatus: 'MANUAL_IN_PROGRESS', nextEditionKey: 'foo' }}
      editions={[
        {
          key: 'foo',
          name: 'Foo',
          textDescription: 'Foo desc',
          downloadUrl: 'download_url',
          homeUrl: 'more_url',
          licenseRequestUrl: 'license_url'
        }
      ]}
      preventRestart={false}
      setEditionStatus={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should display install errors', () => {
  const wrapper = shallow(
    <SettingsEditionsNotif
      editionStatus={{ installationStatus: 'AUTOMATIC_IN_PROGRESS', installError: 'Foo error' }}
      preventRestart={false}
      setEditionStatus={jest.fn()}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should allow to dismiss install errors', async () => {
  const setEditionStatus = jest.fn();
  const wrapper = mount(
    <SettingsEditionsNotif
      editionStatus={{ installationStatus: 'NONE', installError: 'Foo error' }}
      preventRestart={false}
      setEditionStatus={setEditionStatus}
    />
  );
  click(wrapper.find('button'));
  expect(dismissMsg).toHaveBeenCalled();
  await new Promise(setImmediate);
  expect(setEditionStatus).toHaveBeenCalledWith({
    installationStatus: 'NONE',
    installError: undefined
  });
});

it('should not display the restart button', () => {
  const wrapper = shallow(
    <SettingsEditionsNotif
      editionStatus={{ installationStatus: 'AUTOMATIC_READY' }}
      preventRestart={true}
      setEditionStatus={jest.fn()}
    />
  );
  expect(wrapper.find('button.js-restart').exists()).toBeFalsy();
});

it('should have a link to cluster documentation for datacenter edition', () => {
  const editions = [{ key: 'datacenter' }] as any;
  const wrapper = shallow(
    <SettingsEditionsNotif
      editions={editions}
      editionStatus={{ installationStatus: 'AUTOMATIC_READY', nextEditionKey: 'datacenter' }}
      preventRestart={false}
      setEditionStatus={jest.fn()}
    />
  );
  expect(wrapper.find('FormattedMessage').exists()).toBeTruthy();
});
