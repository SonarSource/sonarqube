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
  mockAvailablePlugin,
  mockInstalledPlugin,
  mockPlugin,
  mockUpdate,
} from '../../../../helpers/mocks/plugins';
import PluginAvailable, { PluginAvailableProps } from '../PluginAvailable';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ readOnly: true })).toMatchSnapshot('read only');
  expect(
    shallowRender({
      plugin: mockAvailablePlugin({
        update: mockUpdate({ requires: [mockPlugin()] }),
      }),
    })
  ).toMatchSnapshot('has requirements');
  const installed = mockInstalledPlugin({ key: 'sonar-bar', name: 'Sonar Bar' });
  expect(
    shallowRender({
      installedPlugins: [installed],
      plugin: mockAvailablePlugin({
        update: mockUpdate({
          requires: [mockPlugin(), installed],
        }),
      }),
    })
  ).toMatchSnapshot('has requirements, some of them already met');
});

function shallowRender(props: Partial<PluginAvailableProps> = {}) {
  return shallow<PluginAvailableProps>(
    <PluginAvailable
      installedPlugins={[]}
      plugin={mockAvailablePlugin()}
      readOnly={false}
      refreshPending={jest.fn()}
      {...props}
    />
  );
}
