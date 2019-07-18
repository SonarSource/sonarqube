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
import MultipleSelectionHint from '../MultipleSelectionHint';

it('should render for mac', () => {
  Object.defineProperty(navigator, 'userAgent', {
    configurable: true,
    value: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4)'
  });
  expect(shallow(<MultipleSelectionHint options={3} values={1} />)).toMatchSnapshot();
});

it('should render for windows', () => {
  Object.defineProperty(navigator, 'userAgent', {
    configurable: true,
    value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
  });
  expect(shallow(<MultipleSelectionHint options={3} values={1} />)).toMatchSnapshot();
});

it('should not render when there is not selection', () => {
  expect(shallow(<MultipleSelectionHint options={3} values={0} />).type()).toBe(null);
});

it('should not render when there are not enough options', () => {
  expect(shallow(<MultipleSelectionHint options={1} values={1} />).type()).toBe(null);
});
