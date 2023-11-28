/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { screen } from '@testing-library/react';
import * as React from 'react';
import { mockComponent } from '../../../../helpers/mocks/component';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { AdditionalCategory } from '../AdditionalCategories';
import CategoriesList, { CategoriesListProps } from '../AllCategoriesList';

jest.mock('../AdditionalCategories', () => ({
  ADDITIONAL_CATEGORIES: [
    {
      key: 'CAT_1',
      name: 'CAT_1_NAME',
      renderComponent: jest.fn(),
      availableGlobally: true,
      availableForProject: true,
      displayTab: true,
      requiresBranchSupport: true,
    },
    {
      key: 'CAT_2',
      name: 'CAT_2_NAME',
      renderComponent: jest.fn(),
      availableGlobally: true,
      availableForProject: false,
      displayTab: true,
    },
    {
      key: 'CAT_3',
      name: 'CAT_3_NAME',
      renderComponent: jest.fn(),
      availableGlobally: false,
      availableForProject: true,
      displayTab: true,
    },
    {
      key: 'CAT_4',
      name: 'CAT_4_NAME',
      renderComponent: jest.fn(),
      availableGlobally: true,
      availableForProject: true,
      displayTab: false,
    },
  ] as AdditionalCategory[],
}));

it('should render correctly', () => {
  renderCategoriesList({ selectedCategory: 'CAT_2' });

  expect(screen.getByText('CAT_1_NAME')).toBeInTheDocument();
  expect(screen.getByText('CAT_2_NAME')).toBeInTheDocument();
  expect(screen.queryByText('CAT_3_NAME')).not.toBeInTheDocument();
  expect(screen.queryByText('CAT_4_NAME')).not.toBeInTheDocument();
  expect(screen.getByText('CAT_2_NAME')).toHaveClass('active');
});

it('should correctly for project', () => {
  renderCategoriesList({ component: mockComponent() });

  expect(screen.getByText('CAT_1_NAME')).toBeInTheDocument();
  expect(screen.queryByText('CAT_2_NAME')).not.toBeInTheDocument();
  expect(screen.getByText('CAT_3_NAME')).toBeInTheDocument();
  expect(screen.queryByText('CAT_4_NAME')).not.toBeInTheDocument();
});

it('should render correctly when branches are disabled', () => {
  renderCategoriesList({ hasFeature: () => false });

  expect(screen.queryByText('CAT_1_NAME')).not.toBeInTheDocument();
  expect(screen.getByText('CAT_2_NAME')).toBeInTheDocument();
  expect(screen.queryByText('CAT_4_NAME')).not.toBeInTheDocument();
});

function renderCategoriesList(props?: Partial<CategoriesListProps>) {
  return renderComponent(
    <CategoriesList
      hasFeature={jest.fn().mockReturnValue(true)}
      categories={['general']}
      defaultCategory="general"
      selectedCategory=""
      {...props}
    />,
  );
}
