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
import { mockQuery } from '../../../../helpers/mocks/issues';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { GlobalSettingKeys } from '../../../../types/settings';
import { Sidebar } from '../Sidebar';

it('should render correct facets for Application', () => {
  renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.Application }) });
  expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
    'issues.facet.types',
    'issues.facet.severities',
    'issues.facet.scopes',
    'issues.facet.resolutions',
    'issues.facet.statuses',
    'issues.facet.standards',
    'issues.facet.createdAt',
    'issues.facet.languages',
    'issues.facet.rules',
    'issues.facet.tags',
    'issues.facet.projects',
    'issues.facet.assignees',
    'clear',
    'issues.facet.authors',
  ]);
});

it('should render correct facets for Portfolio', () => {
  renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.Portfolio }) });
  expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
    'issues.facet.types',
    'issues.facet.severities',
    'issues.facet.scopes',
    'issues.facet.resolutions',
    'issues.facet.statuses',
    'issues.facet.standards',
    'issues.facet.createdAt',
    'issues.facet.languages',
    'issues.facet.rules',
    'issues.facet.tags',
    'issues.facet.projects',
    'issues.facet.assignees',
    'clear',
    'issues.facet.authors',
  ]);
});

it('should render correct facets for SubPortfolio', () => {
  renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.SubPortfolio }) });
  expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
    'issues.facet.types',
    'issues.facet.severities',
    'issues.facet.scopes',
    'issues.facet.resolutions',
    'issues.facet.statuses',
    'issues.facet.standards',
    'issues.facet.createdAt',
    'issues.facet.languages',
    'issues.facet.rules',
    'issues.facet.tags',
    'issues.facet.projects',
    'issues.facet.assignees',
    'clear',
    'issues.facet.authors',
  ]);
});

it.each([
  ['week', '1w'],
  ['month', '1m'],
  ['year', '1y'],
])('should render correctly for createdInLast %s', (name, createdInLast) => {
  renderSidebar({ component: mockComponent(), query: mockQuery({ createdInLast }) });

  const text = {
    week: 'issues.facet.createdAt.last_week',
    month: 'issues.facet.createdAt.last_month',
    year: 'issues.facet.createdAt.last_year',
  }[name] as string;
  expect(screen.getByText(text)).toBeInTheDocument();
});

function renderSidebar(props: Partial<Sidebar['props']> = {}) {
  return renderComponent(
    <Sidebar
      appState={mockAppState({
        settings: { [GlobalSettingKeys.DeveloperAggregatedInfoDisabled]: 'false' },
      })}
      component={mockComponent()}
      createdAfterIncludesTime={false}
      facets={{}}
      loadSearchResultCount={jest.fn()}
      loadingFacets={{}}
      myIssues={false}
      onFacetToggle={jest.fn()}
      onFilterChange={jest.fn()}
      openFacets={{}}
      query={mockQuery()}
      referencedComponentsById={{}}
      referencedComponentsByKey={{}}
      referencedLanguages={{}}
      referencedRules={{}}
      referencedUsers={{}}
      {...props}
    />
  );
}
