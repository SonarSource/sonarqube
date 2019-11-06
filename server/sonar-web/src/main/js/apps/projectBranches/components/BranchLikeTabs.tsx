/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import BoxedTabs from 'sonar-ui-common/components/controls/BoxedTabs';
import PullRequestIcon from 'sonar-ui-common/components/icons/PullRequestIcon';
import ShortLivingBranchIcon from 'sonar-ui-common/components/icons/ShortLivingBranchIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isBranch, isMainBranch, isPullRequest, sortBranches } from '../../../helpers/branches';
import BranchLikeTableRenderer from './BranchLikeTableRenderer';
import DeleteBranchModal from './DeleteBranchModal';
import RenameBranchModal from './RenameBranchModal';

interface Props {
  branchLikes: T.BranchLike[];
  component: T.Component;
  onBranchesChange: () => void;
}

interface State {
  currentTab: Tabs;
  deleting?: T.BranchLike;
  renaming?: T.BranchLike;
}

export enum Tabs {
  Branch,
  PullRequest
}

const TABS = [
  {
    key: Tabs.Branch,
    label: (
      <>
        <ShortLivingBranchIcon />
        <span className="spacer-left">
          {translate('project_branch_pull_request.tabs.branches')}
        </span>
      </>
    )
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
    )
  }
];

export default class BranchLikeTabs extends React.PureComponent<Props, State> {
  state: State = { currentTab: Tabs.Branch };

  onTabSelect = (currentTab: Tabs) => {
    this.setState({ currentTab });
  };

  onDeleteBranchLike = (branchLike: T.BranchLike) => this.setState({ deleting: branchLike });

  onRenameBranchLike = (branchLike: T.BranchLike) => this.setState({ renaming: branchLike });

  onClose = () => this.setState({ deleting: undefined, renaming: undefined });

  onModalActionFulfilled = () => {
    this.onClose();
    this.props.onBranchesChange();
  };

  render() {
    const { branchLikes, component } = this.props;
    const { currentTab, deleting, renaming } = this.state;

    let tableTitle = '';
    let branchLikesToDisplay: T.BranchLike[] = [];

    if (currentTab === Tabs.Branch) {
      tableTitle = translate('project_branch_pull_request.table.branch');
      branchLikesToDisplay = sortBranches(branchLikes.filter(isBranch));
    } else if (currentTab === Tabs.PullRequest) {
      tableTitle = translate('project_branch_pull_request.table.pull_request');
      branchLikesToDisplay = branchLikes.filter(isPullRequest);
    }

    return (
      <>
        <BoxedTabs onSelect={this.onTabSelect} selected={currentTab} tabs={TABS} />

        <BranchLikeTableRenderer
          branchLikes={branchLikesToDisplay}
          component={component}
          onDelete={this.onDeleteBranchLike}
          onRename={this.onRenameBranchLike}
          tableTitle={tableTitle}
        />

        {deleting && (
          <DeleteBranchModal
            branchLike={deleting}
            component={component}
            onClose={this.onClose}
            onDelete={this.onModalActionFulfilled}
          />
        )}

        {renaming && isMainBranch(renaming) && (
          <RenameBranchModal
            branch={renaming}
            component={component}
            onClose={this.onClose}
            onRename={this.onModalActionFulfilled}
          />
        )}
      </>
    );
  }
}
