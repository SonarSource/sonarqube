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
import * as React from 'react';
import BoxedTabs, { getTabId, getTabPanelId } from '../../../components/controls/BoxedTabs';
import BranchIcon from '../../../components/icons/BranchIcon';
import PullRequestIcon from '../../../components/icons/PullRequestIcon';
import {
  isBranch,
  isMainBranch,
  isPullRequest,
  sortBranches,
  sortPullRequests,
} from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import BranchLikeTable from './BranchLikeTable';
import DeleteBranchModal from './DeleteBranchModal';
import RenameBranchModal from './RenameBranchModal';

interface Props {
  branchLikes: BranchLike[];
  component: Component;
  onBranchesChange: () => void;
}

interface State {
  currentTab: Tabs;
  deleting?: BranchLike;
  renaming?: BranchLike;
}

export enum Tabs {
  Branch,
  PullRequest,
}

const TABS = [
  {
    key: Tabs.Branch,
    label: (
      <>
        <BranchIcon />
        <span className="spacer-left">
          {translate('project_branch_pull_request.tabs.branches')}
        </span>
      </>
    ),
  },
  {
    key: Tabs.PullRequest,
    label: (
      <>
        <PullRequestIcon />
        <span className="spacer-left">
          {translate('project_branch_pull_request.tabs.pull_requests')}
        </span>
      </>
    ),
  },
];

export default class BranchLikeTabs extends React.PureComponent<Props, State> {
  state: State = { currentTab: Tabs.Branch };

  handleTabSelect = (currentTab: Tabs) => {
    this.setState({ currentTab });
  };

  handleDeleteBranchLike = (branchLike: BranchLike) => {
    this.setState({ deleting: branchLike });
  };

  handleRenameBranchLike = (branchLike: BranchLike) => {
    this.setState({ renaming: branchLike });
  };

  handleUpdatePurgeSetting = () => {
    this.props.onBranchesChange();
  };

  handleClose = () => {
    this.setState({ deleting: undefined, renaming: undefined });
  };

  handleModalActionFulfilled = () => {
    this.handleClose();
    this.props.onBranchesChange();
  };

  render() {
    const { branchLikes, component } = this.props;
    const { currentTab, deleting, renaming } = this.state;

    const isBranchMode = currentTab === Tabs.Branch;
    const branchLikesToDisplay: BranchLike[] = isBranchMode
      ? sortBranches(branchLikes.filter(isBranch))
      : sortPullRequests(branchLikes.filter(isPullRequest));
    const title = translate(
      isBranchMode
        ? 'project_branch_pull_request.table.branch'
        : 'project_branch_pull_request.table.pull_request'
    );

    return (
      <>
        <BoxedTabs
          className="branch-like-tabs"
          onSelect={this.handleTabSelect}
          selected={currentTab}
          tabs={TABS}
        />

        <div role="tabpanel" id={getTabPanelId(currentTab)} aria-labelledby={getTabId(currentTab)}>
          <BranchLikeTable
            branchLikes={branchLikesToDisplay}
            component={component}
            displayPurgeSetting={isBranchMode}
            onDelete={this.handleDeleteBranchLike}
            onRename={this.handleRenameBranchLike}
            onUpdatePurgeSetting={this.handleUpdatePurgeSetting}
            title={title}
          />
        </div>

        {deleting && (
          <DeleteBranchModal
            branchLike={deleting}
            component={component}
            onClose={this.handleClose}
            onDelete={this.handleModalActionFulfilled}
          />
        )}

        {renaming && isMainBranch(renaming) && (
          <RenameBranchModal
            branch={renaming}
            component={component}
            onClose={this.handleClose}
            onRename={this.handleModalActionFulfilled}
          />
        )}
      </>
    );
  }
}
