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
import ComponentNavBranchesMenu from './ComponentNavBranchesMenu';
import { Branch, Component } from '../../../types';
import BranchIcon from '../../../../components/icons-components/BranchIcon';
import { isShortLivingBranch } from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';

interface Props {
  branch: Branch;
  project: Component;
}

interface State {
  open: boolean;
}

export default class ComponentNavBranch extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { open: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.project !== this.props.project || nextProps.branch !== this.props.branch) {
      this.setState({ open: false });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
    this.setState({ open: true });
  };

  closeDropdown = () => {
    if (this.mounted) {
      this.setState({ open: false });
    }
  };

  render() {
    const { branch } = this.props;

    return (
      <div className={classNames('navbar-context-branches', 'dropdown', { open: this.state.open })}>
        <a className="link-base-color link-no-underline" href="#" onClick={this.handleClick}>
          <BranchIcon branch={branch} className="little-spacer-right" />
          {branch.name}
          <i className="icon-dropdown little-spacer-left" />
        </a>
        {this.state.open &&
          <ComponentNavBranchesMenu
            branch={branch}
            onClose={this.closeDropdown}
            project={this.props.project}
          />}
        {isShortLivingBranch(branch) &&
          <span className="note big-spacer-left text-lowercase">
            {translate('from')} <strong>{branch.mergeBranch}</strong>
          </span>}
      </div>
    );
  }
}
