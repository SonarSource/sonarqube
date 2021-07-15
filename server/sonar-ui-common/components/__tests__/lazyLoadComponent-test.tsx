/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from '../../helpers/testUtils';
import { lazyLoadComponent } from '../lazyLoadComponent';

const factory = jest.fn().mockImplementation(() => import('../controls/Checkbox'));

beforeEach(() => {
  factory.mockClear();
});

it('should lazy load and display the component', async () => {
  const LazyComponent = lazyLoadComponent(factory);
  const wrapper = mount(<LazyComponent />);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.render()).toMatchSnapshot();
  expect(factory).toBeCalledTimes(1);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.render()).toMatchSnapshot();
  expect(factory).toBeCalledTimes(1);
});

it('should correctly handle import errors', () => {
  const LazyComponent = lazyLoadComponent(factory);
  const wrapper = mount(<LazyComponent />);
  wrapper.find('Suspense').simulateError({ request: 'test' });
  expect(wrapper.find('Alert').exists()).toBe(true);
});

it('should correctly set given display name', () => {
  const LazyComponent = lazyLoadComponent(factory, 'CustomDisplayName');
  const wrapper = shallow(
    <div>
      <LazyComponent />
    </div>
  );
  expect(wrapper).toMatchSnapshot();
});
