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
import PluginChangeLog, { Props } from '../PluginChangeLog';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <PluginChangeLog
      release={{
        version: '0.11',
        date: '2018-11-05',
        description: 'Change version description',
        changeLogUrl: 'https://my.change.log/url'
      }}
      update={{
        previousUpdates: [
          {
            release: {
              version: '0.10',
              date: '2018-06-05',
              description: 'Change version description',
              changeLogUrl: 'https://my.change.log/url'
            },
            requires: [],
            status: 'COMPATIBLE'
          }
        ],
        requires: [{ key: 'java', name: 'SonarJava', description: 'Code Analyzer for Java' }],
        status: 'COMPATIBLE'
      }}
      {...props}
    />
  );
}
