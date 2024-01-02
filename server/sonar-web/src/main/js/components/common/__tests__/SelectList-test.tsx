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
import { shallow } from 'enzyme';
import * as React from 'react';
import SelectList from '../SelectList';
import SelectListItem from '../SelectListItem';

it('should render correctly without children', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly with children', () => {
  const items = ['item', 'seconditem', 'third'];
  const children = items.map((item) => (
    <SelectListItem item={item} key={item}>
      <i className="myicon" />
      item
    </SelectListItem>
  ));
  const wrapper = shallowRender({ items }, children);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<SelectList['props']> = {}, children?: React.ReactNode) {
  return shallow<SelectList>(
    <SelectList
      currentItem="seconditem"
      items={['item', 'seconditem', 'third']}
      onSelect={jest.fn()}
      {...props}
    >
      {children}
    </SelectList>
  );
}
