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
import { KeyboardCodes } from '../../../helpers/keycodes';
import { click, KEYCODE_MAP, keydown } from '../../../helpers/testUtils';
import SelectList from '../SelectList';
import SelectListItem from '../SelectListItem';

jest.mock('keymaster', () => {
  const key: any = (bindKey: string, _: string, callback: Function) => {
    document.addEventListener('keydown', (event: KeyboardEvent) => {
      const keymasterCode = event.code && KEYCODE_MAP[event.code as KeyboardCodes];
      if (keymasterCode && bindKey.split(',').includes(keymasterCode)) {
        return callback();
      }
      return true;
    });
  };
  let scope = 'key-scope';

  key.getScope = () => scope;
  key.setScope = (newScope: string) => {
    scope = newScope;
  };
  key.deleteScope = jest.fn();

  return key;
});

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
  const list = mount<SelectList>(
    <SelectList currentItem="seconditem" items={items} onSelect={onSelect}>
      {items.map(item => (
        <SelectListItem item={item} key={item}>
          <i className="myicon" />
          item
        </SelectListItem>
      ))}
    </SelectList>
  );
  expect(list.state().active).toBe('seconditem');
  keydown({ code: KeyboardCodes.DownArrow });
  expect(list.state().active).toBe('third');
  keydown({ code: KeyboardCodes.DownArrow });
  expect(list.state().active).toBe('item');
  keydown({ code: KeyboardCodes.UpArrow });
  expect(list.state().active).toBe('third');
  keydown({ code: KeyboardCodes.UpArrow });
  expect(list.state().active).toBe('seconditem');
  click(list.find('a').at(2));
  expect(onSelect).toBeCalledWith('third');
});
