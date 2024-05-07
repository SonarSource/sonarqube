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
import { byLabelText, byRole } from '~sonar-aligned/helpers/testSelector';
import {
  componentsHandler,
  issuesHandler,
  renderProjectIssuesApp,
  usersHandler,
  waitOnDataLoaded,
} from '../test-utils';

beforeEach(() => {
  issuesHandler.reset();
  componentsHandler.reset();
  usersHandler.reset();
  window.scrollTo = jest.fn();
  window.HTMLElement.prototype.scrollTo = jest.fn();
});

const ui = {
  expandAllLines: byRole('button', { name: 'source_viewer.expand_all_lines' }),
  expandLinesAbove: byRole('button', { name: 'source_viewer.expand_above' }),
  expandLinesBelow: byRole('button', { name: 'source_viewer.expand_below' }),

  line1: byLabelText('source_viewer.line_X.1'),
  line44: byLabelText('source_viewer.line_X.44'),
  line45: byLabelText('source_viewer.line_X.45'),
  line60: byLabelText('source_viewer.line_X.60'),
  line199: byLabelText('source_viewer.line_X.199'),

  scmInfoLine60: byLabelText(
    'source_viewer.author_X.simon.brandhof@sonarsource.com, source_viewer.click_for_scm_info.1',
  ),
};

describe('issues source viewer', () => {
  it('should show source across components', async () => {
    const user = userEvent.setup();
    renderProjectIssuesApp('project/issues?issues=issue101&open=issue101&id=myproject');
    await waitOnDataLoaded();

    expect(screen.getByRole('separator', { name: 'test1.js' })).toBeInTheDocument();
    expect(screen.getByRole('separator', { name: 'test2.js' })).toBeInTheDocument();

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
});
