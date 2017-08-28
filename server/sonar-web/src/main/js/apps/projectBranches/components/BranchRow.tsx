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
import { Branch } from '../../../app/types';
import * as classNames from 'classnames';
import DeleteBranchModal from './DeleteBranchModal';
import BranchStatus from '../../../components/common/BranchStatus';
import BranchIcon from '../../../components/icons-components/BranchIcon';
import { isShortLivingBranch } from '../../../helpers/branches';
import ChangeIcon from '../../../components/icons-components/ChangeIcon';
import DeleteIcon from '../../../components/icons-components/DeleteIcon';
import { translate } from '../../../helpers/l10n';
import Tooltip from '../../../components/controls/Tooltip';
import RenameBranchModal from './RenameBranchModal';

interface Props {
  branch: Branch;
  component: string;
  onChange: () => void;
}

interface State {
  deleting: boolean;
  renaming: boolean;
}

export default class BranchRow extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { deleting: false, renaming: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDeleteClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ deleting: true });
  };

  handleDeletingStop = () => {
    this.setState({ deleting: false });
  };

  handleRenameClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ renaming: true });
  };

  handleChange = () => {
    if (this.mounted) {
      this.setState({ deleting: false, renaming: false });
      this.props.onChange();
    }
  };

  handleRenamingStop = () => {
    this.setState({ renaming: false });
  };

  render() {
    const { branch, component } = this.props;

    return (
      <tr>
        <td>
          <BranchIcon
            branch={branch}
            className={classNames('little-spacer-right', {
              'big-spacer-left': isShortLivingBranch(branch) && !branch.isOrphan
            })}
          />
          {branch.name}
        </td>
        <td className="thin nowrap text-right">
          <BranchStatus branch={branch} />
        </td>
        <td className="thin nowrap text-right">
          {branch.isMain
            ? <Tooltip overlay={translate('branches.rename')}>
                <a
                  className="js-rename link-no-underline"
                  href="#"
                  onClick={this.handleRenameClick}>
                  <ChangeIcon />
                </a>
              </Tooltip>
            : <Tooltip overlay={translate('branches.delete')}>
                <a
                  className="js-delete link-no-underline"
                  href="#"
                  onClick={this.handleDeleteClick}>
                  <DeleteIcon />
                </a>
              </Tooltip>}
        </td>

        {this.state.deleting &&
          <DeleteBranchModal
            branch={branch}
            component={component}
            onClose={this.handleDeletingStop}
            onDelete={this.handleChange}
          />}

        {this.state.renaming &&
          <RenameBranchModal
            branch={branch}
            component={component}
            onClose={this.handleRenamingStop}
            onRename={this.handleChange}
          />}
      </tr>
    );
  }
}
