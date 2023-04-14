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
import * as React from 'react';
import { byRole, byText } from 'testing-library-selector';
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockSourceViewerFile } from '../../../../helpers/mocks/sources';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import IssueSourceViewerHeader, { Props } from '../IssueSourceViewerHeader';

const ui = {
  expandAllLines: byRole('button', { name: 'source_viewer.expand_all_lines' }),
  projectLink: byRole('link', { name: 'qualifier.TRK MyProject' }),
  projectName: byText('MyProject'),
  viewAllIssues: byRole('link', { name: 'source_viewer.view_all_issues' }),
};

it('should render correctly', () => {
  renderIssueSourceViewerHeader();

  expect(ui.expandAllLines.get()).toBeInTheDocument();
  expect(ui.projectLink.get()).toBeInTheDocument();
  expect(ui.projectName.get()).toBeInTheDocument();
  expect(ui.viewAllIssues.get()).toBeInTheDocument();
});

it('should not render expandable link', () => {
  renderIssueSourceViewerHeader({ expandable: false });

  expect(ui.expandAllLines.query()).not.toBeInTheDocument();
});

it('should not render link to project', () => {
  renderIssueSourceViewerHeader({ linkToProject: false });

  expect(ui.projectLink.query()).not.toBeInTheDocument();
  expect(ui.projectName.get()).toBeInTheDocument();
});

it('should not render project name', () => {
  renderIssueSourceViewerHeader({ displayProjectName: false });

  expect(ui.projectLink.query()).not.toBeInTheDocument();
  expect(ui.projectName.query()).not.toBeInTheDocument();
});

it('should render without issue expand all when no issue', () => {
  renderIssueSourceViewerHeader({
    sourceViewerFile: mockSourceViewerFile('foo/bar.ts', 'my-project', {
      measures: {},
    }),
  });

  expect(ui.viewAllIssues.query()).not.toBeInTheDocument();
});

function renderIssueSourceViewerHeader(props: Partial<Props> = {}) {
  return renderComponent(
    <IssueSourceViewerHeader
      branchLike={mockMainBranch()}
      expandable={true}
      onExpand={jest.fn()}
      sourceViewerFile={mockSourceViewerFile('foo/bar.ts', 'my-project')}
      {...props}
    />
  );
}
