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
import Link from '../../../../../components/common/Link';
import { DropdownOverlay } from '../../../../../components/controls/Dropdown';
import SearchBox from '../../../../../components/controls/SearchBox';
import { Router, withRouter } from '../../../../../components/hoc/withRouter';
import {
  getBrancheLikesAsTree,
  isBranch,
  isPullRequest,
  isSameBranchLike,
} from '../../../../../helpers/branch-like';
import { KeyboardKeys } from '../../../../../helpers/keycodes';
import { translate } from '../../../../../helpers/l10n';
import { getBranchLikeUrl, queryToSearch } from '../../../../../helpers/urls';
import { BranchLike, BranchLikeTree } from '../../../../../types/branch-like';
import { ComponentQualifier } from '../../../../../types/component';
import { Component } from '../../../../../types/types';
import MenuItemList from './MenuItemList';

interface Props {
  branchLikes: BranchLike[];
  canAdminComponent?: boolean;
  component: Component;
  currentBranchLike: BranchLike;
  onClose: () => void;
  comparisonBranchesEnabled: boolean;
  router: Router;
}

interface State {
  branchLikesToDisplay: BranchLike[];
  branchLikesToDisplayTree: BranchLikeTree;
  query: string;
  selectedBranchLike: BranchLike | undefined;
}

export class Menu extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);

    let selectedBranchLike = undefined;

    if (props.branchLikes.some((b) => isSameBranchLike(b, props.currentBranchLike))) {
      selectedBranchLike = props.currentBranchLike;
    } else if (props.branchLikes.length > 0) {
      selectedBranchLike = props.branchLikes[0];
    }

    this.state = {
      query: '',
      selectedBranchLike,
      ...this.processBranchLikes(props.branchLikes),
    };
  }

  processBranchLikes = (branchLikes: BranchLike[]) => {
    const tree = getBrancheLikesAsTree(branchLikes);
    return {
      branchLikesToDisplay: [
        ...(tree.mainBranchTree
          ? [tree.mainBranchTree.branch, ...tree.mainBranchTree.pullRequests]
          : []),
        ...tree.branchTree.reduce((prev, t) => [...prev, t.branch, ...t.pullRequests], []),
        ...tree.parentlessPullRequests,
        ...tree.orphanPullRequests,
      ],
      branchLikesToDisplayTree: tree,
    };
  };

  openHighlightedBranchLike = () => {
    if (this.state.selectedBranchLike) {
      this.handleOnSelect(this.state.selectedBranchLike);
    }
  };

  highlightSiblingBranchlike = (indexDelta: number) => {
    const selectBranchLikeIndex = this.state.branchLikesToDisplay.findIndex((b) =>
      isSameBranchLike(b, this.state.selectedBranchLike)
    );
    const newIndex = selectBranchLikeIndex + indexDelta;

    if (
      selectBranchLikeIndex !== -1 &&
      newIndex >= 0 &&
      newIndex < this.state.branchLikesToDisplay.length
    ) {
      this.setState(({ branchLikesToDisplay }) => ({
        selectedBranchLike: branchLikesToDisplay[newIndex],
      }));
    }
  };

  handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.nativeEvent.key) {
      case KeyboardKeys.Enter:
        event.preventDefault();
        this.openHighlightedBranchLike();
        break;
      case KeyboardKeys.UpArrow:
        event.preventDefault();
        this.highlightSiblingBranchlike(-1);
        break;
      case KeyboardKeys.DownArrow:
        event.preventDefault();
        this.highlightSiblingBranchlike(+1);
        break;
    }
  };

  handleSearchChange = (query: string) => {
    const q = query.toLowerCase();

    const filterBranch = (branch: BranchLike) =>
      isBranch(branch) && branch.name.toLowerCase().includes(q);
    const filterPullRequest = (pr: BranchLike) =>
      isPullRequest(pr) && (pr.title.toLowerCase().includes(q) || pr.key.toLowerCase().includes(q));

    const filteredBranchLikes = this.props.branchLikes.filter(
      (bl) => filterBranch(bl) || filterPullRequest(bl)
    );

    this.setState({
      query: q,
      selectedBranchLike: filteredBranchLikes.length > 0 ? filteredBranchLikes[0] : undefined,
      ...this.processBranchLikes(filteredBranchLikes),
    });
  };

  handleOnSelect = (branchLike: BranchLike) => {
    this.setState({ selectedBranchLike: branchLike }, () => {
      this.props.onClose();
      this.props.router.push(getBranchLikeUrl(this.props.component.key, branchLike));
    });
  };

  render() {
    const { canAdminComponent, component, onClose } = this.props;
    const { branchLikesToDisplay, branchLikesToDisplayTree, query, selectedBranchLike } =
      this.state;

    const showManageLink = component.qualifier === ComponentQualifier.Project && canAdminComponent;
    const hasResults = branchLikesToDisplay.length > 0;

    return (
      <DropdownOverlay className="branch-like-navigation-menu" noPadding={true}>
        <div className="search-box-container">
          <SearchBox
            autoFocus={true}
            onChange={this.handleSearchChange}
            onKeyDown={this.handleKeyDown}
            placeholder={translate('branch_like_navigation.search_for_branch_like')}
            value={query}
          />
        </div>

        <div className="item-list-container">
          <MenuItemList
            branchLikeTree={branchLikesToDisplayTree}
            component={component}
            hasResults={hasResults}
            onSelect={this.handleOnSelect}
            selectedBranchLike={selectedBranchLike}
            comparisonBranchesEnabled={this.props.comparisonBranchesEnabled}
          />
        </div>

        {showManageLink && (
          <div className="hint-container text-right">
            <Link
              onClick={() => onClose()}
              to={{ pathname: '/project/branches', search: queryToSearch({ id: component.key }) }}
            >
              {
                this.props.comparisonBranchesEnabled
                    ? translate('branch_like_navigation.manage.sf')
                    : translate('branch_like_navigation.manage')
              }
            </Link>
          </div>
        )}
      </DropdownOverlay>
    );
  }
}

export default withRouter(Menu);
