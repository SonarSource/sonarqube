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
import { shallow } from 'enzyme';
import * as React from 'react';
import SelectListItem from '../SelectListItem';

it('should render correctly without children', () => {
  expect(shallow(<SelectListItem item="myitem" />)).toMatchSnapshot();
});

it('should render correctly with children', () => {
  expect(
    shallow(
      <SelectListItem active="myitem" item="seconditem">
        <i className="custom-icon" />
        <p>seconditem</p>
      </SelectListItem>
    )
  ).toMatchSnapshot();
});

it('should render correctly with a tooltip', () => {
  expect(shallow(<SelectListItem item="myitem" title="my custom tooltip" />)).toMatchSnapshot();
});

it('should render with the active class', () => {
  expect(shallow(<SelectListItem active="myitem" item="myitem" />)).toMatchSnapshot();
});
