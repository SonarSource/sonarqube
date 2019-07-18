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
import { click } from 'sonar-ui-common/helpers/testUtils';
import ProjectsSortingSelect from '../ProjectsSortingSelect';

it('should render correctly for overall view', () => {
  expect(
    shallow(
      <ProjectsSortingSelect
        defaultOption="name"
        onChange={jest.fn()}
        selectedSort="name"
        view="overall"
      />
    )
  ).toMatchSnapshot();
});

it('should render correctly for leak view', () => {
  expect(
    shallow(
      <ProjectsSortingSelect
        defaultOption="analysis_date"
        onChange={jest.fn()}
        selectedSort="new_coverage"
        view="leak"
      />
    )
  ).toMatchSnapshot();
});

it('should handle the descending sort direction', () => {
  expect(
    shallow(
      <ProjectsSortingSelect
        defaultOption="name"
        onChange={jest.fn()}
        selectedSort="-vulnerability"
        view="overall"
      />
    )
  ).toMatchSnapshot();
});

it('changes sorting', () => {
  const onChange = jest.fn();
  const instance = shallow(
    <ProjectsSortingSelect
      defaultOption="name"
      onChange={onChange}
      selectedSort="-vulnerabilities"
      view="overall"
    />
  ).instance() as ProjectsSortingSelect;
  instance.handleSortChange({ label: 'size', value: 'size' });
  expect(onChange).toBeCalledWith('size', true);
});

it('reverses sorting', () => {
  const onChange = jest.fn();
  const wrapper = shallow(
    <ProjectsSortingSelect
      defaultOption="name"
      onChange={onChange}
      selectedSort="-size"
      view="overall"
    />
  );
  click(wrapper.find('ButtonIcon'));
  expect(onChange).toBeCalledWith('size', false);
});
