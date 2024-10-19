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
import { screen, waitFor } from '@testing-library/react';
import * as React from 'react';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQuery } from '../../../../helpers/mocks/issues';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../types/features';
import { GlobalSettingKeys } from '../../../../types/settings';
import { Sidebar } from '../Sidebar';

jest.mock('../../../../helpers/security-standard', () => {
  return {
    ...jest.requireActual('../../../../helpers/security-standard'),
    renderOwaspTop10Category: jest.fn(),
    renderOwaspTop102021Category: jest.fn(),
    renderSonarSourceSecurityCategory: jest.fn(),
  };
});

it('should render correct facets for Projects with PrioritizedRules feature', () => {
  renderSidebar(
    {
      component: mockComponent({ qualifier: ComponentQualifier.Project }),
    },
    [Feature.PrioritizedRules],
  );

  expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
    'issues.facet.cleanCodeAttributeCategories',
    'issues.facet.impactSoftwareQualities',
    'coding_rules.facet.impactSeverities',
    // help icon
    '',
    'issues.facet.types',
    'issues.facet.scopes',
    'issues.facet.issueStatuses',
    'issues.facet.standards',
    'issues.facet.createdAt',
    'issues.facet.languages',
    'issues.facet.rules',
    'issues.facet.tags',
    'issues.facet.directories',
    'issues.facet.files',
    'issues.facet.assignees',
    '',
    'issues.facet.authors',
    'issues.facet.prioritized_rule.category',
  ]);
});

it('should render correct facets for Application', () => {
  renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.Application }) });

  expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
    'issues.facet.cleanCodeAttributeCategories',
    'issues.facet.impactSoftwareQualities',
    'coding_rules.facet.impactSeverities',
    // help icon
    '',
    'issues.facet.types',
    'issues.facet.scopes',
    'issues.facet.issueStatuses',
    'issues.facet.standards',
    'issues.facet.createdAt',
    'issues.facet.languages',
    'issues.facet.rules',
    'issues.facet.tags',
    'issues.facet.projects',
    'issues.facet.assignees',
    '',
    'issues.facet.authors',
  ]);
});

it('should render correct facets for Portfolio', () => {
  renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.Portfolio }) });

  expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
    'issues.facet.cleanCodeAttributeCategories',
    'issues.facet.impactSoftwareQualities',
    'coding_rules.facet.impactSeverities',
    // help icon
    '',
    'issues.facet.types',
    'issues.facet.scopes',
    'issues.facet.issueStatuses',
    'issues.facet.standards',
    'issues.facet.createdAt',
    'issues.facet.languages',
    'issues.facet.rules',
    'issues.facet.tags',
    'issues.facet.projects',
    'issues.facet.assignees',
    '',
    'issues.facet.authors',
  ]);
});

it('should render correct facets for SubPortfolio', () => {
  renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.SubPortfolio }) });

  expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
    'issues.facet.cleanCodeAttributeCategories',
    'issues.facet.impactSoftwareQualities',
    'coding_rules.facet.impactSeverities',
    // help icon
    '',
    'issues.facet.types',
    'issues.facet.scopes',
    'issues.facet.issueStatuses',
    'issues.facet.standards',
    'issues.facet.createdAt',
    'issues.facet.languages',
    'issues.facet.rules',
    'issues.facet.tags',
    'issues.facet.projects',
    'issues.facet.assignees',
    '',
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

it('should render correctly for standards', async () => {
  renderSidebar({
    component: mockComponent({ qualifier: ComponentQualifier.Application }),
    query: {
      ...mockQuery(),
      owaspTop10: ['foo'],
      'owaspTop10-2021': ['bar'],
      sonarsourceSecurity: ['baz'],
    },
  });

  await waitFor(() => {
    expect(screen.getByLabelText('x_selected.3')).toBeInTheDocument();
  });
});

function renderSidebar(
  props: Partial<Parameters<typeof Sidebar>[0]> = {},
  features: Feature[] = [],
) {
  return renderApp(
    'sidebar',
    <Sidebar
      component={mockComponent()}
      createdAfterIncludesTime={false}
      facets={{}}
      loadSearchResultCount={jest.fn()}
      loadingFacets={{}}
      myIssues={false}
      onFacetToggle={jest.fn()}
      onFilterChange={jest.fn()}
      openFacets={{ createdAt: true }}
      showVariantsFilter={false}
      query={mockQuery()}
      referencedComponentsById={{}}
      referencedComponentsByKey={{}}
      referencedLanguages={{}}
      referencedRules={{}}
      referencedUsers={{}}
      {...props}
    />,
    {
      appState: mockAppState({
        settings: { [GlobalSettingKeys.DeveloperAggregatedInfoDisabled]: 'false' },
      }),
      featureList: features,
    },
  );
}
