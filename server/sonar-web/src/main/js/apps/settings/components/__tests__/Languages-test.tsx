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
import { shallow } from 'enzyme';
import * as React from 'react';
import Select from '../../../../components/controls/Select';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import CategoryDefinitionsList from '../CategoryDefinitionsList';
import { Languages, LanguagesProps } from '../Languages';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly with an unknow language', () => {
  const wrapper = shallowRender({ selectedCategory: 'unknown' });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle a change of the selected language', () => {
  const push = jest.fn();
  const router = mockRouter({ push });
  const wrapper = shallowRender({ router });
  expect(wrapper.find(CategoryDefinitionsList).props().category).toBe('java');

  const { onChange } = wrapper.find(Select).props();

  onChange({ label: '', originalValue: 'CoBoL', value: 'cobol' });
  expect(push).toHaveBeenCalledWith(expect.objectContaining({ query: { category: 'CoBoL' } }));
});

it('should correctly show the subcategory for a component', () => {
  const component = mockComponent();
  const wrapper = shallowRender({ component });
  expect(wrapper.find(CategoryDefinitionsList).props().component).toBe(component);
});

function shallowRender(props: Partial<LanguagesProps> = {}) {
  return shallow(
    <Languages
      categories={['Java', 'JavaScript', 'COBOL']}
      component={undefined}
      definitions={[]}
      location={mockLocation()}
      router={mockRouter()}
      selectedCategory="java"
      {...props}
    />
  );
}
