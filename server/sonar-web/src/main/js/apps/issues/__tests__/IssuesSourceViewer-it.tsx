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
import userEvent from '@testing-library/user-event';
import { keyBy } from 'lodash';
import { byLabelText, byRole } from '~sonar-aligned/helpers/testSelector';
import { PARENT_COMPONENT_KEY, RULE_1 } from '../../../api/mocks/data/ids';
import { mockIpynbFile } from '../../../api/mocks/data/sources';
import { mockSourceLine, mockSourceViewerFile } from '../../../helpers/mocks/sources';
import { mockRawIssue } from '../../../helpers/testMocks';
import { IssueStatus } from '../../../types/issues';
import {
  componentsHandler,
  issuesHandler,
  renderProjectIssuesApp,
  sourcesHandler,
  usersHandler,
  waitOnDataLoaded,
} from '../test-utils';

beforeEach(() => {
  issuesHandler.reset();
  componentsHandler.reset();
  sourcesHandler.reset();
  usersHandler.reset();
  window.scrollTo = jest.fn();
  window.HTMLElement.prototype.scrollTo = jest.fn();
});

const ui = {
  expandAllLines: byRole('button', { name: 'source_viewer.expand_all_lines' }),
  expandLinesAbove: byRole('button', { name: 'source_viewer.expand_above' }),
  expandLinesBelow: byRole('button', { name: 'source_viewer.expand_below' }),

  preview: byRole('radio', { name: 'preview' }),
  code: byRole('radio', { name: 'code' }),

  line1: byLabelText('source_viewer.line_X.1'),
  line44: byLabelText('source_viewer.line_X.44'),
  line45: byLabelText('source_viewer.line_X.45'),
  line60: byLabelText('source_viewer.line_X.60'),
  line199: byLabelText('source_viewer.line_X.199'),

  scmInfoLine60: byLabelText(
    'source_viewer.author_X.simon.brandhof@sonarsource.com, source_viewer.click_for_scm_info.1',
  ),
};

const JUPYTER_ISSUE = {
  issue: mockRawIssue(false, {
    key: 'some-issue',
    component: `${PARENT_COMPONENT_KEY}:jpt.ipynb`,
    message: 'Issue on Jupyter Notebook',
    rule: RULE_1,
    textRange: {
      startLine: 1,
      endLine: 1,
      startOffset: 1148,
      endOffset: 1159,
    },
    ruleDescriptionContextKey: 'spring',
    ruleStatus: 'DEPRECATED',
    quickFixAvailable: true,
    tags: ['unused'],
    project: 'org.sonarsource.javascript:javascript',
    assignee: 'email1@sonarsource.com',
    author: 'email3@sonarsource.com',
    issueStatus: IssueStatus.Confirmed,
    prioritizedRule: true,
  }),
  snippets: keyBy(
    [
      {
        component: mockSourceViewerFile('jpt.ipynb', PARENT_COMPONENT_KEY),
        sources: {
          1: mockSourceLine({ line: 1, code: mockIpynbFile }),
        },
      },
    ],
    'component.key',
  ),
};

describe('issues source viewer', () => {
  it('should show source across components', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue101&open=issue101&id=myproject');
    await waitOnDataLoaded();

    expect(await screen.findByLabelText('test1.js')).toBeInTheDocument();
    expect(screen.getByLabelText('test2.js')).toBeInTheDocument();

    // Both line 1 of test1.js and test2.js should be rendered after expanding lines above snippet in test2.js
    expect(ui.line1.getAll()).toHaveLength(1);
    await user.click(ui.expandLinesAbove.get());
    expect(ui.line1.getAll()).toHaveLength(2);
  });

  it('should expand a few lines and show SCM info', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue1&open=issue1&id=myproject');
    await waitOnDataLoaded();

    expect(ui.line44.query()).not.toBeInTheDocument();
    expect(ui.line45.get()).toBeInTheDocument();
    expect(ui.line199.query()).not.toBeInTheDocument();

    // Expand lines below snippet
    const expandBelowSecondSnippet = ui.expandLinesBelow.getAll()[1];
    await user.click(expandBelowSecondSnippet);
    expect(ui.line60.get()).toBeInTheDocument();
    expect(ui.line199.query()).not.toBeInTheDocument(); // Expand should only expand a few lines, not all of them

    // Show SCM info for newly expanded line
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    await user.click(ui.scmInfoLine60.get());
    expect(screen.getByRole('tooltip')).toBeVisible();
  });

  it('should expand all lines', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue1&open=issue1&id=myproject');
    await waitOnDataLoaded();

    expect(ui.line199.query()).not.toBeInTheDocument();

    await user.click(ui.expandAllLines.get());

    // All lines should be rendered now
    expect(ui.line199.get()).toBeInTheDocument();
  });

  it('should merge snippet viewers when expanding one near another', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue1&open=issue1&id=myproject');
    await waitOnDataLoaded();

    // Line 44 is between both snippets, it should not be shown
    expect(ui.line44.query()).not.toBeInTheDocument();

    // There currently are two snippet shown
    expect(screen.getAllByRole('table')).toHaveLength(2);

    // Expand lines above second snippet
    await user.click(ui.expandLinesAbove.get());

    // Line 44 should now be shown
    expect(ui.line44.get()).toBeInTheDocument();

    // Snippets should be automatically merged
    // eslint-disable-next-line jest-dom/prefer-in-document
    expect(screen.getAllByRole('table')).toHaveLength(1);
  });

  describe('should render jupyter notebook issues correctly', () => {
    it('should render error when jupyter issue can not be parsed', async () => {
      issuesHandler.setIssueList([JUPYTER_ISSUE]);
      sourcesHandler.setSource('{not a JSON file}');
      renderProjectIssuesApp('project/issues?issues=some-issue&open=some-issue&id=myproject');
      await waitOnDataLoaded();

      // Preview tab should be shown
      expect(ui.preview.get()).toBeChecked();
      expect(ui.code.get()).toBeInTheDocument();

      expect(
        await screen.findByRole('button', { name: 'Issue on Jupyter Notebook' }),
      ).toBeInTheDocument();

      expect(screen.getByText('issue.preview.jupyter_notebook.error')).toBeInTheDocument();
    });

    it('should render error when jupyter issue can not be found', async () => {
      issuesHandler.setIssueList([
        {
          ...JUPYTER_ISSUE,
          issue: {
            ...JUPYTER_ISSUE.issue,
            textRange: {
              startLine: 2,
              endLine: 2,
              startOffset: 1,
              endOffset: 1,
            },
          },
        },
      ]);
      renderProjectIssuesApp('project/issues?issues=some-issue&open=some-issue&id=myproject');
      await waitOnDataLoaded();

      // Preview tab should be shown
      expect(ui.preview.get()).toBeChecked();
      expect(ui.code.get()).toBeInTheDocument();

      expect(
        await screen.findByRole('button', { name: 'Issue on Jupyter Notebook' }),
      ).toBeInTheDocument();

      expect(screen.getByText('issue.preview.jupyter_notebook.error')).toBeInTheDocument();
    });

    it('should show preview tab when jupyter notebook issue', async () => {
      issuesHandler.setIssueList([JUPYTER_ISSUE]);
      renderProjectIssuesApp('project/issues?issues=some-issue&open=some-issue&id=myproject');
      await waitOnDataLoaded();

      // Preview tab should be shown
      expect(ui.preview.get()).toBeChecked();
      expect(ui.code.get()).toBeInTheDocument();

      expect(
        await screen.findByRole('button', { name: 'Issue on Jupyter Notebook' }),
      ).toBeInTheDocument();

      expect(screen.queryByText('issue.preview.jupyter_notebook.error')).not.toBeInTheDocument();
      expect(screen.getByTestId('hljs-sonar-underline')).toHaveTextContent('matplotlib');
      expect(screen.getByText(/pylab/, { exact: false })).toBeInTheDocument();
    });

    it('should not show non-selected issues in code tab of Issues page', async () => {
      issuesHandler.setIssueList([
        JUPYTER_ISSUE,
        {
          ...JUPYTER_ISSUE,
          issue: {
            ...JUPYTER_ISSUE.issue,
            key: 'some-other-issue',
            message: 'Another unrelated issue',
          },
        },
      ]);
      const user = userEvent.setup();
      renderProjectIssuesApp('project/issues?issues=some-issue&open=some-issue&id=myproject');
      await waitOnDataLoaded();

      await user.click(ui.code.get());

      expect(screen.getAllByRole('button', { name: 'Issue on Jupyter Notebook' })).toHaveLength(2);
      expect(screen.queryByText('Another unrelated issue')).not.toBeInTheDocument();
    });

    it('should render issue in jupyter notebook spanning over multiple cells', async () => {
      issuesHandler.setIssueList([
        {
          ...JUPYTER_ISSUE,
          issue: {
            ...JUPYTER_ISSUE.issue,
            textRange: {
              startLine: 1,
              endLine: 1,
              startOffset: 571,
              endOffset: JUPYTER_ISSUE.issue.textRange!.endOffset,
            },
          },
        },
      ]);
      renderProjectIssuesApp('project/issues?issues=some-issue&open=some-issue&id=myproject');
      await waitOnDataLoaded();

      // Preview tab should be shown
      expect(ui.preview.get()).toBeChecked();
      expect(ui.code.get()).toBeInTheDocument();

      expect(
        await screen.findByRole('button', { name: 'Issue on Jupyter Notebook' }),
      ).toBeInTheDocument();

      expect(screen.queryByText('issue.preview.jupyter_notebook.error')).not.toBeInTheDocument();

      const underlined = screen.getAllByTestId('hljs-sonar-underline');
      expect(underlined).toHaveLength(4);
      expect(underlined[0]).toHaveTextContent('print train.shape');
      expect(underlined[1]).toHaveTextContent('print test.shap');
      expect(underlined[2]).toHaveTextContent('import pylab as pl');
      expect(underlined[3]).toHaveTextContent('%matplotlib');
    });
  });
});
