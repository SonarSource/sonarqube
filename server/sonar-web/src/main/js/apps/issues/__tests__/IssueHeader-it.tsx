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
import * as React from 'react';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { WorkspaceContext } from '../../../components/workspace/context';
import { mockIssue, mockRuleDetails } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { Dict } from '../../../types/types';
import IssueHeader from '../components/IssueHeader';

it('renders correctly', async () => {
  const issue = mockIssue();
  renderIssueHeader(
    {
      issue: {
        ...issue,
        codeVariants: ['first', 'second'],
        effort: '5min',
        quickFixAvailable: true,
        externalRuleEngine: 'eslint',
      },
    },
    { eslint: 'eslint' },
  );

  // Title
  expect(byRole('heading', { name: issue.message }).get()).toBeInTheDocument();

  // CCT attribute
  const cctBadge = byText(
    `issue.clean_code_attribute_category.${issue.cleanCodeAttributeCategory}`,
  ).get();
  expect(cctBadge).toBeInTheDocument();
  await expect(cctBadge).toHaveAPopoverWithContent(
    `issue.clean_code_attribute.${issue.cleanCodeAttribute}`,
  );

  // Software Qualities
  const qualityBadge = byText(`software_quality.${issue.impacts[0].softwareQuality}`).get();
  expect(qualityBadge).toBeInTheDocument();
  await expect(qualityBadge).toHaveAPopoverWithContent('software_quality');

  // Deprecated type
  const type = byText(`issue.type.${issue.type}`).get();
  expect(type).toBeInTheDocument();
  await expect(type).toHaveATooltipWithContent('issue.clean_code_attribute');

  // Deprecated severity
  const severity = byText(`severity.${issue.severity}`).get();
  expect(severity).toBeInTheDocument();
  await expect(severity).toHaveATooltipWithContent('issue.severity.new');

  // Code variants
  expect(byText('issue.code_variants').get()).toBeInTheDocument();

  // Effort
  expect(byText('issue.effort').get()).toBeInTheDocument();

  // SonarLint badge
  expect(byText('issue.quick_fix').get()).toBeInTheDocument();

  // Rule external engine
  expect(byText('eslint').get()).toBeInTheDocument();
});

it('renders correctly when some data is not provided', () => {
  const issue = mockIssue();
  renderIssueHeader({
    issue,
  });

  // Code variants
  expect(byText('issues.facet.code_variants').query()).not.toBeInTheDocument();

  // Effort
  expect(byText('issue.effort').query()).not.toBeInTheDocument();

  // SonarLint badge
  expect(
    byText('issue.quick_fix_available_with_sonarlint_no_link').query(),
  ).not.toBeInTheDocument();

  // Rule external engine
  expect(byText('eslint').query()).not.toBeInTheDocument();
});

function renderIssueHeader(
  props: Partial<IssueHeader['props']> = {},
  externalRules: Dict<string> = {},
) {
  return renderComponent(
    <WorkspaceContext.Provider
      value={{ openComponent: jest.fn(), externalRulesRepoNames: externalRules }}
    >
      <IssueHeader
        issue={mockIssue()}
        ruleDetails={mockRuleDetails()}
        onIssueChange={jest.fn()}
        {...props}
      />
    </WorkspaceContext.Provider>,
  );
}
