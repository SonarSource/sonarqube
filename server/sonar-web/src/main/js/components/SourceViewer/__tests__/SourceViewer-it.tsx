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
import { queryHelpers, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { SourceViewerServiceMock } from '../../../api/mocks/SourceViewerServiceMock';
import { mockIssue } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import SourceViewer from '../SourceViewer';
import SourceViewerBase from '../SourceViewerBase';

jest.mock('../../../api/components');
jest.mock('../helpers/lines', () => {
  const lines = jest.requireActual('../helpers/lines');
  return {
    ...lines,
    LINES_TO_LOAD: 20
  };
});

jest.setTimeout(30_000);
const handler = new SourceViewerServiceMock();

it('should show a permalink on line number', async () => {
  const user = userEvent.setup();
  renderSourceViewer();
  let row = await screen.findByRole('row', { name: /\/\*$/ });
  expect(row).toBeInTheDocument();
  const rowScreen = within(row);
  await user.click(
    rowScreen.getByRole('button', {
      name: 'source_viewer.line_X.1'
    })
  );
  await user.click(
    rowScreen.getByRole('link', {
      name: 'component_viewer.copy_permalink'
    })
  );

  expect(
    /* eslint-disable-next-line testing-library/prefer-presence-queries */
    queryHelpers.queryByAttribute(
      'data-clipboard-text',
      row,
      'http://localhost/code?id=project&selected=project%3Atest.js&line=1'
    )
  ).toBeInTheDocument();

  await user.keyboard('[Escape]');

  expect(
    /* eslint-disable-next-line testing-library/prefer-presence-queries */
    queryHelpers.queryByAttribute(
      'data-clipboard-text',
      row,
      'http://localhost/code?id=project&selected=project%3Atest.js&line=1'
    )
  ).not.toBeInTheDocument();

  row = await screen.findByRole('row', { name: / \* 6$/ });
  expect(row).toBeInTheDocument();
  const lowerRowScreen = within(row);
  await user.click(
    lowerRowScreen.getByRole('button', {
      name: 'source_viewer.line_X.6'
    })
  );

  expect(
    lowerRowScreen.getByRole('link', {
      name: 'component_viewer.copy_permalink'
    })
  ).toBeInTheDocument();

  await user.keyboard('[Escape]');
});

it('should show issue on empty file', async () => {
  renderSourceViewer({
    component: handler.getEmptyFile(),
    loadIssues: jest.fn().mockResolvedValue([
      mockIssue(false, {
        key: 'first-issue',
        message: 'First Issue',
        line: undefined,
        textRange: undefined
      })
    ])
  });
  expect(await screen.findByRole('table')).toBeInTheDocument();
  expect(await screen.findByRole('row', { name: 'First Issue' })).toBeInTheDocument();
});

it('should load line when looking arround unloaded line', async () => {
  const { rerender } = renderSourceViewer({
    aroundLine: 50,
    component: handler.getHugeFile()
  });
  expect(await screen.findByRole('row', { name: /Line 50$/ })).toBeInTheDocument();
  rerender(getSourceViewerUi({ aroundLine: 100, component: handler.getHugeFile() }));

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
      name: 'source_viewer.author_X.stas.vilchik@sonarsource.com, source_viewer.click_for_scm_info'
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
  const thirdRowScreen = within(row);
  await user.click(
    thirdRowScreen.getByRole('button', {
      name: 'source_viewer.author_X.stas.vilchik@sonarsource.com, source_viewer.click_for_scm_info'
    })
  );

  expect(
    await thirdRowScreen.findByRole('heading', { level: 4, name: 'author' })
  ).toBeInTheDocument();
  expect(
    thirdRowScreen.queryByRole('heading', {
      level: 4,
      name: 'source_viewer.tooltip.scm.commited_on'
    })
  ).not.toBeInTheDocument();
  expect(
    thirdRowScreen.getByRole('heading', { level: 4, name: 'source_viewer.tooltip.scm.revision' })
  ).toBeInTheDocument();

  // SCM with no date no author
  row = await screen.findByRole('row', { name: /\* 5$/ });
  expect(row).toBeInTheDocument();
  const fourthRowScreen = within(row);
  expect(fourthRowScreen.getByText('…')).toBeInTheDocument();
  await user.click(
    fourthRowScreen.getByRole('button', {
      name: 'source_viewer.click_for_scm_info'
    })
  );

  expect(
    fourthRowScreen.queryByRole('heading', { level: 4, name: 'author' })
  ).not.toBeInTheDocument();
  expect(
    fourthRowScreen.queryByRole('heading', {
      level: 4,
      name: 'source_viewer.tooltip.scm.commited_on'
    })
  ).not.toBeInTheDocument();
  expect(
    fourthRowScreen.getByRole('heading', { level: 4, name: 'source_viewer.tooltip.scm.revision' })
  ).toBeInTheDocument();

  // No SCM Popup
  row = await screen.findByRole('row', {
    name: /\* This program is free software; you can redistribute it and\/or$/
  });
  expect(row).toBeInTheDocument();
  expect(within(row).queryByRole('button')).not.toBeInTheDocument();
});

it('should show issue indicator', async () => {
  const user = userEvent.setup();
  const onIssueSelect = jest.fn();
  renderSourceViewer({
    onIssueSelect,
    displayAllIssues: false,
    loadIssues: jest.fn().mockResolvedValue([
      mockIssue(false, {
        key: 'first-issue',
        message: 'First Issue',
        line: 1,
        textRange: { startLine: 1, endLine: 1, startOffset: 0, endOffset: 1 }
      }),
      mockIssue(false, {
        key: 'second-issue',
        message: 'Second Issue',
        line: 1,
        textRange: { startLine: 1, endLine: 1, startOffset: 1, endOffset: 2 }
      })
    ])
  });
  const row = await screen.findByRole('row', { name: /.*\/ \*$/ });
  const issueRow = within(row);
  expect(issueRow.getByText('2')).toBeInTheDocument();
  await user.click(issueRow.getByRole('button', { name: 'source_viewer.issues_on_line.show' }));
  const firstIssueBox = issueRow.getByRole('region', { name: 'First Issue' });
  const secondIssueBox = issueRow.getByRole('region', { name: 'Second Issue' });
  expect(firstIssueBox).toBeInTheDocument();
  expect(secondIssueBox).toBeInTheDocument();

  await user.click(firstIssueBox);
  expect(onIssueSelect).toBeCalledWith('first-issue');

  await user.click(secondIssueBox);
  expect(onIssueSelect).toBeCalledWith('second-issue');
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

  expect(duplicateLine.getAllByRole('link', { name: 'test2.js' })[0]).toBeInTheDocument();
  await user.keyboard('[Escape]');
  expect(duplicateLine.queryByRole('link', { name: 'test2.js' })).not.toBeInTheDocument();
});

function renderSourceViewer(override?: Partial<SourceViewerBase['props']>) {
  return renderComponent(getSourceViewerUi(override));
}

function getSourceViewerUi(override?: Partial<SourceViewerBase['props']>) {
  return (
    <SourceViewer
      aroundLine={1}
      branchLike={undefined}
      component={handler.getFileWithSource()}
      displayAllIssues={true}
      displayIssueLocationsCount={true}
      displayIssueLocationsLink={false}
      displayLocationMarkers={true}
      loadIssues={jest.fn().mockResolvedValue([])}
      onIssueChange={jest.fn()}
      onIssueSelect={jest.fn()}
      onLoaded={jest.fn()}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      slimHeader={true}
      {...override}
    />
  );
}
