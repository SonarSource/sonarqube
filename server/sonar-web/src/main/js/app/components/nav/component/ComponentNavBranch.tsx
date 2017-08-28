/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import ComponentNavBranchesMenu from './ComponentNavBranchesMenu';
import SingleBranchHelperPopup from './SingleBranchHelperPopup';
import NoBranchSupportPopup from './NoBranchSupportPopup';
import { Branch, Component } from '../../../types';
import BranchIcon from '../../../../components/icons-components/BranchIcon';
import { isShortLivingBranch } from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';
import HelpIcon from '../../../../components/icons-components/HelpIcon';
import BubblePopupHelper from '../../../../components/common/BubblePopupHelper';

interface Props {
  branches: Branch[];
  currentBranch: Branch;
  project: Component;
}

interface State {
  dropdownOpen: boolean;
  noBranchSupportPopupOpen: boolean;
  singleBranchPopupOpen: boolean;
}

export default class ComponentNavBranch extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = {
    dropdownOpen: false,
    noBranchSupportPopupOpen: false,
    singleBranchPopupOpen: false
  };

  static contextTypes = {
    branchesEnabled: PropTypes.bool.isRequired
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.project !== this.props.project ||
      nextProps.currentBranch !== this.props.currentBranch
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
    this.setState({ dropdownOpen: true });
  };

  closeDropdown = () => {
    if (this.mounted) {
      this.setState({ dropdownOpen: false });
    }
  };

  toggleSingleBranchPopup = (show?: boolean) => {
    if (show != undefined) {
      this.setState({ singleBranchPopupOpen: show });
    } else {
      this.setState(state => ({ singleBranchPopupOpen: !state.singleBranchPopupOpen }));
    }
  };

  toggleNoBranchSupportPopup = (show?: boolean) => {
    if (show != undefined) {
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
    return this.state.dropdownOpen
      ? <ComponentNavBranchesMenu
          branches={this.props.branches}
          currentBranch={this.props.currentBranch}
          onClose={this.closeDropdown}
          project={this.props.project}
        />
      : null;
  };

  renderMergeBranch = () => {
    const { currentBranch } = this.props;
    return isShortLivingBranch(currentBranch) && !currentBranch.isOrphan
      ? <span className="note big-spacer-left text-lowercase">
          {translate('from')} <strong>{currentBranch.mergeBranch}</strong>
        </span>
      : null;
  };

  renderSingleBranchPopup = () =>
    <div className="display-inline-block spacer-left">
      <a className="link-no-underline" href="#" onClick={this.handleSingleBranchClick}>
        <HelpIcon className="" fill="#cdcdcd" />
      </a>
      <BubblePopupHelper
        isOpen={this.state.singleBranchPopupOpen}
        position="bottomleft"
        popup={<SingleBranchHelperPopup />}
        togglePopup={this.toggleSingleBranchPopup}
      />
    </div>;

  renderNoBranchSupportPopup = () =>
    <div className="display-inline-block spacer-left">
      <a className="link-no-underline" href="#" onClick={this.handleNoBranchSupportClick}>
        <HelpIcon className="" fill="#cdcdcd" />
      </a>
      <BubblePopupHelper
        isOpen={this.state.noBranchSupportPopupOpen}
        position="bottomleft"
        popup={<NoBranchSupportPopup />}
        togglePopup={this.toggleNoBranchSupportPopup}
      />
    </div>;

  render() {
    const { branches, currentBranch } = this.props;

    if (!this.context.branchesEnabled) {
      return (
        <div className="navbar-context-branches">
          <BranchIcon branch={currentBranch} className="little-spacer-right" color="#cdcdcd" />
          <span className="note">
            {currentBranch.name}
          </span>
          {this.renderNoBranchSupportPopup()}
        </div>
      );
    }

    if (branches.length < 2) {
      return (
        <div className="navbar-context-branches">
          <BranchIcon branch={currentBranch} className="little-spacer-right" color="#cdcdcd" />
          <span className="note">
            {currentBranch.name}
          </span>
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
          <BranchIcon branch={currentBranch} className="little-spacer-right" />
          {currentBranch.name}
          <i className="icon-dropdown little-spacer-left" />
        </a>
        {this.renderDropdown()}
        {this.renderMergeBranch()}
      </div>
    );
  }
}
