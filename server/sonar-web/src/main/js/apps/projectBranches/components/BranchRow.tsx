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
import DeleteBranchModal from './DeleteBranchModal';
import LeakPeriodForm from './LeakPeriodForm';
import RenameBranchModal from './RenameBranchModal';
import { Branch } from '../../../app/types';
import BranchStatus from '../../../components/common/BranchStatus';
import BranchIcon from '../../../components/icons-components/BranchIcon';
import { isShortLivingBranch, isLongLivingBranch } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
import DateFromNow from '../../../components/intl/DateFromNow';
import ActionsDropdown, {
  ActionsDropdownItem,
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';

interface Props {
  branch: Branch;
  component: string;
  onChange: () => void;
}

interface State {
  changingLeak: boolean;
  deleting: boolean;
  renaming: boolean;
}

export default class BranchRow extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { changingLeak: false, deleting: false, renaming: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDeleteClick = () => {
    this.setState({ deleting: true });
  };

  handleDeletingStop = () => {
    this.setState({ deleting: false });
  };

  handleRenameClick = () => {
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

  handleChangeLeakClick = () => {
    this.setState({ changingLeak: true });
  };

  handleChangingLeakStop = () => {
    if (this.mounted) {
      this.setState({ changingLeak: false });
    }
  };

  renderActions() {
    const { branch, component } = this.props;
    return (
      <td className="thin nowrap text-right">
        <ActionsDropdown className="ig-spacer-left">
          {isLongLivingBranch(branch) && (
            <ActionsDropdownItem
              className="js-change-leak-period"
              onClick={this.handleChangeLeakClick}>
              {translate('branches.set_leak_period')}
            </ActionsDropdownItem>
          )}
          {isLongLivingBranch(branch) && !branch.isMain && <ActionsDropdownDivider />}
          {branch.isMain ? (
            <ActionsDropdownItem className="js-rename" onClick={this.handleRenameClick}>
              {translate('branches.rename')}
            </ActionsDropdownItem>
          ) : (
            <ActionsDropdownItem
              className="js-delete"
              destructive={true}
              onClick={this.handleDeleteClick}>
              {translate('branches.delete')}
            </ActionsDropdownItem>
          )}
        </ActionsDropdown>

        {this.state.deleting && (
          <DeleteBranchModal
            branch={branch}
            component={component}
            onClose={this.handleDeletingStop}
            onDelete={this.handleChange}
          />
        )}

        {this.state.renaming && (
          <RenameBranchModal
            branch={branch}
            component={component}
            onClose={this.handleRenamingStop}
            onRename={this.handleChange}
          />
        )}

        {this.state.changingLeak && (
          <LeakPeriodForm
            branch={branch.name}
            onClose={this.handleChangingLeakStop}
            project={component}
          />
        )}
      </td>
    );
  }

  render() {
    const { branch } = this.props;

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
          {branch.isMain && (
            <div className="outline-badge spacer-left">{translate('branches.main_branch')}</div>
          )}
        </td>
        <td className="thin nowrap text-right">
          <BranchStatus branch={branch} />
        </td>
        <td className="thin nowrap text-right">
          {branch.analysisDate && <DateFromNow date={branch.analysisDate} />}
        </td>
        {this.renderActions()}
      </tr>
    );
  }
}
