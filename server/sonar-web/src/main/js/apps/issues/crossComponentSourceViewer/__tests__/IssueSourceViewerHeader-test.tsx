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

import { screen, waitFor } from '@testing-library/react';
import * as React from 'react';
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import { ComponentContext } from '../../../../app/components/componentContext/ComponentContext';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockSourceViewerFile } from '../../../../helpers/mocks/sources';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import { Feature } from '../../../../types/features';
import { IssueSourceViewerHeader, Props } from '../IssueSourceViewerHeader';

const ui = {
  expandAllLines: byRole('button', { name: 'source_viewer.expand_all_lines' }),
  projectLink: byRole('link', { name: 'MyProject' }),
  projectName: byText('MyProject'),
  viewAllIssues: byRole('link', { name: 'source_viewer.view_all_issues' }),
};

const branchHandler = new BranchesServiceMock();

afterEach(() => {
  branchHandler.reset();
});

it('should render correctly', async () => {
  branchHandler.emptyBranchesAndPullRequest();

  renderIssueSourceViewerHeader();

  expect(await screen.findByText('loading')).toBeInTheDocument();
  await waitFor(() => expect(screen.queryByText('loading')).not.toBeInTheDocument());

  expect(ui.expandAllLines.get()).toBeInTheDocument();
  expect(ui.projectLink.get()).toBeInTheDocument();
  expect(ui.projectName.get()).toBeInTheDocument();
  expect(ui.viewAllIssues.get()).toBeInTheDocument();
});

it('should not render expandable link', async () => {
  renderIssueSourceViewerHeader({ expandable: false });

  expect(await screen.findByText('loading')).toBeInTheDocument();
  await waitFor(() => expect(screen.queryByText('loading')).not.toBeInTheDocument());

  expect(ui.expandAllLines.query()).not.toBeInTheDocument();
});

it('should not render link to project', async () => {
  renderIssueSourceViewerHeader({ linkToProject: false }, '?id=my-project&branch=normal-branch');

  expect(await screen.findByText('loading')).toBeInTheDocument();
  await waitFor(() => expect(screen.queryByText('loading')).not.toBeInTheDocument());

  expect(ui.projectLink.query()).not.toBeInTheDocument();
  expect(ui.projectName.get()).toBeInTheDocument();
});

it('should not render project name', async () => {
  renderIssueSourceViewerHeader({ displayProjectName: false }, '?id=my-project&pullRequest=01');

  expect(await screen.findByText('loading')).toBeInTheDocument();
  await waitFor(() => expect(screen.queryByText('loading')).not.toBeInTheDocument());

  expect(ui.projectLink.query()).not.toBeInTheDocument();
  expect(ui.projectName.query()).not.toBeInTheDocument();
});

it('should render without issue expand all when no issue', async () => {
  renderIssueSourceViewerHeader({
    sourceViewerFile: mockSourceViewerFile('foo/bar.ts', 'my-project', {
      measures: {},
    }),
  });

  expect(await screen.findByText('loading')).toBeInTheDocument();
  await waitFor(() => expect(screen.queryByText('loading')).not.toBeInTheDocument());

  expect(ui.viewAllIssues.query()).not.toBeInTheDocument();
});

function renderIssueSourceViewerHeader(props: Partial<Props> = {}, path = '?id=my-project') {
  return renderComponent(
    <AvailableFeaturesContext.Provider value={[Feature.BranchSupport]}>
      <ComponentContext.Provider
        value={{
          component: mockComponent(),
          onComponentChange: jest.fn(),
          fetchComponent: jest.fn(),
        }}
      >
        <IssueSourceViewerHeader
          expandable
          issueKey="issue-key"
          onExpand={jest.fn()}
          sourceViewerFile={mockSourceViewerFile('foo/bar.ts', 'my-project')}
          {...props}
        />
      </ComponentContext.Provider>
    </AvailableFeaturesContext.Provider>,
    path,
  );
}
