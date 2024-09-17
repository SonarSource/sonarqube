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
import { within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { byLabelText, byRole } from '~sonar-aligned/helpers/testSelector';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { ComponentContext } from '../../../app/components/componentContext/ComponentContext';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockAppState } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { AppState } from '../../../types/appstate';
import { Feature } from '../../../types/features';
import { SettingsKey } from '../../../types/settings';
import ProjectBranchesApp from '../ProjectBranchesApp';

const handler = new BranchesServiceMock();
const settingsHandler = new SettingsServiceMock();

const ui = new (class UI {
  branchTabContent = byRole('tabpanel', { name: 'project_branch_pull_request.tabs.branches' });
  branchTabBtn = byRole('tab', { name: 'project_branch_pull_request.tabs.branches' });
  linkForAdmin = byRole('link', { name: 'settings.page' });
  renameBranchBtn = byRole('menuitem', { name: 'project_branch_pull_request.branch.rename' });
  deleteBranchBtn = byRole('menuitem', { name: 'project_branch_pull_request.branch.delete' });
  deletePullRequestBtn = byRole('menuitem', {
    name: 'project_branch_pull_request.pull_request.delete',
  });

  pullRequestTabContent = byRole('tabpanel', {
    name: 'project_branch_pull_request.tabs.pull_requests',
  });

  pullRequestTabBtn = byRole('tab', {
    name: 'project_branch_pull_request.tabs.pull_requests',
  });

  renameBranchDialog = byRole('dialog', { name: 'project_branch_pull_request.branch.rename' });
  deleteBranchDialog = byRole('dialog', { name: 'project_branch_pull_request.branch.delete' });
  deletePullRequestDialog = byRole('dialog', {
    name: 'project_branch_pull_request.pull_request.delete',
  });

  updateMasterBtn = byRole('button', {
    name: 'project_branch_pull_request.branch.actions_label.main',
  });

  updateSecondBranchBtn = byRole('button', {
    name: 'project_branch_pull_request.branch.actions_label.delete-branch',
  });

  updateFirstPRBtn = byRole('button', {
    name: 'project_branch_pull_request.branch.actions_label.01 – TEST-191 update master',
  });

  branchRow = this.branchTabContent.byRole('row');
  pullRequestRow = this.pullRequestTabContent.byRole('row');

  getBranchRow = (name: string | RegExp) =>
    within(this.branchTabContent.get()).getByRole('row', { name });

  setMainBranchBtn = byRole('menuitem', { name: 'project_branch_pull_request.branch.set_main' });
  dialog = byRole('dialog');
})();

beforeEach(() => {
  jest.useFakeTimers({
    advanceTimers: true,
    now: new Date('2018-02-01T07:08:59Z'),
  });
  handler.reset();
  settingsHandler.reset();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

it('should show all branches', async () => {
  renderProjectBranchesApp();
  expect(await ui.branchTabContent.find()).toBeInTheDocument();
  expect(ui.pullRequestTabContent.query()).not.toBeInTheDocument();
  expect(ui.linkForAdmin.query()).not.toBeInTheDocument();
  expect(await ui.branchRow.findAll()).toHaveLength(4);
  expect(ui.branchRow.getAt(1)).toHaveTextContent(
    'mainbranches.main_branchoverview.quality_gate_x.metric.level.OKOK1 month ago',
  );
  await expect(byLabelText('help').get()).toHaveATooltipWithContent(
    'project_branch_pull_request.branch.auto_deletion.main_branch_tooltip',
  );
  expect(within(ui.branchRow.getAt(1)).getByRole('switch')).toBeDisabled();
  expect(within(ui.branchRow.getAt(1)).getByRole('switch')).toBeChecked();
  expect(ui.branchRow.getAt(2)).toHaveTextContent(
    'delete-branchoverview.quality_gate_x.metric.level.ERRORERROR2 days ago',
  );
  expect(within(ui.branchRow.getAt(2)).getByRole('switch')).toBeEnabled();
  expect(within(ui.branchRow.getAt(2)).getByRole('switch')).not.toBeChecked();
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
  expect(within(ui.renameBranchDialog.get()).getByRole('textbox')).toHaveValue('main');
  expect(
    within(ui.renameBranchDialog.get()).getByRole('button', { name: 'rename' }),
  ).toBeDisabled();
  await user.clear(within(ui.renameBranchDialog.get()).getByRole('textbox'));
  expect(
    within(ui.renameBranchDialog.get()).getByRole('button', { name: 'rename' }),
  ).toBeDisabled();
  await user.type(within(ui.renameBranchDialog.get()).getByRole('textbox'), 'develop');
  expect(within(ui.renameBranchDialog.get()).getByRole('button', { name: 'rename' })).toBeEnabled();
  await user.click(ui.renameBranchDialog.byRole('button', { name: 'rename' }).get());
  expect(ui.branchRow.getAt(1)).toHaveTextContent(
    'developbranches.main_branchoverview.quality_gate_x.metric.level.OKOK1 month ago',
  );
  await expect(byLabelText('help').get()).toHaveATooltipWithContent(
    'project_branch_pull_request.branch.auto_deletion.main_branch_tooltip',
  );

  await user.click(await ui.updateSecondBranchBtn.find());
  expect(ui.renameBranchBtn.query()).not.toBeInTheDocument();
});

it('should be able to set a branch as the main branch', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();

  // Cannot set main branch as main branch.
  await user.click(await ui.updateMasterBtn.find());
  expect(ui.setMainBranchBtn.query()).not.toBeInTheDocument();
  expect(ui.getBranchRow(/^main/)).toBeInTheDocument();
  expect(within(ui.getBranchRow(/^main/)).getByText('branches.main_branch')).toBeInTheDocument();
  expect(within(ui.getBranchRow(/^main/)).getByRole('switch')).toBeChecked();
  expect(within(ui.getBranchRow(/^main/)).getByRole('switch')).toBeDisabled();

  // Change main branch.
  await user.click(await ui.updateSecondBranchBtn.find());
  await user.click(ui.setMainBranchBtn.get());
  await user.click(
    ui.dialog
      .byRole('button', {
        name: 'project_branch_pull_request.branch.set_main',
      })
      .get(),
  );

  // "delete-branch" is now the main branch.
  expect(ui.getBranchRow(/delete-branch/)).toBeInTheDocument();
  expect(
    within(ui.getBranchRow(/delete-branch/)).getByText('branches.main_branch'),
  ).toBeInTheDocument();
  expect(within(ui.getBranchRow(/delete-branch/)).getByRole('switch')).toBeChecked();
  expect(within(ui.getBranchRow(/delete-branch/)).getByRole('switch')).toBeDisabled();

  // "main" is now excluded from purge
  expect(within(ui.getBranchRow(/^main/)).getByRole('switch')).toBeChecked();
});

it('should be able to delete branch, but not main', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  await user.click(await ui.updateSecondBranchBtn.find());
  expect(ui.deleteBranchBtn.get()).toBeInTheDocument();
  await user.click(ui.deleteBranchBtn.get());
  expect(ui.deleteBranchDialog.get()).toBeInTheDocument();
  expect(ui.deleteBranchDialog.get()).toHaveTextContent('delete-branch');
  await user.click(ui.deleteBranchDialog.byRole('button', { name: 'delete' }).get());
  expect(ui.branchRow.getAll()).toHaveLength(3);

  await user.click(await ui.updateMasterBtn.find());
  expect(ui.deleteBranchBtn.query()).not.toBeInTheDocument();
});

it('should exclude from purge', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  expect(await ui.branchTabContent.find()).toBeInTheDocument();
  expect(within(await ui.branchRow.findAt(2)).getByRole('switch')).not.toBeChecked();

  await user.click(within(ui.branchRow.getAt(2)).getByRole('switch'));
  expect(within(ui.branchRow.getAt(2)).getByRole('switch')).toBeChecked();

  expect(within(ui.branchRow.getAt(3)).getByRole('switch')).toBeChecked();
  await user.click(within(ui.branchRow.getAt(3)).getByRole('switch'));
  expect(within(ui.branchRow.getAt(3)).getByRole('switch')).not.toBeChecked();

  await user.click(ui.pullRequestTabBtn.get());
  await user.click(ui.branchTabBtn.get());
  expect(within(ui.branchRow.getAt(2)).getByRole('switch')).toBeChecked();
  expect(within(ui.branchRow.getAt(3)).getByRole('switch')).not.toBeChecked();
});

it('should show all pull requests', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  await user.click(await ui.pullRequestTabBtn.find());
  expect(await ui.pullRequestTabContent.find()).toBeInTheDocument();
  expect(ui.branchTabContent.query()).not.toBeInTheDocument();
  expect(await ui.pullRequestRow.findAll()).toHaveLength(4);
  expect(ui.pullRequestRow.getAt(1)).toHaveTextContent(
    '01 – TEST-191 update masteroverview.quality_gate_x.metric.level.OKOK1 month ago',
  );
  expect(ui.pullRequestRow.getAt(2)).toHaveTextContent(
    '02 – TEST-192 update normal-branchoverview.quality_gate_x.metric.level.ERRORERROR2 days ago',
  );
});

it('should delete pull requests', async () => {
  const user = userEvent.setup();
  renderProjectBranchesApp();
  await user.click(await ui.pullRequestTabBtn.find());
  expect(await ui.pullRequestRow.findAll()).toHaveLength(4);
  await user.click(ui.updateFirstPRBtn.get());
  await user.click(ui.deletePullRequestBtn.get());
  expect(await ui.deletePullRequestDialog.find()).toBeInTheDocument();
  expect(ui.deletePullRequestDialog.get()).toHaveTextContent('01 – TEST-191 update master');
  await user.click(ui.deletePullRequestDialog.byRole('button', { name: 'delete' }).get());
  expect(ui.pullRequestRow.getAll()).toHaveLength(3);
});

function renderProjectBranchesApp(overrides?: Partial<AppState>) {
  return renderComponent(
    <ComponentContext.Provider
      value={{
        onComponentChange: jest.fn(),
        fetchComponent: jest.fn(),
        component: mockComponent(),
      }}
    >
      <ProjectBranchesApp />
    </ComponentContext.Provider>,
    '/?id=my-project',
    { appState: mockAppState(overrides), featureList: [Feature.BranchSupport] },
  );
}
