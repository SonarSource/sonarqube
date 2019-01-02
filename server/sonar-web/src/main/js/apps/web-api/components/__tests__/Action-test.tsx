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
import Action from '../Action';

const ACTION = {
  key: 'foo',
  changelog: [{ description: 'Changelog desc', version: '5.0' }],
  description: 'Foo Desc',
  hasResponseExample: true,
  internal: false,
  params: [
    {
      key: 'param',
      description: 'Param desc',
      internal: true,
      required: true
    }
  ],
  post: false
};
const DOMAIN = {
  actions: [ACTION],
  path: 'foo',
  description: 'API Foo',
  deprecated: false,
  internal: false
};

const PROPS = {
  action: ACTION,
  domain: DOMAIN,
  showDeprecated: false,
  showInternal: false
};

it('should render correctly', () => {
  expect(shallow(<Action {...PROPS} />)).toMatchSnapshot();
});

it('should display the params', () => {
  const wrapper = shallow(<Action {...PROPS} />);
  wrapper.setState({ showParams: true });
  expect(wrapper.find('Params')).toMatchSnapshot();
});

it('should display the response example', () => {
  const wrapper = shallow(<Action {...PROPS} />);
  wrapper.setState({ showResponse: true });
  expect(wrapper.find('ResponseExample')).toMatchSnapshot();
});

it('should display the changelog', () => {
  const wrapper = shallow(<Action {...PROPS} />);
  wrapper.setState({ showChangelog: true });
  expect(wrapper.find('ActionChangelog')).toMatchSnapshot();
});
