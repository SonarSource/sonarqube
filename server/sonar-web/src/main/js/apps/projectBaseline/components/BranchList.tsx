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
import ActionsDropdown, {
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { listBranchesNewCodePeriod, resetNewCodePeriod } from '../../../api/newCodePeriod';
import BranchIcon from '../../../components/icons-components/BranchIcon';
import { isLongLivingBranch, isMainBranch, sortBranchesAsTree } from '../../../helpers/branches';
import BranchBaselineSettingModal from './BranchBaselineSettingModal';

interface Props {
  branchLikes: T.BranchLike[];
  component: T.Component;
}

interface State {
  branches: T.BranchWithNewCodePeriod[];
  editedBranch?: T.BranchWithNewCodePeriod;
  loading: boolean;
}

export default class BranchList extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    branches: [],
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchBranches();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  sortAndFilterBranches(branchLikes: T.BranchLike[] = []) {
    return sortBranchesAsTree(
      branchLikes.filter(b => isMainBranch(b) || isLongLivingBranch(b))
    ) as T.Branch[];
  }

  fetchBranches() {
    const project = this.props.component.key;
    this.setState({ loading: true });

    const sortedBranches = this.sortAndFilterBranches(this.props.branchLikes);

    listBranchesNewCodePeriod({ project }).then(
      branchSettings => {
        const newCodePeriods = branchSettings.newCodePeriods
          ? branchSettings.newCodePeriods.filter(ncp => !ncp.inherited)
          : [];

        const branchesWithBaseline = sortedBranches.map(b => {
          const newCodePeriod = newCodePeriods.find(ncp => ncp.branchKey === b.name);
          if (!newCodePeriod) {
            return b;
          }
          const { type = 'PREVIOUS_VERSION', value = null } = newCodePeriod;
          return {
            ...b,
            newCodePeriod: { type, value }
          };
        });

        this.setState({ branches: branchesWithBaseline, loading: false });
      },
      () => {
        this.setState({ loading: false });
      }
    );
  }

  updateBranchNewCodePeriod = (
    branch: string,
    newSetting: { type: T.NewCodePeriodSettingType; value: string | null } | undefined
  ) => {
    const { branches } = this.state;

    const updated = branches.find(b => b.name === branch);
    if (updated) {
      updated.newCodePeriod = newSetting;
    }
    return branches.slice(0);
  };

  openEditModal = (branch: T.BranchWithNewCodePeriod) => {
    this.setState({ editedBranch: branch });
  };

  closeEditModal = (
    branch?: string,
    newSetting?: { type: T.NewCodePeriodSettingType; value: string | null }
  ) => {
    if (branch) {
      this.setState({
        branches: this.updateBranchNewCodePeriod(branch, newSetting),
        editedBranch: undefined
      });
    } else {
      this.setState({ editedBranch: undefined });
    }
  };

  resetToDefault(branch: string) {
    return resetNewCodePeriod({
      project: this.props.component.key,
      branch
    }).then(() => {
      this.setState({ branches: this.updateBranchNewCodePeriod(branch, undefined) });
    });
  }

  renderNewCodePeriodSetting(newCodePeriod: {
    type: T.NewCodePeriodSettingType;
    value: string | null;
  }) {
    switch (newCodePeriod.type) {
      case 'SPECIFIC_ANALYSIS':
        return `${translate('baseline.specific_analysis')}: ${newCodePeriod.value}`;
      case 'NUMBER_OF_DAYS':
        return `${translate('baseline.number_days')}: ${newCodePeriod.value}`;
      case 'PREVIOUS_VERSION':
        return translate('baseline.previous_version');
      default:
        return newCodePeriod.type;
    }
  }

  render() {
    const { branches, editedBranch, loading } = this.state;

    if (branches.length < 1) {
      return null;
    }

    if (loading) {
      return <DeferredSpinner />;
    }

    return (
      <>
        <table className="data zebra">
          <thead>
            <tr>
              <th>{translate('branch_list.branch')}</th>
              <th className="thin nowrap huge-spacer-right">
                {translate('branch_list.current_setting')}
              </th>
              <th className="thin nowrap">{translate('branch_list.edit_settings')}</th>
            </tr>
          </thead>
          <tbody>
            {branches.map(branch => (
              <tr key={branch.name}>
                <td className="nowrap">
                  <BranchIcon branchLike={branch} className="little-spacer-right" />
                  {branch.name}
                  {branch.isMain && (
                    <div className="badge spacer-left">{translate('branches.main_branch')}</div>
                  )}
                </td>
                <td className="huge-spacer-right nowrap">
                  {branch.newCodePeriod ? (
                    this.renderNewCodePeriodSetting(branch.newCodePeriod)
                  ) : (
                    <span className="badge badge-info">{translate('default')}</span>
                  )}
                </td>
                <td className="text-right">
                  <ActionsDropdown>
                    <ActionsDropdownItem onClick={() => this.openEditModal(branch)}>
                      {translate('edit')}
                    </ActionsDropdownItem>
                    {branch.newCodePeriod && (
                      <ActionsDropdownItem onClick={() => this.resetToDefault(branch.name)}>
                        {translate('reset_to_default')}
                      </ActionsDropdownItem>
                    )}
                  </ActionsDropdown>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {editedBranch && (
          <BranchBaselineSettingModal
            branch={editedBranch}
            component={this.props.component.key}
            onClose={this.closeEditModal}
          />
        )}
      </>
    );
  }
}
