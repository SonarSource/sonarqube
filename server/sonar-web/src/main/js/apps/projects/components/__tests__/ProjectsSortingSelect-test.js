/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import ProjectsSortingSelect from '../ProjectsSortingSelect';

it('should render correctly for overall view', () => {
  expect(
    shallow(<ProjectsSortingSelect selectedSort="name" view="overall" defaultOption="name" />)
  ).toMatchSnapshot();
});

it('should render correctly for leak view', () => {
  expect(
    shallow(
      <ProjectsSortingSelect
        selectedSort="new_coverage"
        view="leak"
        defaultOption="analysis_date"
      />
    )
  ).toMatchSnapshot();
});

it('should handle the descending sort direction', () => {
  expect(
    shallow(
      <ProjectsSortingSelect selectedSort="-vulnerability" view="overall" defaultOption="name" />
    )
  ).toMatchSnapshot();
});
