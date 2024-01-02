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
import { act } from 'react-dom/test-utils';
import { byRole } from 'testing-library-selector';
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import IssuesServiceMock from '../../../api/mocks/IssuesServiceMock';
import { HttpStatus } from '../../../helpers/request';
import { mockIssue } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import loadIssues from '../helpers/loadIssues';
import SourceViewer from '../SourceViewer';

jest.mock('../../../api/components');
jest.mock('../../../api/issues');
// The following 2 mocks are needed, because IssuesServiceMock mocks more than it should.
// This should be removed once IssuesServiceMock is cleaned up.
jest.mock('../../../api/rules');
jest.mock('../../../api/users');

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

const ui = {
  codeSmellTypeButton: byRole('button', { name: 'issue.type.CODE_SMELL' }),
  minorSeverityButton: byRole('button', { name: 'severity.MINOR' }),
};

const componentsHandler = new ComponentsServiceMock();
const issuesHandler = new IssuesServiceMock();

beforeEach(() => {
  issuesHandler.reset();
  componentsHandler.reset();
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
    })
  );

  expect(
    /* eslint-disable-next-line testing-library/prefer-presence-queries */
    queryHelpers.queryByAttribute(
      'data-clipboard-text',
      row,
      'http://localhost/code?id=foo&selected=foo%3Atest1.js&line=1'
    )
  ).toBeInTheDocument();

  await act(async () => {
    await user.keyboard('[Escape]');
  });

  expect(
    /* eslint-disable-next-line testing-library/prefer-presence-queries */
    queryHelpers.queryByAttribute(
      'data-clipboard-text',
      row,
      'http://localhost/code?id=foo&selected=foo%3Atest1.js&line=1'
    )
  ).not.toBeInTheDocument();

  row = await screen.findByRole('row', { name: / \* 6$/ });
  expect(row).toBeInTheDocument();
  const lowerRowScreen = within(row);

  await act(async () => {
    await user.click(
      lowerRowScreen.getByRole('button', {
        name: 'source_viewer.line_X.6',
      })
    );
  });

  expect(
    lowerRowScreen.getByRole('button', {
      name: 'component_viewer.copy_permalink',
    })
  ).toBeInTheDocument();
});

it('should show issue on empty file', async () => {
  (loadIssues as jest.Mock).mockResolvedValueOnce([
    mockIssue(false, {
      key: 'first-issue',
      message: 'First Issue',
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
  (loadIssues as jest.Mock).mockResolvedValueOnce([
    mockIssue(false, {
      actions: ['set_type', 'set_tags', 'comment', 'set_severity', 'assign'],
      key: 'issue1',
      message: 'First Issue',
      line: 1,
      textRange: { startLine: 1, endLine: 1, startOffset: 0, endOffset: 1 },
    }),
  ]);
  const user = userEvent.setup();
  renderSourceViewer();

  //Open Issue type
  await user.click(
    await screen.findByRole('button', { name: 'issue.type.type_x_click_to_change.issue.type.BUG' })
  );
  expect(ui.codeSmellTypeButton.get()).toBeInTheDocument();

  // Open severity
  await user.click(
    await screen.findByRole('button', {
      name: 'issue.severity.severity_x_click_to_change.severity.MAJOR',
    })
  );
  expect(ui.minorSeverityButton.get()).toBeInTheDocument();

  // Close
  await user.keyboard('{Escape}');
  expect(ui.minorSeverityButton.query()).not.toBeInTheDocument();

  // Change the severity
  await user.click(
    await screen.findByRole('button', {
      name: 'issue.severity.severity_x_click_to_change.severity.MAJOR',
    })
  );
  expect(ui.minorSeverityButton.get()).toBeInTheDocument();
  await user.click(ui.minorSeverityButton.get());
  expect(
    screen.getByRole('button', {
      name: 'issue.severity.severity_x_click_to_change.severity.MINOR',
    })
  ).toBeInTheDocument();
});

it('should load line when looking arround unloaded line', async () => {
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
    firstRowScreen.getByRole('cell', { name: 'stas.vilchik@sonarsource.com' })
  ).toBeInTheDocument();
  await user.click(
    firstRowScreen.getByRole('button', {
      name: 'source_viewer.author_X.stas.vilchik@sonarsource.com, source_viewer.click_for_scm_info.1',
    })
  );

  expect(
    await firstRowScreen.findByRole('heading', { level: 4, name: 'author' })
  ).toBeInTheDocument();
  expect(
    firstRowScreen.getByRole('heading', { level: 4, name: 'source_viewer.tooltip.scm.commited_on' })
  ).toBeInTheDocument();
  expect(
    firstRowScreen.getByRole('heading', { level: 4, name: 'source_viewer.tooltip.scm.revision' })
  ).toBeInTheDocument();

  row = screen.getByRole('row', { name: /\* SonarQube$/ });
  expect(row).toBeInTheDocument();
  const secondRowScreen = within(row);
  expect(
    secondRowScreen.queryByRole('cell', { name: 'stas.vilchik@sonarsource.com' })
  ).not.toBeInTheDocument();

  // SCM with no date
  row = await screen.findByRole('row', { name: /\* mailto:info AT sonarsource DOT com$/ });
  expect(row).toBeInTheDocument();
  const fourthRowScreen = within(row);
  await user.click(
    fourthRowScreen.getByRole('button', {
      name: 'source_viewer.author_X.stas.vilchik@sonarsource.com, source_viewer.click_for_scm_info.4',
    })
  );

  expect(
    await fourthRowScreen.findByRole('heading', { level: 4, name: 'author' })
  ).toBeInTheDocument();
  expect(
    fourthRowScreen.queryByRole('heading', {
      level: 4,
      name: 'source_viewer.tooltip.scm.commited_on',
    })
  ).not.toBeInTheDocument();
  expect(
    fourthRowScreen.getByRole('heading', { level: 4, name: 'source_viewer.tooltip.scm.revision' })
  ).toBeInTheDocument();

  // SCM with no date no author
  row = await screen.findByRole('row', { name: /\* 5$/ });
  expect(row).toBeInTheDocument();
  const fithRowScreen = within(row);
  expect(fithRowScreen.getByText('…')).toBeInTheDocument();
  await user.click(
    fithRowScreen.getByRole('button', {
      name: 'source_viewer.click_for_scm_info.5',
    })
  );

  expect(
    fithRowScreen.queryByRole('heading', { level: 4, name: 'author' })
  ).not.toBeInTheDocument();
  expect(
    fithRowScreen.queryByRole('heading', {
      level: 4,
      name: 'source_viewer.tooltip.scm.commited_on',
    })
  ).not.toBeInTheDocument();
  expect(
    fithRowScreen.getByRole('heading', { level: 4, name: 'source_viewer.tooltip.scm.revision' })
  ).toBeInTheDocument();

  // No SCM Popup
  row = await screen.findByRole('row', {
    name: /\* This program is free software; you can redistribute it and\/or$/,
  });
  expect(row).toBeInTheDocument();
  expect(within(row).queryByRole('button')).not.toBeInTheDocument();
});

it('should show issue indicator', async () => {
  (loadIssues as jest.Mock).mockResolvedValueOnce([
    mockIssue(false, {
      key: 'first-issue',
      message: 'First Issue',
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
      name: 'source_viewer.issues_on_line.X_issues_of_type_Y.source_viewer.issues_on_line.show.2.issue.type.BUG.plural',
    })
  );
  const firstIssueBox = issueRow.getByRole('region', { name: 'First Issue' });
  const secondIssueBox = issueRow.getByRole('region', { name: 'Second Issue' });
  expect(firstIssueBox).toBeInTheDocument();
  expect(secondIssueBox).toBeInTheDocument();
  expect(
    issueRow.getByRole('button', {
      name: 'source_viewer.issues_on_line.X_issues_of_type_Y.source_viewer.issues_on_line.hide.2.issue.type.BUG.plural',
    })
  ).toBeInTheDocument();

  await user.click(firstIssueBox);
  expect(onIssueSelect).toHaveBeenCalledWith('first-issue');

  await user.click(secondIssueBox);
  expect(onIssueSelect).toHaveBeenCalledWith('second-issue');
});

it('should show coverage information', async () => {
  renderSourceViewer();
  const coverdLine = within(
    await screen.findByRole('row', { name: /\* mailto:info AT sonarsource DOT com$/ })
  );
  expect(
    coverdLine.getByLabelText('source_viewer.tooltip.covered.conditions.1')
  ).toBeInTheDocument();

  const partialyCoveredWithConditionLine = within(
    await screen.findByRole('row', { name: / \* 5$/ })
  );
  expect(
    partialyCoveredWithConditionLine.getByLabelText(
      'source_viewer.tooltip.partially-covered.conditions.1.2'
    )
  ).toBeInTheDocument();

  const partialyCoveredLine = within(await screen.findByRole('row', { name: /\/\*$/ }));
  expect(
    partialyCoveredLine.getByLabelText('source_viewer.tooltip.partially-covered')
  ).toBeInTheDocument();

  const uncoveredLine = within(await screen.findByRole('row', { name: / \* 6$/ }));
  expect(uncoveredLine.getByLabelText('source_viewer.tooltip.uncovered')).toBeInTheDocument();

  const uncoveredWithConditionLine = within(
    await screen.findByRole('row', { name: / \* SonarQube$/ })
  );
  expect(
    uncoveredWithConditionLine.getByLabelText('source_viewer.tooltip.uncovered.conditions.1')
  ).toBeInTheDocument();

  const coveredWithNoCondition = within(await screen.findByRole('row', { name: /\* Copyright$/ }));
  expect(
    coveredWithNoCondition.getByLabelText('source_viewer.tooltip.covered')
  ).toBeInTheDocument();
});

it('should show duplication block', async () => {
  const user = userEvent.setup();
  renderSourceViewer();
  const duplicateLine = within(await screen.findByRole('row', { name: /\* 7$/ }));
  expect(
    duplicateLine.getByLabelText('source_viewer.tooltip.duplicated_block')
  ).toBeInTheDocument();

  await user.click(
    duplicateLine.getByRole('button', { name: 'source_viewer.tooltip.duplicated_block' })
  );

  expect(duplicateLine.getAllByRole('link', { name: 'foo:test2.js' })[0]).toBeInTheDocument();

  await act(async () => {
    await user.keyboard('[Escape]');
  });
  expect(duplicateLine.queryByRole('link', { name: 'foo:test2.js' })).not.toBeInTheDocument();
});

it('should highlight symbol', async () => {
  const user = userEvent.setup();
  renderSourceViewer({ component: 'foo:testSymb.tsx' });
  const symbols = await screen.findAllByText('symbole');
  await user.click(symbols[0]);

  // For now just check the class. Maybe found a better accessible way of showing higlighted symbole
  symbols.forEach((element) => {
    expect(element).toHaveClass('highlighted');
  });
});

it('should show correct message when component is not asscessible', async () => {
  componentsHandler.setFailLoadingComponentStatus(HttpStatus.Forbidden);
  renderSourceViewer();
  expect(
    await screen.findByText('code_viewer.no_source_code_displayed_due_to_security')
  ).toBeInTheDocument();
});

it('should show correct message when component does not exist', async () => {
  componentsHandler.setFailLoadingComponentStatus(HttpStatus.NotFound);
  renderSourceViewer();
  expect(await screen.findByText('component_viewer.no_component')).toBeInTheDocument();
});

function renderSourceViewer(override?: Partial<SourceViewer['props']>) {
  const { rerender } = renderComponent(
    <SourceViewer
      aroundLine={1}
      branchLike={undefined}
      component={componentsHandler.getNonEmptyFileKey()}
      displayAllIssues={true}
      displayIssueLocationsCount={true}
      displayIssueLocationsLink={false}
      displayLocationMarkers={true}
      onIssueChange={jest.fn()}
      onIssueSelect={jest.fn()}
      onLoaded={jest.fn()}
      onLocationSelect={jest.fn()}
      {...override}
    />
  );
  return function (reoverride?: Partial<SourceViewer['props']>) {
    rerender(
      <SourceViewer
        aroundLine={1}
        branchLike={undefined}
        component={componentsHandler.getNonEmptyFileKey()}
        displayAllIssues={true}
        displayIssueLocationsCount={true}
        displayIssueLocationsLink={false}
        displayLocationMarkers={true}
        onIssueChange={jest.fn()}
        onIssueSelect={jest.fn()}
        onLoaded={jest.fn()}
        onLocationSelect={jest.fn()}
        {...override}
        {...reoverride}
      />
    );
  };
}
