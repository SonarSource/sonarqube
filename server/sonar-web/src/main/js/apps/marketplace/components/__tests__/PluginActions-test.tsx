/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import PluginActions from '../PluginActions';
import { PluginInstalled, PluginAvailable } from '../../../../api/plugins';

const installedPlugin: PluginInstalled = {
  key: 'foo',
  name: 'Foo',
  filename: 'foo.zip',
  hash: '',
  implementationBuild: '',
  sonarLintSupported: true,
  termsAndConditionsUrl: 'https://url',
  updatedAt: 1,
  updates: [{ status: 'COMPATIBLE', requires: [] }],
  version: '7.7'
};

const availablePlugin: PluginAvailable = {
  key: 'foo',
  name: 'Foo',
  release: { version: '7.7', date: 'date' },
  termsAndConditionsUrl: 'https://url',
  update: { status: 'COMPATIBLE', requires: [] }
};

it('should render installed plugin correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ plugin: { ...installedPlugin, editionBundled: true } })).toMatchSnapshot();
});

it('should render available plugin correctly', () => {
  expect(shallowRender({ plugin: availablePlugin })).toMatchSnapshot();
  expect(shallowRender({ plugin: { ...availablePlugin, editionBundled: true } })).toMatchSnapshot();
});

function shallowRender(props: Partial<PluginActions['props']> = {}) {
  return shallow(<PluginActions plugin={installedPlugin} refreshPending={jest.fn()} {...props} />);
}
