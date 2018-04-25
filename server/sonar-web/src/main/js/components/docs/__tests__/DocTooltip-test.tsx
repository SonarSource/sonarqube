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
import DocTooltip from '../DocTooltip';
import { click } from '../../../helpers/testUtils';

jest.useFakeTimers();

it('should render', () => {
  const wrapper = shallow(<DocTooltip doc="foo/bar" />);
  wrapper.setState({ content: 'this is *bold* text', open: true, loading: true });
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ loading: false });
  expect(wrapper).toMatchSnapshot();
});

it('should reset state when receiving new doc', () => {
  const wrapper = shallow(<DocTooltip doc="foo/bar" />);
  wrapper.setState({ content: 'this is *bold* text', open: true });
  wrapper.setProps({ doc: 'baz' });
  expect(wrapper.state()).toEqual({ content: undefined, loading: false, open: false });
});

it('should toggle', () => {
  const wrapper = shallow(<DocTooltip doc="foo/bar" />);
  expect(wrapper.state('open')).toBe(false);
  click(wrapper.find('a'));
  jest.runAllTimers();
  expect(wrapper.state('open')).toBe(true);
});
