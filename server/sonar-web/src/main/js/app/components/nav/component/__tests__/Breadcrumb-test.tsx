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
import { screen } from '@testing-library/react';
import * as React from 'react';
import { mockBranch, mockMainBranch } from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../../types/component';
import { Breadcrumb, BreadcrumbProps } from '../Breadcrumb';

it('should render correctly', () => {
  renderBreadcrumb();
  expect(screen.getByRole('link', { name: 'Parent portfolio' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Child portfolio' })).toBeInTheDocument();
});

it('should render correctly when not on a main branch', () => {
  renderBreadcrumb({
    component: mockComponent({
      breadcrumbs: [
        {
          key: 'project',
          name: 'My Project',
          qualifier: ComponentQualifier.Project,
        },
      ],
    }),
    currentBranchLike: mockBranch(),
  });
  expect(
    screen.getByRole('link', { name: `qualifier.${ComponentQualifier.Project} My Project` })
  ).toBeInTheDocument();
});

function renderBreadcrumb(props: Partial<BreadcrumbProps> = {}) {
  return renderComponent(
    <Breadcrumb
      component={mockComponent({
        breadcrumbs: [
          {
            key: 'parent-portfolio',
            name: 'Parent portfolio',
            qualifier: ComponentQualifier.Portfolio,
          },
          {
            key: 'child-portfolio',
            name: 'Child portfolio',
            qualifier: ComponentQualifier.SubPortfolio,
          },
        ],
      })}
      currentBranchLike={mockMainBranch()}
      {...props}
    />
  );
}
