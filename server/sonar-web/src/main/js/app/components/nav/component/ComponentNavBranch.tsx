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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import PlusCircleIcon from 'sonar-ui-common/components/icons/PlusCircleIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../../../components/docs/DocTooltip';
import { withAppState } from '../../../../components/hoc/withAppState';
import BranchIcon from '../../../../components/icons-components/BranchIcon';
import {
  getBranchLikeDisplayName,
  isPullRequest,
  isSameBranchLike,
  isShortLivingBranch
} from '../../../../helpers/branches';
import { isSonarCloud } from '../../../../helpers/system';
import { getPortfolioAdminUrl } from '../../../../helpers/urls';
import { colors } from '../../../theme';
import ComponentNavBranchesMenu from './ComponentNavBranchesMenu';

interface Props {
  appState: Pick<T.AppState, 'branchesEnabled'>;
  branchLikes: T.BranchLike[];
  component: T.Component;
  currentBranchLike: T.BranchLike;
  location?: any;
}

interface State {
  dropdownOpen: boolean;
}

export class ComponentNavBranch extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { dropdownOpen: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.component !== this.props.component ||
      !isSameBranchLike(nextProps.currentBranchLike, this.props.currentBranchLike) ||
      nextProps.location !== this.props.location
    ) {
      this.setState({ dropdownOpen: false });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
    this.setState(state => ({ dropdownOpen: !state.dropdownOpen }));
  };

  closeDropdown = () => {
    if (this.mounted) {
      this.setState({ dropdownOpen: false });
    }
  };

  renderMergeBranch = () => {
    const { currentBranchLike } = this.props;
    if (isShortLivingBranch(currentBranchLike)) {
      return currentBranchLike.isOrphan ? (
        <span className="note big-spacer-left text-ellipsis flex-shrink">
          <span className="text-middle">{translate('branches.orphan_branch')}</span>
          <HelpTooltip
            className="spacer-left"
            overlay={translate('branches.orphan_branches.tooltip')}
          />
        </span>
      ) : (
        <span className="note big-spacer-left">
          {translate('from')} <strong>{currentBranchLike.mergeBranch}</strong>
        </span>
      );
    } else if (isPullRequest(currentBranchLike)) {
      return (
        <span className="note big-spacer-left text-ellipsis flex-shrink">
          <FormattedMessage
            defaultMessage={translate('branches.pull_request.for_merge_into_x_from_y')}
            id="branches.pull_request.for_merge_into_x_from_y"
            values={{
              target: <strong>{currentBranchLike.target}</strong>,
              branch: <strong>{currentBranchLike.branch}</strong>
            }}
          />
        </span>
      );
    } else {
      return null;
    }
  };

  renderOverlay = () => {
    return (
      <>
        <p>{translate('application.branches.help')}</p>
        <hr className="spacer-top spacer-bottom" />
        <Link
          className="spacer-left link-no-underline"
          to={getPortfolioAdminUrl(this.props.component.breadcrumbs[0].key, 'APP')}>
          {translate('application.branches.link')}
        </Link>
      </>
    );
  };

  render() {
    const { branchLikes, currentBranchLike } = this.props;
    const { configuration, breadcrumbs } = this.props.component;

    if (isSonarCloud() && !this.props.appState.branchesEnabled) {
      return null;
    }

    const displayName = getBranchLikeDisplayName(currentBranchLike);
    const isApp = breadcrumbs && breadcrumbs[0] && breadcrumbs[0].qualifier === 'APP';

    if (isApp && branchLikes.length < 2) {
      return (
        <div className="navbar-context-branches">
          <BranchIcon
            branchLike={currentBranchLike}
            className="little-spacer-right"
            fill={colors.gray80}
          />
          <span className="note">{displayName}</span>
          {configuration && configuration.showSettings && (
            <HelpTooltip className="spacer-left" overlay={this.renderOverlay()}>
              <PlusCircleIcon className="text-middle" fill={colors.blue} size={12} />
            </HelpTooltip>
          )}
        </div>
      );
    } else {
      if (!this.props.appState.branchesEnabled) {
        return (
          <div className="navbar-context-branches">
            <BranchIcon
              branchLike={currentBranchLike}
              className="little-spacer-right"
              fill={colors.gray80}
            />
            <span className="note">{displayName}</span>
            <DocTooltip
              className="spacer-left"
              doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/branches/no-branch-support.md')}>
              <PlusCircleIcon fill={colors.gray71} size={12} />
            </DocTooltip>
          </div>
        );
      }

      if (branchLikes.length < 2) {
        return (
          <div className="navbar-context-branches">
            <BranchIcon branchLike={currentBranchLike} className="little-spacer-right" />
            <span className="note">{displayName}</span>
            <DocTooltip
              className="spacer-left"
              doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/branches/single-branch.md')}>
              <PlusCircleIcon fill={colors.blue} size={12} />
            </DocTooltip>
          </div>
        );
      }
    }

    return (
      <div className="navbar-context-branches">
        <div className="dropdown">
          <Toggler
            onRequestClose={this.closeDropdown}
            open={this.state.dropdownOpen}
            overlay={
              <ComponentNavBranchesMenu
                branchLikes={this.props.branchLikes}
                canAdmin={configuration && configuration.showSettings}
                component={this.props.component}
                currentBranchLike={this.props.currentBranchLike}
                onClose={this.closeDropdown}
              />
            }>
            <a
              className="link-base-color link-no-underline nowrap"
              href="#"
              onClick={this.handleClick}>
              <BranchIcon branchLike={currentBranchLike} className="little-spacer-right" />
              <span className="text-limited text-top" title={displayName}>
                {displayName}
              </span>
              <DropdownIcon className="little-spacer-left" />
            </a>
          </Toggler>
        </div>
        {this.renderMergeBranch()}
      </div>
    );
  }
}

export default withAppState(ComponentNavBranch);
