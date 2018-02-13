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
import { shallow, ShallowRendererProps, ShallowWrapper, ReactWrapper } from 'enzyme';
import { IntlProvider } from 'react-intl';

export const mockEvent = {
  target: { blur() {} },
  currentTarget: { blur() {} },
  preventDefault() {},
  stopPropagation() {}
};

export function click(element: ShallowWrapper | ReactWrapper, event = {}): void {
  element.simulate('click', { ...mockEvent, ...event });
}

export function clickOutside(event = {}): void {
  const dispatchedEvent = new MouseEvent('click', event);
  window.dispatchEvent(dispatchedEvent);
}

export function submit(element: ShallowWrapper): void {
  element.simulate('submit', {
    preventDefault() {}
  });
}

export function change(element: ShallowWrapper, value: string, event = {}): void {
  element.simulate('change', {
    target: { value },
    currentTarget: { value },
    ...event
  });
}

export function keydown(keyCode: number): void {
  const event = new KeyboardEvent('keydown', { keyCode } as KeyboardEventInit);
  document.dispatchEvent(event);
}

export function elementKeydown(element: ShallowWrapper, keyCode: number): void {
  const event = {
    currentTarget: { element },
    keyCode,
    preventDefault() {}
  };

  if (typeof element.type() === 'string') {
    // `type()` is string for native dom elements
    element.simulate('keydown', event);
  } else {
    element.prop<Function>('onKeyDown')(event);
  }
}

export function doAsync(fn?: Function): Promise<void> {
  return new Promise(resolve => {
    setImmediate(() => {
      if (fn) {
        fn();
      }
      resolve();
    });
  });
}

// Create the IntlProvider to retrieve context for wrapping around.
const intlProvider = new IntlProvider({ locale: 'en' }, {});
const { intl } = intlProvider.getChildContext();
export function shallowWithIntl(node: React.ReactElement<any>, options: ShallowRendererProps = {}) {
  return shallow(node, { ...options, context: { intl, ...options.context } });
}

export async function waitAndUpdate(wrapper: ShallowWrapper<any, any> | ReactWrapper<any, any>) {
  await new Promise(setImmediate);
  wrapper.update();
}
