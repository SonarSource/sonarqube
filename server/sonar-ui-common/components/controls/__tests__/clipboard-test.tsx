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
import { Button } from '../buttons';
import { ClipboardBase, ClipboardButton, ClipboardIconButton } from '../clipboard';

const constructor = jest.fn();
const destroy = jest.fn();
const on = jest.fn();

jest.mock(
  'clipboard',
  () =>
    function (...args: any) {
      constructor(...args);
      return {
        destroy,
        on,
      };
    }
);

jest.useFakeTimers();

describe('ClipboardBase', () => {
  it('should display correctly', () => {
    const children = jest.fn().mockReturnValue(<Button>copy</Button>);
    const wrapper = shallowRender(children);
    const instance = wrapper.instance();
    expect(wrapper).toMatchSnapshot();
    instance.handleSuccessCopy();
    expect(children).toBeCalledWith({ copySuccess: true, setCopyButton: instance.setCopyButton });
    jest.runAllTimers();
    expect(children).toBeCalledWith({ copySuccess: false, setCopyButton: instance.setCopyButton });
  });

  it('should allow its content to be copied', () => {
    const wrapper = mountRender(({ setCopyButton }) => (
      <Button innerRef={setCopyButton}>click</Button>
    ));
    const button = wrapper.find('button').getDOMNode();
    const instance = wrapper.instance();

    expect(constructor).toBeCalledWith(button);
    expect(on).toBeCalledWith('success', instance.handleSuccessCopy);

    jest.clearAllMocks();

    wrapper.unmount();
    expect(destroy).toBeCalled();
  });

  function shallowRender(children?: ClipboardBase['props']['children']) {
    return shallow<ClipboardBase>(<ClipboardBase>{children}</ClipboardBase>);
  }

  function mountRender(children?: ClipboardBase['props']['children']) {
    return mount<ClipboardBase>(<ClipboardBase>{children}</ClipboardBase>);
  }
});

describe('ClipboardButton', () => {
  it('should display correctly', () => {
    expect(shallowRender()).toMatchSnapshot();
  });

  it('should render a custom label if provided', () => {
    expect(shallowRender('Foo Bar')).toMatchSnapshot();
  });

  function shallowRender(children?: React.ReactNode) {
    return shallow(<ClipboardButton copyValue="foo">{children}</ClipboardButton>).dive();
  }
});

describe('ClipboardIconButton', () => {
  it('should display correctly', () => {
    expect(shallowRender()).toMatchSnapshot();
  });

  function shallowRender() {
    return shallow(<ClipboardIconButton copyValue="foo" />).dive();
  }
});
