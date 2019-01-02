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
import Tabs, { Tab } from '../Tabs';
import { click } from '../../../helpers/testUtils';

it('should render correctly', () => {
  const wrapper = shallow(
    <Tabs
      onChange={jest.fn()}
      selected={'bar'}
      tabs={[{ key: 'foo', node: 'Foo' }, { key: 'bar', node: 'Bar' }]}
    />
  );

  expect(wrapper).toMatchSnapshot();
});

it('should switch tabs', () => {
  const onChange = jest.fn();
  const wrapper = shallow(
    <Tabs
      onChange={onChange}
      selected={'bar'}
      tabs={[{ key: 'foo', node: 'Foo' }, { key: 'bar', node: 'Bar' }]}
    />
  );

  click(shallow(wrapper.find('Tab').get(0)).find('.js-foo'));
  expect(onChange).toBeCalledWith('foo');
  click(shallow(wrapper.find('Tab').get(1)).find('.js-bar'));
  expect(onChange).toBeCalledWith('bar');
});

it('should render single tab correctly', () => {
  const onSelect = jest.fn();
  const wrapper = shallow(
    <Tab name="foo" onSelect={onSelect} selected={true}>
      <span>Foo</span>
    </Tab>
  );
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('a'));
  expect(onSelect).toBeCalledWith('foo');
});

it('should disable single tab', () => {
  const onSelect = jest.fn();
  const wrapper = shallow(
    <Tab disabled={true} name="foo" onSelect={onSelect} selected={true}>
      <span>Foo</span>
    </Tab>
  );
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('a'));
  expect(onSelect).not.toBeCalled();
});
