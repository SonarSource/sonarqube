/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { find } from 'lodash';
import { mockComponent } from '../../../../helpers/testMocks';
import { ADDITIONAL_CATEGORIES } from '../AdditionalCategories';
import { PULL_REQUEST_DECORATION_BINDING_CATEGORY } from '../AdditionalCategoryKeys';

it('should render additional categories component correctly', () => {
  ADDITIONAL_CATEGORIES.forEach(cat => {
    expect(
      cat.renderComponent({
        component: mockComponent(),
        selectedCategory: 'TEST'
      })
    ).toMatchSnapshot();
  });
});

it('should not render pull request decoration binding component when the component is not defined', () => {
  const category = find(
    ADDITIONAL_CATEGORIES,
    c => c.key === PULL_REQUEST_DECORATION_BINDING_CATEGORY
  );

  if (!category) {
    fail('category should be defined');
  } else {
    expect(
      category.renderComponent({ component: undefined, selectedCategory: '' })
    ).toBeUndefined();
  }
});
