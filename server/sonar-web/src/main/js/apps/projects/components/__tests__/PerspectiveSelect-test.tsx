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
import PerspectiveSelect from '../PerspectiveSelect';

it('should render correctly', () => {
  expect(shallow(<PerspectiveSelect onChange={jest.fn()} view="overall" />)).toMatchSnapshot();
});

it('should render with coverage selected', () => {
  expect(
    shallow(
      <PerspectiveSelect onChange={jest.fn()} view="visualizations" visualization="coverage" />
    )
  ).toMatchSnapshot();
});

it('should handle perspective change correctly', () => {
  const onChange = jest.fn();
  const instance = shallow(
    <PerspectiveSelect onChange={onChange} view="visualizations" visualization="coverage" />
  ).instance() as PerspectiveSelect;
  instance.handleChange({ label: 'overall', value: 'overall', type: 'view' });
  instance.handleChange({ label: 'leak', value: 'leak', type: 'view' });
  instance.handleChange({ label: 'coverage', value: 'coverage', type: 'visualization' });
  expect(onChange.mock.calls).toMatchSnapshot();
});
