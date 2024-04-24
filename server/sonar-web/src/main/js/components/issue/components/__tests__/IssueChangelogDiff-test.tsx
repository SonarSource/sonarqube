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
import { mockIssueChangelogDiff } from '../../../../helpers/mocks/issues';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import IssueChangelogDiff, { IssueChangelogDiffProps } from '../IssueChangelogDiff';

jest.mock('~sonar-aligned/helpers/measures', () => ({
  formatMeasure: jest
    .fn()
    .mockImplementation((value: string, type: string) => `formatted.${value}.as.${type}`),
}));

it.each([
  ['file', 'issue.change.file_move.oldValue.newValue', undefined],
  ['from_branch', 'issue.change.from_branch.oldValue.newValue', undefined],
  ['line', 'issue.changelog.line_removed_X.oldValue', undefined],
  [
    'effort',
    'issue.changelog.changed_to.issue.changelog.field.effort.formatted.12.as.WORK_DUR',
    { newValue: '12', oldValue: undefined },
  ],
  [
    'effort',
    'issue.changelog.removed.issue.changelog.field.effort (issue.changelog.was.formatted.14.as.WORK_DUR)',
    { newValue: undefined, oldValue: '14' },
  ],
  [
    'effort',
    'issue.changelog.removed.issue.changelog.field.effort',
    { newValue: undefined, oldValue: undefined },
  ],
  [
    'assign',
    'issue.changelog.changed_to.issue.changelog.field.assign.newValue (issue.changelog.was.oldValue)',
    undefined,
  ],
  ['from_short_branch', 'issue.change.from_non_branch.oldValue.newValue', undefined],

  // This should be deprecated. Can this still happen?
  ['from_long_branch', 'issue.change.from_branch.oldValue.newValue', undefined],
])(
  'should render correctly for "%s" diff types',
  (key, expected, diff?: Partial<IssueChangelogDiffProps['diff']>) => {
    renderIssueChangelogDiff({
      diff: mockIssueChangelogDiff({ key, newValue: 'newValue', oldValue: 'oldValue', ...diff }),
    });
    expect(screen.getByText(expected)).toBeInTheDocument();
  },
);

function renderIssueChangelogDiff(props: Partial<IssueChangelogDiffProps> = {}) {
  return renderComponent(<IssueChangelogDiff diff={mockIssueChangelogDiff()} {...props} />);
}
