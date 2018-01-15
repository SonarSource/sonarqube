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
import * as React from 'react';
import { shallow } from 'enzyme';
import PageActions from '../PageActions';
import { click } from '../../../../helpers/testUtils';

jest.mock('../../utils', () => ({
  getFileNameSuffix: (suffix?: string) => `filesuffix(${suffix || ''})`
}));

it('should render correctly', () => {
  expect(getWrapper({ serverId: 'MyServerId' })).toMatchSnapshot();
});

it('should render without restart and log download', () => {
  expect(
    getWrapper({ canDownloadLogs: false, canRestart: false, cluster: true })
  ).toMatchSnapshot();
});

it('should open restart modal', () => {
  const wrapper = getWrapper();
  click(wrapper.find('#restart-server-button'));
  expect(wrapper.find('RestartForm')).toHaveLength(1);
});

it('should open change log level modal', () => {
  const wrapper = getWrapper();
  click(wrapper.find('#edit-logs-level-button'));
  expect(wrapper.find('ChangeLogLevelForm')).toHaveLength(1);
});

function getWrapper(props = {}) {
  return shallow(
    <PageActions
      canDownloadLogs={true}
      canRestart={true}
      cluster={false}
      logLevel="INFO"
      onLogLevelChange={() => {}}
      {...props}
    />
  );
}
