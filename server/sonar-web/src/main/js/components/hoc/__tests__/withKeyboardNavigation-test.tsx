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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { KEYCODE_MAP, keydown } from 'sonar-ui-common/helpers/testUtils';
import { mockComponent } from '../../../helpers/testMocks';
import withKeyboardNavigation, { WithKeyboardNavigationProps } from '../withKeyboardNavigation';

class X extends React.Component<{
  components?: T.ComponentMeasure[];
  selected?: T.ComponentMeasure;
}> {
  render() {
    return <div />;
  }
}

const WrappedComponent = withKeyboardNavigation(X);

const COMPONENTS = [
  mockComponent({ key: 'file-1' }),
  mockComponent({ key: 'file-2' }),
  mockComponent({ key: 'file-3' })
];

jest.mock('keymaster', () => {
  const key: any = (bindKey: string, _: string, callback: Function) => {
    document.addEventListener('keydown', (event: KeyboardEvent) => {
      if (bindKey.split(',').includes(KEYCODE_MAP[event.keyCode])) {
        return callback();
      }
      return true;
    });
  };

  key.setScope = jest.fn();
  key.deleteScope = jest.fn();

  return key;
});

it('should wrap component correctly', () => {
  const wrapper = shallow(applyProps());
  expect(wrapper.find('X').exists()).toBe(true);
});

it('should correctly bind key events for component navigation', () => {
  const onGoToParent = jest.fn();
  const onHighlight = jest.fn(selected => {
    wrapper.setProps({ selected });
  });
  const onSelect = jest.fn();

  const wrapper = mount(
    applyProps({
      cycle: true,
      onGoToParent,
      onHighlight,
      onSelect,
      selected: COMPONENTS[1]
    })
  );

  keydown('down');
  expect(onHighlight).toBeCalledWith(COMPONENTS[2]);
  expect(onSelect).not.toBeCalled();

  keydown('up');
  keydown('up');
  expect(onHighlight).toBeCalledWith(COMPONENTS[0]);
  expect(onSelect).not.toBeCalled();

  keydown('up');
  expect(onHighlight).toBeCalledWith(COMPONENTS[2]);

  keydown('down');
  expect(onHighlight).toBeCalledWith(COMPONENTS[0]);

  keydown('right');
  expect(onSelect).toBeCalledWith(COMPONENTS[0]);

  keydown('enter');
  expect(onSelect).toBeCalledWith(COMPONENTS[0]);

  keydown('left');
  expect(onGoToParent).toBeCalled();
});

it('should support not cycling through elements, and triggering a callback on reaching the last element', () => {
  const onEndOfList = jest.fn();
  const onHighlight = jest.fn(selected => {
    wrapper.setProps({ selected });
  });

  const wrapper = mount(
    applyProps({
      onEndOfList,
      onHighlight
    })
  );

  keydown('down');
  expect(onHighlight).toBeCalledWith(COMPONENTS[0]);
  keydown('down');
  keydown('down');
  keydown('down');
  expect(onHighlight).toBeCalledWith(COMPONENTS[2]);
  expect(onEndOfList).toBeCalled();

  keydown('up');
  keydown('up');
  keydown('up');
  keydown('up');
  expect(onHighlight).toBeCalledWith(COMPONENTS[0]);
});

it('should correctly bind key events for codeview navigation', () => {
  const onGoToParent = jest.fn();
  const onHighlight = jest.fn();
  const onSelect = jest.fn();

  mount(
    applyProps({
      isFile: true,
      onGoToParent,
      onHighlight,
      onSelect,
      selected: COMPONENTS[1]
    })
  );

  expect(onHighlight).not.toBeCalled();

  keydown('down');
  expect(onHighlight).not.toBeCalled();

  keydown('up');
  expect(onHighlight).not.toBeCalled();

  keydown('right');
  expect(onSelect).not.toBeCalled();

  keydown('enter');
  expect(onSelect).not.toBeCalled();

  keydown('left');
  expect(onGoToParent).toBeCalled();
});

function applyProps(props: Partial<WithKeyboardNavigationProps> = {}) {
  return <WrappedComponent components={COMPONENTS} {...props} />;
}
