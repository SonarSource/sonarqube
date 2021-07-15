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
import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from '../../../helpers/testUtils';
import SelectListListElement from '../SelectListListElement';

const listElement = (
  <SelectListListElement
    element="foo"
    key="foo"
    onSelect={jest.fn(() => Promise.resolve())}
    onUnselect={jest.fn(() => Promise.resolve())}
    renderElement={(foo: string) => foo}
    selected={false}
  />
);

it('should display a loader when checking', async () => {
  const wrapper = shallow<SelectListListElement>(listElement);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.state().loading).toBe(false);

  (wrapper.instance() as SelectListListElement).handleCheck(true);
  expect(wrapper.state().loading).toBe(true);
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toBe(false);
});
