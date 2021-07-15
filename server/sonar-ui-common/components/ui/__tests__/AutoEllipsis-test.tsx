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
import AutoEllipsis, { defaultShouldEllipsis } from '../AutoEllipsis';

it('should render', () => {
  const wrapper = shallow(
    <AutoEllipsis maxWidth={5} useParent={false}>
      <span className="medium">my test text</span>
    </AutoEllipsis>
  );

  expect(wrapper).toMatchSnapshot();
});

it('should render with text-ellipsis class', () => {
  const wrapper = mount(
    <AutoEllipsis customShouldEllipsis={() => true} maxWidth={5} useParent={false}>
      <span className="medium">my test text</span>
    </AutoEllipsis>
  );

  expect(wrapper.find('span').hasClass('medium')).toBe(true);
  expect(wrapper.find('span').hasClass('text-ellipsis')).toBe(true);
});

const node5 = { clientWidth: 5, clientHeight: 5 } as any;
const node10 = { clientWidth: 10, clientHeight: 10 } as any;
const nodeParentSmaller = { ...node10, parentElement: node5 };
const nodeParentBigger = { ...node5, parentElement: node10 };

it('should correctly compute the auto-ellipsis', () => {
  expect(defaultShouldEllipsis(node10, { maxWidth: 5, useParent: false })).toBe(true);
  expect(defaultShouldEllipsis(node10, { maxHeight: 5, useParent: false })).toBe(true);
  expect(defaultShouldEllipsis(node10, { maxWidth: 5, maxHeight: 5, useParent: false })).toBe(true);
  expect(defaultShouldEllipsis(node10, { maxWidth: 5, maxHeight: 10, useParent: false })).toBe(
    true
  );
  expect(defaultShouldEllipsis(node10, { maxWidth: 10, maxHeight: 5, useParent: false })).toBe(
    true
  );
  expect(defaultShouldEllipsis(node10, { maxWidth: 10, useParent: false })).toBe(false);
  expect(defaultShouldEllipsis(node10, { maxHeight: 10, useParent: false })).toBe(false);

  expect(defaultShouldEllipsis(nodeParentSmaller, { maxWidth: 10, useParent: false })).toBe(false);
  expect(defaultShouldEllipsis(nodeParentSmaller, { maxHeight: 10, useParent: false })).toBe(false);
});

it('should correctly compute the auto-ellipsis with a parent node', () => {
  expect(defaultShouldEllipsis(nodeParentSmaller, {})).toBe(true);
  expect(defaultShouldEllipsis(nodeParentSmaller, { maxWidth: 10 })).toBe(true);
  expect(defaultShouldEllipsis(nodeParentSmaller, { maxHeight: 10 })).toBe(true);
  expect(defaultShouldEllipsis(nodeParentSmaller, { maxWidth: 10, maxHeight: 10 })).toBe(false);
  expect(defaultShouldEllipsis(nodeParentBigger, {})).toBe(false);
  expect(defaultShouldEllipsis(nodeParentBigger, { maxWidth: 2 })).toBe(true);
  expect(defaultShouldEllipsis(nodeParentBigger, { maxHeight: 2 })).toBe(true);
});
