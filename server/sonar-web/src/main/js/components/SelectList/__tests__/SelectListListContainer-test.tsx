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
import SelectListListContainer from '../SelectListListContainer';
import { Filter } from '../SelectList';

const elementsContainer = (
  <SelectListListContainer
    disabledElements={[]}
    elements={['foo', 'bar', 'baz']}
    filter={Filter.All}
    onSelect={jest.fn(() => Promise.resolve())}
    onUnselect={jest.fn(() => Promise.resolve())}
    renderElement={(foo: string) => foo}
    selectedElements={['foo']}
  />
);

it('should display elements based on filters', () => {
  const wrapper = shallow(elementsContainer);
  expect(wrapper.find('SelectListListElement')).toHaveLength(3);
  expect(wrapper).toMatchSnapshot();

  wrapper.setProps({ filter: Filter.Unselected });
  expect(wrapper.find('SelectListListElement')).toHaveLength(2);
  expect(wrapper).toMatchSnapshot();

  wrapper.setProps({ filter: Filter.Selected });
  expect(wrapper.find('SelectListListElement')).toHaveLength(1);
  expect(wrapper).toMatchSnapshot();
});
