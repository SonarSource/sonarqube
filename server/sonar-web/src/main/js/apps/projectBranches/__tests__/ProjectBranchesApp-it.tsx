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

import { act, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React, { useEffect, useState } from 'react';
import { byRole } from 'testing-library-selector';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import BranchStatusContextProvider from '../../../app/components/branch-status/BranchStatusContextProvider';
import withBranchStatusActions from '../../../app/components/branch-status/withBranchStatusActions';
import { ComponentContext } from '../../../app/components/componentContext/ComponentContext';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockAppState } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { AppState } from '../../../types/appstate';
import { BranchLike } from '../../../types/branch-like';
import { SettingsKey } from '../../../types/settings';
import ProjectBranchesApp from '../ProjectBranchesApp';

const handler = new BranchesServiceMock();
const settingsHandler = new SettingsServiceMock();

const ui = {
  branchTabContent: byRole('tabpanel', { name: 'project_branch_pull_request.tabs.branches' }),
  branchTabBtn: byRole('tab', { name: 'project_branch_pull_request.tabs.branches' }),
  linkForAdmin: byRole('link', { name: 'settings.page' }),
  renameBranchBtn: byRole('button', { name: 'project_branch_pull_request.branch.rename' }),
  deleteBranchBtn: byRole('button', { name: 'project_branch_pull_request.branch.delete' }),
  deletePullRequestBtn: byRole('button', {
    name: 'project_branch_pull_request.pull_request.delete',
  }),
  pullRequestTabContent: byRole('tabpanel', {
    name: 'project_branch_pull_request.tabs.pull_requests',
  }),
  pullRequestTabBtn: byRole('tab', {
    name: 'project_branch_pull_request.tabs.pull_requests',
  }),
  renameBranchDialog: byRole('dialog', { name: 'project_branch_pull_request.branch.rename' }),
  deleteBranchDialog: byRole('dialog', { name: 'project_branch_pull_request.branch.delete' }),
  deletePullRequestDialog: byRole('dialog', {
    name: 'project_branch_pull_request.pull_request.delete',
  }),
  updateMasterBtn: byRole('button', {
    name: 'project_branch_pull_request.branch.actions_label.master',
  }),
  updateSecondBranchBtn: byRole('button', {
    name: 'project_branch_pull_request.branch.actions_label.delete-branch',
  }),
  updateFirstPRBtn: byRole('button', {
    name: 'project_branch_pull_request.branch.actions_label.01 – TEST-191 update master',
  }),
  getBranchRow: () => within(ui.branchTabContent.get()).getAllByRole('row'),
  getPullRequestRow: () => within(ui.pullRequestTabContent.get()).getAllByRole('row'),
};

beforeAll(() => {
  jest.useFakeTimers({
    advanceTimers: true,
    now: new Date('2018-02-01T07:08:59Z'),
  });
});

beforeEach(() => {
  handler.reset();
  settingsHandler.reset();
});

it('should show all branches', async () => {
  renderProjectBranchesApp();
  expect(await ui.branchTabContent.find()).toBeInTheDocument();
  expect(ui.pullRequestTabContent.query()).not.toBeInTheDocument();
  expect(ui.linkForAdmin.query()).not.toBeInTheDocument();
  expect(ui.getBranchRow()).toHaveLength(4);
  expect(ui.getBranchRow()[1]).toHaveTextContent('masterbranches.main_branchOK1 month ago');
  expect(within(ui.getBranchRow()[1]).getByRole('switch')).toBeDisabled();
  expect(within(ui.getBranchRow()[1]).getByRole('switch')).toBeChecked();
  expect(ui.getBranchRow()[2]).toHaveTextContent('delete-branchERROR2 days ago');
  expect(within(ui.getBranchRow()[2]).getByRole('switch')).toBeEnabled();
  expect(within(ui.getBranchRow()[2]).getByRole('switch')).not.toBeChecked();
});

it('should show link to change purge options for admin', async () => {
  settingsHandler.set(SettingsKey.DaysBeforeDeletingInactiveBranchesAndPRs, '3');
  renderProjectBranchesApp({ canAdmin: true });
  expect(await ui.linkForAdmin.find()).toBeInTheDocument();
});

it('should be able to rename main branch, but not others', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  await user.click(await ui.updateMasterBtn.find());
  expect(ui.renameBranchBtn.get()).toBeInTheDocument();
  await user.click(ui.renameBranchBtn.get());
  expect(ui.renameBranchDialog.get()).toBeInTheDocument();
  expect(within(ui.renameBranchDialog.get()).getByRole('textbox')).toHaveValue('master');
  expect(
    within(ui.renameBranchDialog.get()).getByRole('button', { name: 'rename' })
  ).toBeDisabled();
  await user.clear(within(ui.renameBranchDialog.get()).getByRole('textbox'));
  expect(
    within(ui.renameBranchDialog.get()).getByRole('button', { name: 'rename' })
  ).toBeDisabled();
  await user.type(within(ui.renameBranchDialog.get()).getByRole('textbox'), 'main');
  expect(within(ui.renameBranchDialog.get()).getByRole('button', { name: 'rename' })).toBeEnabled();
  await act(() =>
    user.click(within(ui.renameBranchDialog.get()).getByRole('button', { name: 'rename' }))
  );
  expect(ui.getBranchRow()[1]).toHaveTextContent('mainbranches.main_branchOK1 month ago');

  await user.click(await ui.updateSecondBranchBtn.find());
  expect(ui.renameBranchBtn.query()).not.toBeInTheDocument();
});

it('should be able to delete branch, but not main', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  await user.click(await ui.updateSecondBranchBtn.find());
  expect(ui.deleteBranchBtn.get()).toBeInTheDocument();
  await user.click(ui.deleteBranchBtn.get());
  expect(ui.deleteBranchDialog.get()).toBeInTheDocument();
  expect(ui.deleteBranchDialog.get()).toHaveTextContent('delete-branch');
  await act(() =>
    user.click(within(ui.deleteBranchDialog.get()).getByRole('button', { name: 'delete' }))
  );
  expect(ui.getBranchRow()).toHaveLength(3);

  await user.click(await ui.updateMasterBtn.find());
  expect(ui.deleteBranchBtn.query()).not.toBeInTheDocument();
});

it('should exclude from purge', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  expect(await ui.branchTabContent.find()).toBeInTheDocument();
  expect(within(ui.getBranchRow()[2]).getByRole('switch')).not.toBeChecked();
  await act(() => user.click(within(ui.getBranchRow()[2]).getByRole('switch')));
  expect(within(ui.getBranchRow()[2]).getByRole('switch')).toBeChecked();

  expect(within(ui.getBranchRow()[3]).getByRole('switch')).toBeChecked();
  await act(() => user.click(within(ui.getBranchRow()[3]).getByRole('switch')));
  expect(within(ui.getBranchRow()[3]).getByRole('switch')).not.toBeChecked();

  await user.click(ui.pullRequestTabBtn.get());
  await user.click(ui.branchTabBtn.get());
  expect(within(ui.getBranchRow()[2]).getByRole('switch')).toBeChecked();
  expect(within(ui.getBranchRow()[3]).getByRole('switch')).not.toBeChecked();
});

it('should show all pull requests', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  await user.click(await ui.pullRequestTabBtn.find());
  expect(await ui.pullRequestTabContent.find()).toBeInTheDocument();
  expect(ui.branchTabContent.query()).not.toBeInTheDocument();
  expect(ui.getPullRequestRow()).toHaveLength(4);
  expect(ui.getPullRequestRow()[1]).toHaveTextContent('01 – TEST-191 update masterOK1 month ago');
  expect(ui.getPullRequestRow()[2]).toHaveTextContent(
    '02 – TEST-192 update normal-branchERROR2 days ago'
  );
});

it('should delete pull requests', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  await user.click(await ui.pullRequestTabBtn.find());
  expect(ui.getPullRequestRow()).toHaveLength(4);
  await user.click(ui.updateFirstPRBtn.get());
  await user.click(ui.deletePullRequestBtn.get());
  expect(await ui.deletePullRequestDialog.find()).toBeInTheDocument();
  expect(ui.deletePullRequestDialog.get()).toHaveTextContent('01 – TEST-191 update master');
  await act(() =>
    user.click(within(ui.deletePullRequestDialog.get()).getByRole('button', { name: 'delete' }))
  );
  expect(ui.getPullRequestRow()).toHaveLength(3);
});

function renderProjectBranchesApp(overrides?: Partial<AppState>) {
  function TestWrapper(props: any) {
    const [init, setInit] = useState<boolean>(false);
    const [branches, setBranches] = useState<BranchLike[]>([
      ...handler.branches,
      ...handler.pullRequests,
    ]);

    const updateBranches = (branches: BranchLike[]) => {
      branches.forEach((item) => {
        props.updateBranchStatus(item, 'my-project', item.status?.qualityGateStatus);
      });
    };

    useEffect(() => {
      updateBranches(branches);
      setInit(true);
    }, []);

    const onBranchesChange = () => {
      const changedBranches = [...handler.branches, ...handler.pullRequests];
      updateBranches(changedBranches);
      setBranches(changedBranches);
    };

    return init ? (
      <ComponentContext.Provider
        value={{
          branchLikes: branches,
          onBranchesChange,
          onComponentChange: jest.fn(),
          component: mockComponent(),
        }}
      >
        {props.children}
      </ComponentContext.Provider>
    ) : null;
  }

  const Wrapper = withBranchStatusActions(TestWrapper);

  return renderComponent(
    <BranchStatusContextProvider>
      <Wrapper>
        <ProjectBranchesApp />
      </Wrapper>
    </BranchStatusContextProvider>,
    '/',
    { appState: mockAppState(overrides) }
  );
}
