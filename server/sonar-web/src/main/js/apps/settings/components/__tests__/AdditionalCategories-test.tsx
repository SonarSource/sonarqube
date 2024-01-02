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
import { find } from 'lodash';
import { mockComponent } from '../../../../helpers/mocks/component';
import { PULL_REQUEST_DECORATION_BINDING_CATEGORY } from '../../constants';
import { ADDITIONAL_CATEGORIES } from '../AdditionalCategories';

it('should render additional categories component correctly', () => {
  ADDITIONAL_CATEGORIES.forEach((cat) => {
    expect(
      cat.renderComponent({
        categories: [],
        component: mockComponent(),
        definitions: [],
        selectedCategory: 'TEST',
      })
    ).toMatchSnapshot();
  });
});

it('should not render pull request decoration binding component when the component is not defined', () => {
  const category = find(
    ADDITIONAL_CATEGORIES,
    (c) => c.key === PULL_REQUEST_DECORATION_BINDING_CATEGORY
  );

  expect(
    category!.renderComponent({
      categories: [],
      component: undefined,
      definitions: [],
      selectedCategory: '',
    })
  ).toBeUndefined();
});
