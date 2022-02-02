/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockAppState } from '../../../../helpers/testMocks';
import { AdditionalCategory } from '../AdditionalCategories';
import { CategoriesList, CategoriesListProps } from '../AllCategoriesList';

jest.mock('../AdditionalCategories', () => ({
  ADDITIONAL_CATEGORIES: [
    {
      key: 'CAT_1',
      name: 'CAT_1_NAME',
      renderComponent: jest.fn(),
      availableGlobally: true,
      availableForProject: true,
      displayTab: true,
      requiresBranchesEnabled: true
    },
    {
      key: 'CAT_2',
      name: 'CAT_2_NAME',
      renderComponent: jest.fn(),
      availableGlobally: true,
      availableForProject: false,
      displayTab: true
    },
    {
      key: 'CAT_3',
      name: 'CAT_3_NAME',
      renderComponent: jest.fn(),
      availableGlobally: false,
      availableForProject: true,
      displayTab: true
    },
    {
      key: 'CAT_4',
      name: 'CAT_4_NAME',
      renderComponent: jest.fn(),
      availableGlobally: true,
      availableForProject: true,
      displayTab: false
    }
  ] as AdditionalCategory[]
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('global mode');
  expect(shallowRender({ selectedCategory: 'CAT_2' })).toMatchSnapshot('selected category');
  expect(shallowRender({ component: mockComponent() })).toMatchSnapshot('project mode');
  expect(shallowRender({ appState: mockAppState({ branchesEnabled: false }) })).toMatchSnapshot(
    'branches disabled'
  );
});

function shallowRender(props?: Partial<CategoriesListProps>) {
  return shallow<CategoriesListProps>(
    <CategoriesList
      appState={mockAppState({ branchesEnabled: true })}
      categories={['general']}
      defaultCategory="general"
      selectedCategory=""
      {...props}
    />
  );
}
