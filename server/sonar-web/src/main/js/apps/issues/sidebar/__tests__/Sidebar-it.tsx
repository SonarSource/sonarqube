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
import userEvent from '@testing-library/user-event';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import SettingsServiceMock from '../../../../api/mocks/SettingsServiceMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQuery } from '../../../../helpers/mocks/issues';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../sonar-aligned/helpers/testSelector';
import { SoftwareImpactSeverity, SoftwareQuality } from '../../../../types/clean-code-taxonomy';
import { Feature } from '../../../../types/features';
import { IssueSeverity, IssueType } from '../../../../types/issues';
import { GlobalSettingKeys, SettingsKey } from '../../../../types/settings';
import { Sidebar } from '../Sidebar';

jest.mock('../../../../helpers/security-standard', () => {
  return {
    ...jest.requireActual('../../../../helpers/security-standard'),
    renderOwaspTop10Category: jest.fn(),
    renderOwaspTop102021Category: jest.fn(),
    renderSonarSourceSecurityCategory: jest.fn(),
  };
});

const settingsHandler = new SettingsServiceMock();

beforeEach(() => {
  settingsHandler.reset();
});

describe('MQR mode', () => {
  it('should render correct facets for Projects with PrioritizedRules feature', () => {
    renderSidebar(
      {
        component: mockComponent({ qualifier: ComponentQualifier.Project }),
      },
      [Feature.PrioritizedRules],
    );

    expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
      'issues.facet.impactSoftwareQualities',
      '',
      'coding_rules.facet.impactSeverities',
      // help icon
      '',
      'issues.facet.cleanCodeAttributeCategories',
      '',
      'issues.facet.scopes',
      'issues.facet.issueStatuses',
      '',
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

  it('should show standard filters if they exist in query', async () => {
    const user = userEvent.setup();
    let component = renderSidebar({
      query: mockQuery({ types: [IssueType.CodeSmell] }),
    });

    expect(
      await screen.findByRole('button', { name: 'issues.facet.impactSoftwareQualities' }),
    ).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();
    expect(
      byRole('button', { name: 'issues.facet.types' })
        .byText('issues.facet.second_line.mode.standard')
        .get(),
    ).toBeInTheDocument();
    // help icon
    await user.click(byRole('button', { name: 'help' }).getAt(2));
    expect(screen.getByText('issues.qg_mismatch.title')).toBeInTheDocument();

    expect(
      screen.queryByRole('button', { name: 'issues.facet.severities' }),
    ).not.toBeInTheDocument();

    component.unmount();

    component = renderSidebar({
      query: mockQuery({ severities: [IssueSeverity.Blocker] }),
    });

    expect(
      await screen.findByRole('button', { name: 'issues.facet.impactSoftwareQualities' }),
    ).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'issues.facet.severities' })).toBeInTheDocument();
    expect(
      byRole('button', { name: 'issues.facet.severities' })
        .byText('issues.facet.second_line.mode.standard')
        .get(),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'issues.facet.types' })).not.toBeInTheDocument();

    component.unmount();

    renderSidebar({
      query: mockQuery({
        types: [IssueType.CodeSmell],
        severities: [IssueSeverity.Blocker],
      }),
    });

    expect(
      await screen.findByRole('button', { name: 'issues.facet.impactSoftwareQualities' }),
    ).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'issues.facet.severities' })).toBeInTheDocument();
  });

  it('should render correct facets for Application', () => {
    renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.Application }) });

    expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
      'issues.facet.impactSoftwareQualities',
      '',
      'coding_rules.facet.impactSeverities',
      // help icon
      '',
      'issues.facet.cleanCodeAttributeCategories',
      '',
      'issues.facet.scopes',
      'issues.facet.issueStatuses',
      '',
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
      'issues.facet.impactSoftwareQualities',
      '',
      'coding_rules.facet.impactSeverities',
      // help icon
      '',
      'issues.facet.cleanCodeAttributeCategories',
      '',
      'issues.facet.scopes',
      'issues.facet.issueStatuses',
      '',
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
      'issues.facet.impactSoftwareQualities',
      '',
      'coding_rules.facet.impactSeverities',
      // help icon
      '',
      'issues.facet.cleanCodeAttributeCategories',
      '',
      'issues.facet.scopes',
      'issues.facet.issueStatuses',
      '',
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
});

describe('Standard mode', () => {
  beforeEach(() => {
    settingsHandler.set(SettingsKey.MQRMode, 'false');
  });

  it('should render correct facets for Projects with PrioritizedRules feature', async () => {
    renderSidebar(
      {
        component: mockComponent({ qualifier: ComponentQualifier.Project }),
      },
      [Feature.PrioritizedRules],
    );

    expect(await screen.findByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();

    expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
      'issues.facet.types',
      'issues.facet.severities',
      'issues.facet.scopes',
      'issues.facet.issueStatuses',
      '',
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

  it('should show show mqr filters if they exist in query', async () => {
    const user = userEvent.setup();
    let component = renderSidebar({
      query: mockQuery({ impactSeverities: [SoftwareImpactSeverity.Blocker] }),
    });

    expect(await screen.findByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();

    expect(
      screen.getByRole('button', { name: 'coding_rules.facet.impactSeverities' }),
    ).toBeInTheDocument();
    expect(
      byRole('button', { name: 'coding_rules.facet.impactSeverities' })
        .byText('issues.facet.second_line.mode.mqr')
        .get(),
    ).toBeInTheDocument();

    // help icon
    await user.click(byRole('button', { name: 'help' }).getAt(0));
    expect(screen.getByText('issues.qg_mismatch.title')).toBeInTheDocument();

    expect(
      screen.queryByRole('button', { name: 'issues.facet.impactSoftwareQualities' }),
    ).not.toBeInTheDocument();

    component.unmount();

    component = renderSidebar({
      query: mockQuery({ impactSoftwareQualities: [SoftwareQuality.Maintainability] }),
    });

    expect(await screen.findByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();

    expect(
      screen.getByRole('button', { name: 'issues.facet.impactSoftwareQualities' }),
    ).toBeInTheDocument();
    expect(
      byRole('button', { name: 'issues.facet.impactSoftwareQualities' })
        .byText('issues.facet.second_line.mode.mqr')
        .get(),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'coding_rules.facet.impactSeverities' }),
    ).not.toBeInTheDocument();

    component.unmount();

    renderSidebar({
      query: mockQuery({
        impactSoftwareQualities: [SoftwareQuality.Maintainability],
        impactSeverities: [SoftwareImpactSeverity.Blocker],
      }),
    });

    expect(await screen.findByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();

    expect(
      screen.getByRole('button', { name: 'issues.facet.impactSoftwareQualities' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'coding_rules.facet.impactSeverities' }),
    ).toBeInTheDocument();
  });

  it('should render correct facets for Application', async () => {
    renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.Application }) });

    expect(await screen.findByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();

    expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
      'issues.facet.types',
      'issues.facet.severities',
      'issues.facet.scopes',
      'issues.facet.issueStatuses',
      '',
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

  it('should render correct facets for Portfolio', async () => {
    renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.Portfolio }) });

    expect(await screen.findByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();

    expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
      'issues.facet.types',
      'issues.facet.severities',
      'issues.facet.scopes',
      'issues.facet.issueStatuses',
      '',
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

  it('should render correct facets for SubPortfolio', async () => {
    renderSidebar({ component: mockComponent({ qualifier: ComponentQualifier.SubPortfolio }) });

    expect(await screen.findByRole('button', { name: 'issues.facet.types' })).toBeInTheDocument();

    expect(screen.getAllByRole('button').map((button) => button.textContent)).toStrictEqual([
      'issues.facet.types',
      'issues.facet.severities',
      'issues.facet.scopes',
      'issues.facet.issueStatuses',
      '',
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
