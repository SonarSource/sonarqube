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
import { queryHelpers, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { MetricKey } from '~sonar-aligned/types/metrics';
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import IssuesServiceMock from '../../../api/mocks/IssuesServiceMock';
import UsersServiceMock from '../../../api/mocks/UsersServiceMock';
import { CCT_SOFTWARE_QUALITY_METRICS } from '../../../helpers/constants';
import { isDiffMetric } from '../../../helpers/measures';
import { HttpStatus } from '../../../helpers/request';
import { mockIssue, mockLoggedInUser, mockMeasure } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { byLabelText } from '../../../helpers/testSelector';
import { RestUserDetailed } from '../../../types/users';
import SourceViewer, { Props } from '../SourceViewer';
import loadIssues from '../helpers/loadIssues';

jest.mock('../../../api/components');
jest.mock('../../../api/issues');
// The following 2 mocks are needed, because IssuesServiceMock mocks more than it should.
// This should be removed once IssuesServiceMock is cleaned up.
jest.mock('../../../api/rules');

jest.mock('../helpers/loadIssues', () => ({
  __esModule: true,
  default: jest.fn().mockResolvedValue([]),
}));

jest.mock('../helpers/lines', () => {
  const lines = jest.requireActual('../helpers/lines');
  return {
    ...lines,
    LINES_TO_LOAD: 20,
  };
});

const componentsHandler = new ComponentsServiceMock();
const issuesHandler = new IssuesServiceMock();
const usersHandler = new UsersServiceMock();
const message = 'First Issue';

beforeEach(() => {
  issuesHandler.reset();
  componentsHandler.reset();
  usersHandler.reset();
  usersHandler.users = [mockLoggedInUser() as unknown as RestUserDetailed];
});

it('should show a permalink on line number', async () => {
  const user = userEvent.setup();
  renderSourceViewer();
  let row = await screen.findByRole('row', { name: /\/\*$/ });
  expect(row).toBeInTheDocument();
  const rowScreen = within(row);

  await user.click(
    rowScreen.getByRole('button', {
      name: 'source_viewer.line_X.1',
    }),
  );

  expect(
    /* eslint-disable-next-line testing-library/prefer-presence-queries */
    queryHelpers.queryByAttribute(
      'data-clipboard-text',
      row,
      'http://localhost/code?id=foo&selected=foo%3Atest1.js&line=1',
    ),
  ).toBeInTheDocument();

  await user.keyboard('[Escape]');

  expect(
    /* eslint-disable-next-line testing-library/prefer-presence-queries */
    queryHelpers.queryByAttribute(
      'data-clipboard-text',
      row,
      'http://localhost/code?id=foo&selected=foo%3Atest1.js&line=1',
    ),
  ).not.toBeInTheDocument();

  row = await screen.findByRole('row', { name: / \* 6$/ });
  expect(row).toBeInTheDocument();
  const lowerRowScreen = within(row);

  await user.click(
    lowerRowScreen.getByRole('button', {
      name: 'source_viewer.line_X.6',
    }),
  );

  expect(
    lowerRowScreen.getByRole('menuitem', {
      name: 'source_viewer.copy_permalink',
    }),
  ).toBeInTheDocument();
});

it('should show issue on empty file', async () => {
  jest.mocked(loadIssues).mockResolvedValueOnce([
    mockIssue(false, {
      key: 'first-issue',
      message,
      line: undefined,
      textRange: undefined,
    }),
  ]);

  renderSourceViewer({
    component: componentsHandler.getEmptyFileKey(),
  });

  expect(await screen.findByRole('table')).toBeInTheDocument();
  expect(await screen.findByRole('row', { name: 'First Issue' })).toBeInTheDocument();
});

it('should be able to interact with issue action', async () => {
  jest.mocked(loadIssues).mockResolvedValueOnce([
    mockIssue(false, {
      actions: ['set_tags', 'comment', 'assign'],
      key: 'issue1',
      message,
      line: 1,
      textRange: { startLine: 1, endLine: 1, startOffset: 0, endOffset: 1 },
    }),
  ]);

  const user = userEvent.setup();
  renderSourceViewer();

  // Assign issue to a different user
  await user.click(
    await screen.findByRole('combobox', { name: 'issue.assign.unassigned_click_to_assign' }),
  );
  await user.click(screen.getByLabelText('search.search_for_users'));
  await user.keyboard('luke');
  expect(screen.getByText('Skywalker')).toBeInTheDocument();
});

it('should load line when looking around unloaded line', async () => {
  const rerender = renderSourceViewer({
    aroundLine: 50,
    component: componentsHandler.getHugeFileKey(),
  });

  expect(await screen.findByRole('row', { name: /Line 50$/ })).toBeInTheDocument();
  rerender({ aroundLine: 100, component: componentsHandler.getHugeFileKey() });

  expect(await screen.findByRole('row', { name: /Line 100$/ })).toBeInTheDocument();
});

it('should show SCM information', async () => {
  const user = userEvent.setup();
  renderSourceViewer();
  let row = await screen.findByRole('row', { name: /\/\*$/ });
  expect(row).toBeInTheDocument();
  const firstRowScreen = within(row);

  expect(
    firstRowScreen.getByRole('cell', { name: 'stas.vilchik@sonarsource.com' }),
  ).toBeInTheDocument();

  await user.click(
    firstRowScreen.getByRole('button', {
      name: 'source_viewer.author_X.stas.vilchik@sonarsource.com, source_viewer.click_for_scm_info.1',
    }),
  );

  // After using miui component the tooltip is appearing outside of the row
  expect(await screen.findAllByText('author')).toHaveLength(4);
  expect(screen.getAllByText('source_viewer.tooltip.scm.commited_on')).toHaveLength(3);
  expect(screen.getAllByText('source_viewer.tooltip.scm.revision')).toHaveLength(7);

  row = screen.getByRole('row', { name: /\* SonarQube$/ });
  expect(row).toBeInTheDocument();
  const secondRowScreen = within(row);

  expect(
    secondRowScreen.queryByRole('cell', { name: 'stas.vilchik@sonarsource.com' }),
  ).not.toBeInTheDocument();

  // SCM with no date
  row = await screen.findByRole('row', { name: /\* mailto:info AT sonarsource DOT com$/ });
  expect(row).toBeInTheDocument();
  const fourthRowScreen = within(row);

  await user.click(
    fourthRowScreen.getByRole('button', {
      name: 'source_viewer.author_X.stas.vilchik@sonarsource.com, source_viewer.click_for_scm_info.4',
    }),
  );

  // SCM with no date no author
  row = await screen.findByRole('row', { name: /\* 5$/ });
  expect(row).toBeInTheDocument();
  const fithRowScreen = within(row);
  expect(fithRowScreen.getByText('…')).toBeInTheDocument();

  await user.click(
    fithRowScreen.getByRole('button', {
      name: 'source_viewer.click_for_scm_info.5',
    }),
  );

  // No SCM Popup
  row = await screen.findByRole('row', {
    name: /\* This program is free software; you can redistribute it and\/or$/,
  });

  expect(row).toBeInTheDocument();
  expect(within(row).queryByRole('button')).not.toBeInTheDocument();
});

it('should show issue indicator', async () => {
  jest.mocked(loadIssues).mockResolvedValueOnce([
    mockIssue(false, {
      key: 'first-issue',
      message,
      line: 1,
      textRange: { startLine: 1, endLine: 1, startOffset: 0, endOffset: 1 },
    }),
    mockIssue(false, {
      key: 'second-issue',
      message: 'Second Issue',
      line: 1,
      textRange: { startLine: 1, endLine: 1, startOffset: 1, endOffset: 2 },
    }),
  ]);

  const user = userEvent.setup();
  const onIssueSelect = jest.fn();

  renderSourceViewer({
    onIssueSelect,
    displayAllIssues: false,
  });

  const row = await screen.findByRole('row', { name: /.*\/ \*$/ });
  const issueRow = within(row);
  expect(issueRow.getByText('2')).toBeInTheDocument();

  await user.click(
    issueRow.getByRole('button', {
      name: 'source_viewer.issues_on_line.multiple_issues_same_category.true.2.issue.clean_code_attribute_category.responsible',
    }),
  );
});

it('should show coverage information', async () => {
  renderSourceViewer();

  const coverdLine = within(
    await screen.findByRole('row', { name: /\* mailto:info AT sonarsource DOT com$/ }),
  );

  expect(
    coverdLine.getByLabelText('source_viewer.tooltip.covered.conditions.1'),
  ).toBeInTheDocument();

  const partialyCoveredWithConditionLine = within(
    await screen.findByRole('row', { name: / \* 5$/ }),
  );

  expect(
    partialyCoveredWithConditionLine.getByLabelText(
      'source_viewer.tooltip.partially-covered.conditions.1.2',
    ),
  ).toBeInTheDocument();

  const partialyCoveredLine = within(await screen.findByRole('row', { name: /\/\*$/ }));

  expect(
    partialyCoveredLine.getByLabelText('source_viewer.tooltip.partially-covered'),
  ).toBeInTheDocument();

  const uncoveredLine = within(await screen.findByRole('row', { name: / \* 6$/ }));
  expect(uncoveredLine.getByLabelText('source_viewer.tooltip.uncovered')).toBeInTheDocument();

  const uncoveredWithConditionLine = within(
    await screen.findByRole('row', { name: / \* SonarQube$/ }),
  );

  expect(
    uncoveredWithConditionLine.getByLabelText('source_viewer.tooltip.uncovered.conditions.1'),
  ).toBeInTheDocument();

  const coveredWithNoCondition = within(await screen.findByRole('row', { name: /\* Copyright$/ }));

  expect(
    coveredWithNoCondition.getByLabelText('source_viewer.tooltip.covered'),
  ).toBeInTheDocument();
});

it('should show duplication block', async () => {
  const user = userEvent.setup();
  renderSourceViewer();
  const duplicateLine = within(await screen.findByRole('row', { name: /\* 7$/ }));

  expect(
    duplicateLine.getByLabelText('source_viewer.tooltip.duplicated_block'),
  ).toBeInTheDocument();

  await user.click(
    duplicateLine.getByRole('button', { name: 'source_viewer.tooltip.duplicated_block' }),
  );

  expect(screen.getByRole('tooltip')).toBeVisible();

  await user.click(document.body);

  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it('should highlight symbol', async () => {
  const user = userEvent.setup();
  renderSourceViewer({ component: 'foo:testSymb.tsx' });
  const symbols = await screen.findAllByText('symbole');
  await user.click(symbols[0]);

  // For now just check the class. Maybe find a better accessible way of showing higlighted symbols
  symbols.forEach((element) => {
    expect(element).toHaveClass('highlighted');
  });
});

it('should show software quality measures in header', async () => {
  renderSourceViewer({ componentMeasures: generateMeasures(), showMeasures: true });

  expect(
    await byLabelText('source_viewer.issue_link_x.3.metric.security_issues.short_name').find(),
  ).toBeInTheDocument();
  expect(
    await byLabelText('source_viewer.issue_link_x.3.metric.reliability_issues.short_name').find(),
  ).toBeInTheDocument();
  expect(
    await byLabelText(
      'source_viewer.issue_link_x.3.metric.maintainability_issues.short_name',
    ).find(),
  ).toBeInTheDocument();
});

it('should show old issue measures in header', async () => {
  renderSourceViewer({
    componentMeasures: generateMeasures().filter(
      (m) => !CCT_SOFTWARE_QUALITY_METRICS.includes(m.metric as MetricKey),
    ),
    showMeasures: true,
  });

  expect(
    await byLabelText('source_viewer.issue_link_x.1.metric.security_issues.short_name').find(),
  ).toBeInTheDocument();
  expect(
    await byLabelText('source_viewer.issue_link_x.1.metric.reliability_issues.short_name').find(),
  ).toBeInTheDocument();
  expect(
    await byLabelText(
      'source_viewer.issue_link_x.1.metric.maintainability_issues.short_name',
    ).find(),
  ).toBeInTheDocument();
});

it('should show correct message when component is not asscessible', async () => {
  componentsHandler.setFailLoadingComponentStatus(HttpStatus.Forbidden);
  renderSourceViewer();

  expect(
    await screen.findByText('code_viewer.no_source_code_displayed_due_to_security'),
  ).toBeInTheDocument();
});

it('should show correct message when component does not exist', async () => {
  componentsHandler.setFailLoadingComponentStatus(HttpStatus.NotFound);
  renderSourceViewer();
  expect(await screen.findByText('component_viewer.no_component')).toBeInTheDocument();
});

function generateMeasures(qualitiesValue = '3.0', overallValue = '1.0', newValue = '2.0') {
  return [
    ...[
      MetricKey.security_issues,
      MetricKey.reliability_issues,
      MetricKey.maintainability_issues,
    ].map((metric) =>
      mockMeasure({ metric, value: JSON.stringify({ total: qualitiesValue }), period: undefined }),
    ),
    ...[
      MetricKey.ncloc,
      MetricKey.new_lines,
      MetricKey.bugs,
      MetricKey.vulnerabilities,
      MetricKey.code_smells,
      MetricKey.security_hotspots,
      MetricKey.coverage,
      MetricKey.new_coverage,
    ].map((metric) =>
      isDiffMetric(metric)
        ? mockMeasure({ metric, period: { index: 1, value: newValue } })
        : mockMeasure({ metric, value: overallValue, period: undefined }),
    ),
  ];
}

function renderSourceViewer(override?: Partial<Props>) {
  const { rerender } = renderComponent(
    <SourceViewer
      aroundLine={1}
      branchLike={undefined}
      component={componentsHandler.getNonEmptyFileKey()}
      displayAllIssues
      displayLocationMarkers
      onIssueSelect={jest.fn()}
      onLoaded={jest.fn()}
      onLocationSelect={jest.fn()}
      {...override}
    />,
  );

  return function (reoverride?: Partial<Props>) {
    rerender(
      <SourceViewer
        aroundLine={1}
        branchLike={undefined}
        component={componentsHandler.getNonEmptyFileKey()}
        displayAllIssues
        displayLocationMarkers
        onIssueSelect={jest.fn()}
        onLoaded={jest.fn()}
        onLocationSelect={jest.fn()}
        {...override}
        {...reoverride}
      />,
    );
  };
}
