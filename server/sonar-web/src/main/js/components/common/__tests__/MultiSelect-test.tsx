/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { mockEvent } from '../../../helpers/testUtils';
import MultiSelect, { MultiSelectProps } from '../MultiSelect';

const props = {
  selectedElements: ['bar'],
  elements: [],
  onSearch: () => Promise.resolve(),
  onSelect: () => {},
  onUnselect: () => {},
  renderLabel: (element: string) => element,
  placeholder: ''
};

const elements = ['foo', 'bar', 'baz'];

it('should render multiselect with selected elements', () => {
  const multiselect = shallowRender();
  // Will not only the selected element
  expect(multiselect).toMatchSnapshot();

  multiselect.setProps({ elements });
  expect(multiselect).toMatchSnapshot();
  multiselect.setState({ activeIdx: 2 });
  expect(multiselect).toMatchSnapshot();
  multiselect.setState({ query: 'test' });
  expect(multiselect).toMatchSnapshot();
});

it('should render with the focus inside the search input', () => {
  /*
   * Need to attach to document body to have it set to `document.activeElement`
   * See: https://github.com/jsdom/jsdom/issues/2723#issuecomment-580163361
   */
  const container = document.createElement('div');
  document.body.appendChild(container);
  const multiselect = mount(<MultiSelect {...props} />, { attachTo: container });

  expect(multiselect.find('input').getDOMNode()).toBe(document.activeElement);

  multiselect.unmount();
});

it.each([
  [KeyboardKeys.DownArrow, 1, 1],
  [KeyboardKeys.UpArrow, 1, 1],
  [KeyboardKeys.LeftArrow, 1, 0]
])('should handle keyboard event: %s', (key, stopPropagationCalls, preventDefaultCalls) => {
  const wrapper = shallowRender();

  const stopPropagation = jest.fn();
  const preventDefault = jest.fn();
  const event = mockEvent({ preventDefault, stopPropagation, key });

  wrapper.instance().handleKeyboard(event);

  expect(stopPropagation).toBeCalledTimes(stopPropagationCalls);
  expect(preventDefault).toBeCalledTimes(preventDefaultCalls);
});

it('should handle keyboard event: enter', () => {
  const wrapper = shallowRender();

  wrapper.instance().toggleSelect = jest.fn();

  wrapper.instance().handleKeyboard(mockEvent({ key: KeyboardKeys.Enter }));

  expect(wrapper.instance().toggleSelect).toBeCalled();
});

function shallowRender(overrides: Partial<MultiSelectProps> = {}) {
  return shallow<MultiSelect>(
    <MultiSelect
      selectedElements={['bar']}
      elements={[]}
      onSearch={() => Promise.resolve()}
      onSelect={jest.fn()}
      onUnselect={jest.fn()}
      renderLabel={(element: string) => element}
      placeholder=""
      {...overrides}
    />
  );
}
