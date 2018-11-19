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
import { Branch } from '../../../app/types';
import * as classNames from 'classnames';
import DeleteBranchModal from './DeleteBranchModal';
import LeakPeriodForm from './LeakPeriodForm';
import BranchStatus from '../../../components/common/BranchStatus';
import BranchIcon from '../../../components/icons-components/BranchIcon';
import { isShortLivingBranch, isLongLivingBranch } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
import RenameBranchModal from './RenameBranchModal';
import DateFromNow from '../../../components/intl/DateFromNow';
import SettingsIcon from '../../../components/icons-components/SettingsIcon';

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
  mounted: boolean;
  state: State = { changingLeak: false, deleting: false, renaming: false };

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

  handleChangeLeakClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ changingLeak: true });
  };

  handleChangingLeakStop = () => {
    if (this.mounted) {
      this.setState({ changingLeak: false });
    }
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
        <td className="thin nowrap text-right">
          <div className="dropdown big-spacer-left">
            <button
              className="dropdown-toggle little-spacer-right button-compact"
              data-toggle="dropdown">
              <SettingsIcon style={{ marginTop: 4 }} /> <i className="icon-dropdown" />
            </button>
            <ul className="dropdown-menu dropdown-menu-right">
              {isLongLivingBranch(branch) && (
                <li>
                  <a
                    className="js-change-leak-period link-no-underline"
                    href="#"
                    onClick={this.handleChangeLeakClick}>
                    {translate('branches.set_leak_period')}
                  </a>
                </li>
              )}
              {branch.isMain ? (
                <li>
                  <a
                    className="js-rename link-no-underline"
                    href="#"
                    onClick={this.handleRenameClick}>
                    {translate('branches.rename')}
                  </a>
                </li>
              ) : (
                <li>
                  <a
                    className="js-delete link-no-underline"
                    href="#"
                    onClick={this.handleDeleteClick}>
                    {translate('branches.delete')}
                  </a>
                </li>
              )}
            </ul>
          </div>
        </td>

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
      </tr>
    );
  }
}
