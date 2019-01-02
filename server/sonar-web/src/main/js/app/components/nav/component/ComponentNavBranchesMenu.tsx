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
import { Link } from 'react-router';
import ComponentNavBranchesMenuItem from './ComponentNavBranchesMenuItem';
import {
  sortBranchesAsTree,
  isLongLivingBranch,
  isShortLivingBranch,
  isSameBranchLike,
  getBranchLikeKey,
  isPullRequest,
  isBranch
} from '../../../../helpers/branches';
import { scrollToElement } from '../../../../helpers/scrolling';
import { translate } from '../../../../helpers/l10n';
import { getBranchLikeUrl } from '../../../../helpers/urls';
import SearchBox from '../../../../components/controls/SearchBox';
import HelpTooltip from '../../../../components/controls/HelpTooltip';
import { DropdownOverlay } from '../../../../components/controls/Dropdown';
import { withRouter, Router } from '../../../../components/hoc/withRouter';

interface Props {
  branchLikes: T.BranchLike[];
  canAdmin?: boolean;
  component: T.Component;
  currentBranchLike: T.BranchLike;
  onClose: () => void;
  router: Pick<Router, 'push'>;
}

interface State {
  query: string;
  selected: T.BranchLike | undefined;
}

export class ComponentNavBranchesMenu extends React.PureComponent<Props, State> {
  listNode?: HTMLUListElement | null;
  selectedBranchNode?: HTMLLIElement | null;
  state: State = { query: '', selected: undefined };

  componentDidMount() {
    this.scrollToSelectedBranch(false);
  }

  componentDidUpdate() {
    this.scrollToSelectedBranch(true);
  }

  scrollToSelectedBranch(smooth: boolean) {
    if (this.listNode && this.selectedBranchNode) {
      scrollToElement(this.selectedBranchNode, {
        parent: this.listNode,
        smooth
      });
    }
  }

  getFilteredBranchLikes = () => {
    const query = this.state.query.toLowerCase();
    return sortBranchesAsTree(this.props.branchLikes).filter(branchLike => {
      const matchBranchName = isBranch(branchLike) && branchLike.name.toLowerCase().includes(query);
      const matchPullRequestTitleOrId =
        isPullRequest(branchLike) &&
        (branchLike.title.toLowerCase().includes(query) ||
          branchLike.key.toLowerCase().includes(query));
      return matchBranchName || matchPullRequestTitleOrId;
    });
  };

  handleSearchChange = (query: string) => this.setState({ query, selected: undefined });

  handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    switch (event.keyCode) {
      case 13:
        event.preventDefault();
        this.openSelected();
        return;
      case 38:
        event.preventDefault();
        this.selectPrevious();
        return;
      case 40:
        event.preventDefault();
        this.selectNext();
        // keep this return to prevent fall-through in case more cases will be adder later
        // eslint-disable-next-line no-useless-return
        return;
    }
  };

  openSelected = () => {
    const selected = this.getSelected();
    if (selected) {
      this.props.router.push(this.getProjectBranchUrl(selected));
    }
  };

  selectPrevious = () => {
    const selected = this.getSelected();
    const branchLikes = this.getFilteredBranchLikes();
    const index = branchLikes.findIndex(b => isSameBranchLike(b, selected));
    if (index > 0) {
      this.setState({ selected: branchLikes[index - 1] });
    }
  };

  selectNext = () => {
    const selected = this.getSelected();
    const branches = this.getFilteredBranchLikes();
    const index = branches.findIndex(b => isSameBranchLike(b, selected));
    if (index >= 0 && index < branches.length - 1) {
      this.setState({ selected: branches[index + 1] });
    }
  };

  handleSelect = (branchLike: T.BranchLike) => {
    this.setState({ selected: branchLike });
  };

  getSelected = () => {
    if (this.state.selected) {
      return this.state.selected;
    }

    const branchLikes = this.getFilteredBranchLikes();
    if (branchLikes.find(b => isSameBranchLike(b, this.props.currentBranchLike))) {
      return this.props.currentBranchLike;
    }

    if (branchLikes.length > 0) {
      return branchLikes[0];
    }

    return undefined;
  };

  getProjectBranchUrl = (branchLike: T.BranchLike) =>
    getBranchLikeUrl(this.props.component.key, branchLike);

  isOrphan = (branchLike: T.BranchLike) => {
    return (isShortLivingBranch(branchLike) || isPullRequest(branchLike)) && branchLike.isOrphan;
  };

  renderSearch = () => (
    <div className="menu-search">
      <SearchBox
        autoFocus={true}
        onChange={this.handleSearchChange}
        onKeyDown={this.handleKeyDown}
        placeholder={translate('branches.search_for_branches')}
        value={this.state.query}
      />
    </div>
  );

  renderBranchesList = () => {
    const branchLikes = this.getFilteredBranchLikes();
    const selected = this.getSelected();

    if (branchLikes.length === 0) {
      return <div className="menu-message note">{translate('no_results')}</div>;
    }

    const items = branchLikes.map((branchLike, index) => {
      const isOrphan = this.isOrphan(branchLike);
      const previous = index > 0 ? branchLikes[index - 1] : undefined;
      const isPreviousOrphan = previous !== undefined && this.isOrphan(previous);
      const showDivider = isLongLivingBranch(branchLike) || (isOrphan && !isPreviousOrphan);
      const showOrphanHeader = isOrphan && !isPreviousOrphan;
      const showPullRequestHeader =
        !showOrphanHeader && isPullRequest(branchLike) && !isPullRequest(previous);
      const showShortLivingBranchHeader =
        !showOrphanHeader && isShortLivingBranch(branchLike) && !isShortLivingBranch(previous);
      const isSelected = isSameBranchLike(branchLike, selected);
      return (
        <React.Fragment key={getBranchLikeKey(branchLike)}>
          {showDivider && <li className="divider" />}
          {showOrphanHeader && (
            <li className="menu-header">
              <div className="display-inline-block text-middle">
                {translate('branches.orphan_branches')}
              </div>
              <HelpTooltip
                className="spacer-left"
                overlay={translate('branches.orphan_branches.tooltip')}
              />
            </li>
          )}
          {showPullRequestHeader && (
            <li className="menu-header navbar-context-meta-branch-menu-title">
              {translate('branches.pull_requests')}
            </li>
          )}
          {showShortLivingBranchHeader && (
            <li className="menu-header navbar-context-meta-branch-menu-title">
              {translate('branches.short_lived_branches')}
            </li>
          )}
          <ComponentNavBranchesMenuItem
            branchLike={branchLike}
            component={this.props.component}
            innerRef={node =>
              (this.selectedBranchNode = isSelected ? node : this.selectedBranchNode)
            }
            key={getBranchLikeKey(branchLike)}
            onSelect={this.handleSelect}
            selected={isSelected}
          />
        </React.Fragment>
      );
    });

    return (
      <ul className="menu menu-vertically-limited" ref={node => (this.listNode = node)}>
        {items}
      </ul>
    );
  };

  render() {
    const { component } = this.props;
    const showManageLink =
      component.qualifier === 'TRK' &&
      component.configuration &&
      component.configuration.showSettings;

    return (
      <DropdownOverlay noPadding={true}>
        {this.renderSearch()}
        {this.renderBranchesList()}
        {showManageLink && (
          <div className="dropdown-bottom-hint text-right">
            <Link
              className="text-muted"
              to={{ pathname: '/project/branches', query: { id: component.key } }}>
              {translate('branches.manage')}
            </Link>
          </div>
        )}
      </DropdownOverlay>
    );
  }
}

export default withRouter(ComponentNavBranchesMenu);
