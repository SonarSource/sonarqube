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
import { shallow } from 'enzyme';
import * as React from 'react';
import PluginVersionMetaData from '../PluginVersionMetaData';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should correctly show all versions', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('.plugin-meta-data-version').length).toBe(2);
  wrapper.instance().setState({ collapsed: false });
  expect(wrapper.find('.plugin-meta-data-version').length).toBe(5);
});

function shallowRender(props: Partial<PluginVersionMetaData['props']> = {}) {
  return shallow(
    <PluginVersionMetaData
      versions={[
        {
          version: '5.13',
          date: '2019-05-31',
          compatibility: '6.7',
          archived: false,
          downloadURL: 'https://example.com/sonar-java-plugin-5.13.0.18197.jar',
          changeLogUrl: 'https://example.com/sonar-java-plugin/release'
        },
        {
          version: '4.2',
          archived: false,
          downloadURL: 'https://example.com/sonar-java-plugin-5.13.0.18197.jar'
        },
        {
          version: '3.2',
          date: '2015-04-30',
          compatibility: '6.0 to 7.1',
          archived: true,
          changeLogUrl: 'https://example.com/sonar-java-plugin/release',
          downloadURL: 'https://example.com/sonar-java-plugin-3.2.jar'
        },
        {
          version: '3.1',
          description: 'Lorem ipsum dolor sit amet',
          archived: true,
          changeLogUrl: 'https://example.com/sonar-java-plugin/release',
          downloadURL: 'https://example.com/sonar-java-plugin-3.1.jar'
        },
        {
          version: '2.1',
          archived: true,
          downloadURL: 'https://example.com/sonar-java-plugin-2.1.jar'
        }
      ]}
      {...props}
    />
  );
}
