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
import { shallow, mount } from 'enzyme';
import ClipboardButton from '../ClipboardButton';

const constructor = jest.fn();
const destroy = jest.fn();
const on = jest.fn();

jest.mock(
  'clipboard',
  () =>
    function(...args: any) {
      constructor(...args);
      return {
        destroy,
        on
      };
    }
);

jest.useFakeTimers();

it('should display correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  wrapper.instance().showTooltip();
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
  jest.runAllTimers();
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('should render a custom label if provided', () => {
  expect(shallowRender({ label: 'Foo Bar' })).toMatchSnapshot();
});

it('should allow its content to be copied', () => {
  const wrapper = mountRender();
  const button = wrapper.find('button').getDOMNode();
  const instance = wrapper.instance();

  expect(constructor).toBeCalledWith(button);
  expect(on).toBeCalledWith('success', instance.showTooltip);

  jest.clearAllMocks();

  wrapper.setProps({ label: 'Some new label' });
  expect(destroy).toBeCalled();
  expect(constructor).toBeCalledWith(button);
  expect(on).toBeCalledWith('success', instance.showTooltip);

  jest.clearAllMocks();

  wrapper.unmount();
  expect(destroy).toBeCalled();
});

function shallowRender(props: Partial<ClipboardButton['props']> = {}) {
  return shallow<ClipboardButton>(createComponent(props));
}

function mountRender(props: Partial<ClipboardButton['props']> = {}) {
  return mount<ClipboardButton>(createComponent(props));
}

function createComponent(props: Partial<ClipboardButton['props']> = {}) {
  return <ClipboardButton copyValue="foo" {...props} />;
}
