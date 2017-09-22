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
import { Link } from 'react-router';
import * as classNames from 'classnames';
import DeleteBranchModal from './DeleteBranchModal';
import RenameBranchModal from './RenameBranchModal';
import BranchStatus from '../../../../components/common/BranchStatus';
import { Branch, Component } from '../../../types';
import BranchIcon from '../../../../components/icons-components/BranchIcon';
import ChangeIcon from '../../../../components/icons-components/ChangeIcon';
import DeleteIcon from '../../../../components/icons-components/DeleteIcon';
import { isShortLivingBranch } from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';
import { getProjectBranchUrl } from '../../../../helpers/urls';

export interface Props {
  branch: Branch;
  canAdmin?: boolean;
  component: Component;
  onBranchesChange: () => void;
  onSelect: (branch: Branch) => void;
  selected: boolean;
}

interface State {
  deleteBranchModal: boolean;
  renameBranchModal: boolean;
}

export default class ComponentNavBranchesMenuItem extends React.PureComponent<Props, State> {
  state: State = { deleteBranchModal: false, renameBranchModal: false };

  handleMouseEnter = () => {
    this.props.onSelect(this.props.branch);
  };

  handleDeleteBranchClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ deleteBranchModal: true });
  };

  handleDeleteBranchClose = () => {
    this.setState({ deleteBranchModal: false });
  };

  handleBranchDelete = () => {
    this.props.onBranchesChange();
    this.setState({ deleteBranchModal: false });
  };

  handleRenameBranchClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ renameBranchModal: true });
  };

  handleRenameBranchClose = () => {
    this.setState({ renameBranchModal: false });
  };

  handleBranchRename = () => {
    this.props.onBranchesChange();
    this.setState({ renameBranchModal: false });
  };

  render() {
    const { branch } = this.props;
    return (
      <li key={branch.name} onMouseEnter={this.handleMouseEnter}>
        <Link
          className={classNames('navbar-context-meta-branch-menu-item', {
            active: this.props.selected
          })}
          to={getProjectBranchUrl(this.props.component.key, branch)}>
          <div className="navbar-context-meta-branch-menu-item-name">
            <BranchIcon
              branch={branch}
              className={classNames('little-spacer-right', {
                'big-spacer-left': isShortLivingBranch(branch) && !branch.isOrphan
              })}
            />
            {branch.name}
            {branch.isMain && (
              <div className="outline-badge spacer-left">{translate('branches.main_branch')}</div>
            )}
          </div>
          <div className="big-spacer-left note">
            <BranchStatus branch={branch} concise={true} />
          </div>
          {this.props.canAdmin && (
            <div className="navbar-context-meta-branch-menu-item-actions">
              {branch.isMain ? (
                <button className="js-rename button-link" onClick={this.handleRenameBranchClick}>
                  <ChangeIcon />
                </button>
              ) : (
                <button className="js-delete button-link" onClick={this.handleDeleteBranchClick}>
                  <DeleteIcon />
                </button>
              )}
            </div>
          )}
        </Link>

        {this.state.deleteBranchModal && (
          <DeleteBranchModal
            branch={branch}
            component={this.props.component.key}
            onClose={this.handleDeleteBranchClose}
            onDelete={this.handleBranchDelete}
          />
        )}

        {this.state.renameBranchModal && (
          <RenameBranchModal
            branch={branch}
            component={this.props.component.key}
            onClose={this.handleRenameBranchClose}
            onRename={this.handleBranchRename}
          />
        )}
      </li>
    );
  }
}
