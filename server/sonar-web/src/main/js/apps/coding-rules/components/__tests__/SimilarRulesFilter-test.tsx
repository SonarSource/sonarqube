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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { mockRule } from '../../../../helpers/testMocks';
import { click } from '../../../../helpers/testUtils';
import SimilarRulesFilter from '../SimilarRulesFilter';

it('should render correctly', () => {
  expect(
    shallow(<SimilarRulesFilter onFilterChange={jest.fn()} rule={mockRule()} />)
  ).toMatchSnapshot();
});

it('should filter by similar language', () => {
  const onFilterChange = jest.fn();
  const wrapper = mountRenderAction('language', { onFilterChange });
  click(wrapper);
  expect(onFilterChange).toHaveBeenCalledWith({ languages: ['js'] });
});

it('should filter by similar type', () => {
  const onFilterChange = jest.fn();
  const wrapper = mountRenderAction('type', { onFilterChange });
  click(wrapper);
  expect(onFilterChange).toHaveBeenCalledWith({ types: ['CODE_SMELL'] });
});

it('should filter by similar severity', () => {
  const onFilterChange = jest.fn();
  const wrapper = mountRenderAction('severity', { onFilterChange });
  click(wrapper);
  expect(onFilterChange).toHaveBeenCalledWith({ severities: ['MAJOR'] });
});

it('should filter by similar tag', () => {
  const onFilterChange = jest.fn();
  const wrapper = mountRenderAction('tag', { onFilterChange });
  click(wrapper);
  expect(onFilterChange).toHaveBeenCalledWith({ tags: ['x'] });
});

function mountRenderAction(actionName: string, props: Partial<SimilarRulesFilter['props']> = {}) {
  const wrapper = mount(
    <SimilarRulesFilter onFilterChange={jest.fn()} rule={mockRule()} {...props} />
  );
  return mount(wrapper.find('Dropdown').prop<React.ReactElement>('overlay'))
    .find(`button[data-test="coding-rules__similar-${actionName}"]`)
    .first();
}
