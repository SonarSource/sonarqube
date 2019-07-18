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
import { mount } from 'enzyme';
import * as React from 'react';
import PluginMetaData from '../PluginMetaData';
import { getPluginMetaData } from '../utils';

jest.mock('../utils', () => ({
  getPluginMetaData: jest.fn().mockResolvedValue({
    name: 'SonarJava',
    key: 'java',
    isSonarSourceCommercial: true,
    organization: {
      name: 'SonarSource',
      url: 'http://www.sonarsource.com/'
    },
    category: 'Languages',
    license: 'SonarSource',
    issueTrackerURL: 'https://jira.sonarsource.com/browse/SONARJAVA',
    sourcesURL: 'https://github.com/SonarSource/sonar-java',
    versions: [
      {
        version: '4.2',
        compatibilityRange: { minimum: '6.0', maximum: '6.6' },
        archived: false,
        downloadURL: 'https://example.com/sonar-java-plugin-5.13.0.18197.jar'
      },
      {
        version: '3.2',
        date: '2015-04-30',
        compatibilityRange: { maximum: '6.0' },
        archived: true,
        changeLogUrl: 'https://example.com/sonar-java-plugin/release',
        downloadURL: 'https://example.com/sonar-java-plugin-3.2.jar'
      }
    ]
  })
}));

beforeAll(() => {
  (global as any).document.body.innerHTML = `
<div class="page-container">
  <p>Lorem ipsum</p>
  <!-- update_center:java -->
  <p>Dolor sit amet</p>
  <!-- update_center : python -->
  <p>Foo Bar</p>
  <!--update_center       :       abap-->
</div>
`;
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await new Promise(setImmediate);
  expect(wrapper).toMatchSnapshot();
  expect(getPluginMetaData).toBeCalledWith('java');
  expect(getPluginMetaData).toBeCalledWith('python');
  expect(getPluginMetaData).toBeCalledWith('abap');
});

function shallowRender(props: Partial<PluginMetaData['props']> = {}) {
  return mount(<PluginMetaData location={{ pathname: 'foo' }} {...props} />);
}
