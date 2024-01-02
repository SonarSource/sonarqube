/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { KeyboardKeys } from '../../../helpers/keycodes';
import { mockComponent } from '../../../helpers/mocks/component';
import { keydown } from '../../../helpers/testUtils';
import { ComponentMeasure } from '../../../types/types';
import withKeyboardNavigation, { WithKeyboardNavigationProps } from '../withKeyboardNavigation';

class X extends React.Component<{
  components?: ComponentMeasure[];
  selected?: ComponentMeasure;
}> {
  render() {
    return <div />;
  }
}

const WrappedComponent = withKeyboardNavigation(X);

const COMPONENTS = [
  mockComponent({ key: 'file-1' }),
  mockComponent({ key: 'file-2' }),
  mockComponent({ key: 'file-3' }),
];

it('should wrap component correctly', () => {
  const wrapper = shallow(applyProps());
  expect(wrapper.find('X').exists()).toBe(true);
});

it('should correctly bind key events for component navigation', () => {
  const onGoToParent = jest.fn();
  const onHighlight = jest.fn((selected) => {
    wrapper.setProps({ selected });
  });
  const onSelect = jest.fn();

  const wrapper = mount(
    applyProps({
      cycle: true,
      onGoToParent,
      onHighlight,
      onSelect,
      selected: COMPONENTS[1],
    })
  );

  keydown({ key: KeyboardKeys.DownArrow });
  expect(onHighlight).toHaveBeenCalledWith(COMPONENTS[2]);
  expect(onSelect).not.toHaveBeenCalled();

  keydown({ key: KeyboardKeys.UpArrow });
  keydown({ key: KeyboardKeys.UpArrow });
  expect(onHighlight).toHaveBeenCalledWith(COMPONENTS[0]);
  expect(onSelect).not.toHaveBeenCalled();

  keydown({ key: KeyboardKeys.UpArrow });
  expect(onHighlight).toHaveBeenCalledWith(COMPONENTS[2]);

  keydown({ key: KeyboardKeys.DownArrow });
  expect(onHighlight).toHaveBeenCalledWith(COMPONENTS[0]);

  keydown({ key: KeyboardKeys.RightArrow, metaKey: true });
  expect(onSelect).not.toHaveBeenCalled();
  keydown({ key: KeyboardKeys.RightArrow });
  expect(onSelect).toHaveBeenCalledWith(COMPONENTS[0]);

  keydown({ key: KeyboardKeys.Enter });
  expect(onSelect).toHaveBeenCalledWith(COMPONENTS[0]);

  keydown({ key: KeyboardKeys.LeftArrow, metaKey: true });
  expect(onGoToParent).not.toHaveBeenCalled();
  keydown({ key: KeyboardKeys.LeftArrow });
  expect(onGoToParent).toHaveBeenCalled();
});

it('should support not cycling through elements, and triggering a callback on reaching the last element', () => {
  const onEndOfList = jest.fn();
  const onHighlight = jest.fn((selected) => {
    wrapper.setProps({ selected });
  });

  const wrapper = mount(
    applyProps({
      onEndOfList,
      onHighlight,
    })
  );

  keydown({ key: KeyboardKeys.DownArrow });
  expect(onHighlight).toHaveBeenCalledWith(COMPONENTS[0]);
  keydown({ key: KeyboardKeys.DownArrow });
  keydown({ key: KeyboardKeys.DownArrow });
  keydown({ key: KeyboardKeys.DownArrow });
  expect(onHighlight).toHaveBeenCalledWith(COMPONENTS[2]);
  expect(onEndOfList).toHaveBeenCalled();

  keydown({ key: KeyboardKeys.UpArrow });
  keydown({ key: KeyboardKeys.UpArrow });
  keydown({ key: KeyboardKeys.UpArrow });
  keydown({ key: KeyboardKeys.UpArrow });
  expect(onHighlight).toHaveBeenCalledWith(COMPONENTS[0]);
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
      selected: COMPONENTS[1],
    })
  );

  expect(onHighlight).not.toHaveBeenCalled();

  keydown({ key: KeyboardKeys.DownArrow });
  expect(onHighlight).not.toHaveBeenCalled();

  keydown({ key: KeyboardKeys.UpArrow });
  expect(onHighlight).not.toHaveBeenCalled();

  keydown({ key: KeyboardKeys.RightArrow });
  expect(onSelect).not.toHaveBeenCalled();

  keydown({ key: KeyboardKeys.Enter });
  expect(onSelect).not.toHaveBeenCalled();

  keydown({ key: KeyboardKeys.LeftArrow });
  expect(onGoToParent).toHaveBeenCalled();
});

function applyProps(props: Partial<WithKeyboardNavigationProps> = {}) {
  return <WrappedComponent components={COMPONENTS} {...props} />;
}
