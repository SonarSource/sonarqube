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

import { IconGitBranch, IconPullrequest } from '@sonarsource/echoes-react';
import { useState } from 'react';
import { ToggleButton, getTabId, getTabPanelId } from '~design-system';
import { isBranch, isMainBranch, isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { sortBranches, sortPullRequests } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { useBranchesQuery } from '../../../queries/branch';
import { Branch, BranchLike, PullRequest } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import BranchLikeTable from './BranchLikeTable';
import DeleteBranchModal from './DeleteBranchModal';
import RenameBranchModal from './RenameBranchModal';
import SetAsMainBranchModal from './SetAsMainBranchModal';

interface Props {
  component: Component;
  fetchComponent: () => Promise<void>;
}

export enum Tabs {
  Branch,
  PullRequest,
}

const TABS = [
  {
    key: Tabs.Branch,
    value: Tabs.Branch,
    label: (
      <>
        <IconGitBranch />
        <span className="sw-ml-2">{translate('project_branch_pull_request.tabs.branches')}</span>
      </>
    ),
  },
  {
    key: Tabs.PullRequest,
    value: Tabs.PullRequest,
    label: (
      <>
        <IconPullrequest />
        <span className="sw-ml-2">
          {translate('project_branch_pull_request.tabs.pull_requests')}
        </span>
      </>
    ),
  },
];

export default function BranchLikeTabs(props: Props) {
  const { component, fetchComponent } = props;
  const [currentTab, setCurrentTab] = useState<Tabs>(Tabs.Branch);
  const [renaming, setRenaming] = useState<BranchLike>();
  const [settingAsMain, setSettingAsMain] = useState<Branch>();
  const [deleting, setDeleting] = useState<BranchLike>();

  const handleClose = () => {
    setRenaming(undefined);
    setDeleting(undefined);
    setSettingAsMain(undefined);
  };

  const handleSetAsMainBranch = () => {
    handleClose();
    fetchComponent();
  };

  const handleSetAsMainBranchOption = (branchLike: BranchLike) => {
    if (isBranch(branchLike)) {
      setSettingAsMain(branchLike);
    }
  };

  const { data: branchLikes = [] } = useBranchesQuery(component);

  const isBranchMode = currentTab === Tabs.Branch;
  const branchLikesToDisplay: BranchLike[] = isBranchMode
    ? sortBranches(branchLikes.filter(isBranch) as Branch[])
    : sortPullRequests(branchLikes.filter(isPullRequest) as PullRequest[]);
  const title = translate(
    isBranchMode
      ? 'project_branch_pull_request.table.branch'
      : 'project_branch_pull_request.table.pull_request',
  );

  return (
    <>
      <ToggleButton
        onChange={(currentTabKey: Tabs) => setCurrentTab(currentTabKey)}
        value={currentTab}
        options={TABS}
        role="tablist"
      />

      <div role="tabpanel" id={getTabPanelId(currentTab)} aria-labelledby={getTabId(currentTab)}>
        <BranchLikeTable
          branchLikes={branchLikesToDisplay}
          component={component}
          displayPurgeSetting={isBranchMode}
          onDelete={setDeleting}
          onRename={setRenaming}
          onSetAsMain={handleSetAsMainBranchOption}
          title={title}
        />
      </div>

      {deleting && (
        <DeleteBranchModal branchLike={deleting} component={component} onClose={handleClose} />
      )}

      {renaming && isMainBranch(renaming) && (
        <RenameBranchModal branch={renaming} component={component} onClose={handleClose} />
      )}

      {settingAsMain && !isMainBranch(settingAsMain) && (
        <SetAsMainBranchModal
          component={component}
          branch={settingAsMain}
          onClose={handleClose}
          onSetAsMain={handleSetAsMainBranch}
        />
      )}
    </>
  );
}
