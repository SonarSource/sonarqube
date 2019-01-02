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
import SelectList from '../SelectList';
import SelectListItem from '../SelectListItem';
import { click, keydown } from '../../../helpers/testUtils';

it('should render correctly without children', () => {
  const onSelect = jest.fn();
  expect(
    shallow(
      <SelectList
        currentItem="seconditem"
        items={['item', 'seconditem', 'third']}
        onSelect={onSelect}
      />
    )
  ).toMatchSnapshot();
});

it('should render correctly with children', () => {
  const onSelect = jest.fn();
  const items = ['item', 'seconditem', 'third'];
  expect(
    shallow(
      <SelectList currentItem="seconditem" items={items} onSelect={onSelect}>
        {items.map(item => (
          <SelectListItem item={item} key={item}>
            <i className="myicon" />
            item
          </SelectListItem>
        ))}
      </SelectList>
    )
  ).toMatchSnapshot();
});

it('should correclty handle user actions', () => {
  const onSelect = jest.fn();
  const items = ['item', 'seconditem', 'third'];
  const list = mount(
    <SelectList currentItem="seconditem" items={items} onSelect={onSelect}>
      {items.map(item => (
        <SelectListItem item={item} key={item}>
          <i className="myicon" />
          item
        </SelectListItem>
      ))}
    </SelectList>
  );
  keydown(40);
  expect(list.state()).toMatchSnapshot();
  keydown(40);
  expect(list.state()).toMatchSnapshot();
  keydown(38);
  expect(list.state()).toMatchSnapshot();
  click(list.find('a').at(2));
  expect(onSelect.mock.calls).toMatchSnapshot(); // eslint-disable-linelist
});
