/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as classNames from 'classnames';
import * as PropTypes from 'prop-types';
import { FormattedMessage } from 'react-intl';
import ComponentNavBranchesMenu from './ComponentNavBranchesMenu';
import SingleBranchHelperPopup from './SingleBranchHelperPopup';
import NoBranchSupportPopup from './NoBranchSupportPopup';
import { BranchLike, Component } from '../../../types';
import * as theme from '../../../theme';
import BranchIcon from '../../../../components/icons-components/BranchIcon';
import {
  isShortLivingBranch,
  isSameBranchLike,
  getBranchLikeDisplayName,
  isPullRequest
} from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';
import HelpIcon from '../../../../components/icons-components/HelpIcon';
import BubblePopupHelper from '../../../../components/common/BubblePopupHelper';
import Tooltip from '../../../../components/controls/Tooltip';

interface Props {
  branchLikes: BranchLike[];
  component: Component;
  currentBranchLike: BranchLike;
  location?: any;
}

interface State {
  dropdownOpen: boolean;
  noBranchSupportPopupOpen: boolean;
  singleBranchPopupOpen: boolean;
}

export default class ComponentNavBranch extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    branchesEnabled: PropTypes.bool.isRequired,
    onSonarCloud: PropTypes.bool
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      dropdownOpen: false,
      noBranchSupportPopupOpen: false,
      singleBranchPopupOpen: false
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.component !== this.props.component ||
      !isSameBranchLike(nextProps.currentBranchLike, this.props.currentBranchLike) ||
      nextProps.location !== this.props.location
    ) {
      this.setState({ dropdownOpen: false, singleBranchPopupOpen: false });
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

  toggleSingleBranchPopup = (show?: boolean) => {
    if (show !== undefined) {
      this.setState({ singleBranchPopupOpen: show });
    } else {
      this.setState(state => ({ singleBranchPopupOpen: !state.singleBranchPopupOpen }));
    }
  };

  toggleNoBranchSupportPopup = (show?: boolean) => {
    if (show !== undefined) {
      this.setState({ noBranchSupportPopupOpen: show });
    } else {
      this.setState(state => ({ noBranchSupportPopupOpen: !state.noBranchSupportPopupOpen }));
    }
  };

  handleSingleBranchClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.toggleSingleBranchPopup();
  };

  handleNoBranchSupportClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.toggleNoBranchSupportPopup();
  };

  renderDropdown = () => {
    const { configuration } = this.props.component;
    return this.state.dropdownOpen ? (
      <ComponentNavBranchesMenu
        branchLikes={this.props.branchLikes}
        canAdmin={configuration && configuration.showSettings}
        component={this.props.component}
        currentBranchLike={this.props.currentBranchLike}
        onClose={this.closeDropdown}
      />
    ) : null;
  };

  renderMergeBranch = () => {
    const { currentBranchLike } = this.props;
    if (isShortLivingBranch(currentBranchLike)) {
      return currentBranchLike.isOrphan ? (
        <span className="note big-spacer-left text-lowercase">
          {translate('branches.orphan_branch')}
          <Tooltip overlay={translate('branches.orphan_branches.tooltip')}>
            <i className="icon-help spacer-left" />
          </Tooltip>
        </span>
      ) : (
        <span className="note big-spacer-left text-lowercase">
          {translate('from')} <strong>{currentBranchLike.mergeBranch}</strong>
        </span>
      );
    } else if (isPullRequest(currentBranchLike)) {
      return (
        <span className="note big-spacer-left text-lowercase">
          <FormattedMessage
            defaultMessage={translate('branches.pull_request.for_merge_into_x_from_y')}
            id="branches.pull_request.for_merge_into_x_from_y"
            values={{
              base: <strong>{currentBranchLike.base}</strong>,
              branch: <strong>{currentBranchLike.branch}</strong>
            }}
          />
        </span>
      );
    } else {
      return null;
    }
  };

  renderSingleBranchPopup = () => (
    <div className="display-inline-block spacer-left">
      <a className="link-no-underline" href="#" onClick={this.handleSingleBranchClick}>
        <HelpIcon fill={theme.blue} />
      </a>
      <BubblePopupHelper
        isOpen={this.state.singleBranchPopupOpen}
        position="bottomleft"
        popup={<SingleBranchHelperPopup />}
        togglePopup={this.toggleSingleBranchPopup}
      />
    </div>
  );

  renderNoBranchSupportPopup = () => (
    <div className="display-inline-block spacer-left">
      <a className="link-no-underline" href="#" onClick={this.handleNoBranchSupportClick}>
        <HelpIcon fill={theme.gray80} />
      </a>
      <BubblePopupHelper
        isOpen={this.state.noBranchSupportPopupOpen}
        position="bottomleft"
        popup={<NoBranchSupportPopup />}
        togglePopup={this.toggleNoBranchSupportPopup}
      />
    </div>
  );

  render() {
    const { branchLikes, currentBranchLike } = this.props;

    if (this.context.onSonarCloud && !this.context.branchesEnabled) {
      return null;
    }

    const displayName = getBranchLikeDisplayName(currentBranchLike);

    if (!this.context.branchesEnabled) {
      return (
        <div className="navbar-context-branches">
          <BranchIcon
            branchLike={currentBranchLike}
            className="little-spacer-right"
            fill={theme.gray80}
          />
          <span className="note">{displayName}</span>
          {this.renderNoBranchSupportPopup()}
        </div>
      );
    }

    if (branchLikes.length < 2) {
      return (
        <div className="navbar-context-branches">
          <BranchIcon branchLike={currentBranchLike} className="little-spacer-right" />
          <span className="note">{displayName}</span>
          {this.renderSingleBranchPopup()}
        </div>
      );
    }

    return (
      <div
        className={classNames('navbar-context-branches', 'dropdown', {
          open: this.state.dropdownOpen
        })}>
        <a className="link-base-color link-no-underline" href="#" onClick={this.handleClick}>
          <BranchIcon branchLike={currentBranchLike} className="little-spacer-right" />
          <Tooltip overlay={displayName} mouseEnterDelay={1}>
            <span className="text-limited text-top">{displayName}</span>
          </Tooltip>
          <i className="icon-dropdown little-spacer-left" />
        </a>
        {this.renderDropdown()}
        {this.renderMergeBranch()}
      </div>
    );
  }
}
