/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { listBranchesNewCodePeriod, resetNewCodePeriod } from '../../../api/newCodePeriod';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { isBranch, sortBranches } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { Branch, BranchLike, BranchWithNewCodePeriod } from '../../../types/branch-like';
import { Component, NewCodePeriod } from '../../../types/types';
import BranchBaselineSettingModal from './BranchBaselineSettingModal';
import BranchListRow from './BranchListRow';

interface Props {
  branchList: Branch[];
  component: Component;
  inheritedSetting: NewCodePeriod;
}

interface State {
  branches: BranchWithNewCodePeriod[];
  editedBranch?: BranchWithNewCodePeriod;
  loading: boolean;
}

export default class BranchList extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    branches: [],
    loading: true,
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchBranches();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  sortAndFilterBranches(branchLikes: BranchLike[] = []) {
    return sortBranches(branchLikes.filter(isBranch));
  }

  fetchBranches() {
    const project = this.props.component.key;
    this.setState({ loading: true });

    listBranchesNewCodePeriod({ project }).then(
      (branchSettings) => {
        const newCodePeriods = branchSettings.newCodePeriods
          ? branchSettings.newCodePeriods.filter((ncp) => !ncp.inherited)
          : [];

        const branchesWithBaseline = this.props.branchList.map((b) => {
          const newCodePeriod = newCodePeriods.find((ncp) => ncp.branchKey === b.name);
          if (!newCodePeriod) {
            return b;
          }
          const { type = 'PREVIOUS_VERSION', value, effectiveValue } = newCodePeriod;
          return {
            ...b,
            newCodePeriod: { type, value, effectiveValue },
          };
        });

        this.setState({ branches: branchesWithBaseline, loading: false });
      },
      () => {
        this.setState({ loading: false });
      }
    );
  }

  updateBranchNewCodePeriod = (branch: string, newSetting: NewCodePeriod | undefined) => {
    const { branches } = this.state;

    const updated = branches.find((b) => b.name === branch);
    if (updated) {
      updated.newCodePeriod = newSetting;
    }
    return branches.slice(0);
  };

  openEditModal = (branch: BranchWithNewCodePeriod) => {
    this.setState({ editedBranch: branch });
  };

  closeEditModal = (branch?: string, newSetting?: NewCodePeriod) => {
    if (branch) {
      this.setState({
        branches: this.updateBranchNewCodePeriod(branch, newSetting),
        editedBranch: undefined,
      });
    } else {
      this.setState({ editedBranch: undefined });
    }
  };

  resetToDefault = (branch: string) => {
    return resetNewCodePeriod({
      project: this.props.component.key,
      branch,
    }).then(() => {
      this.setState({ branches: this.updateBranchNewCodePeriod(branch, undefined) });
    });
  };

  render() {
    const { branchList, inheritedSetting } = this.props;
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
              <th className="nowrap huge-spacer-right">
                {translate('branch_list.current_setting')}
              </th>
              <th className="thin nowrap">{translate('branch_list.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {branches.map((branch) => (
              <BranchListRow
                branch={branch}
                existingBranches={branchList.map((b) => b.name)}
                inheritedSetting={inheritedSetting}
                key={branch.name}
                onOpenEditModal={this.openEditModal}
                onResetToDefault={this.resetToDefault}
              />
            ))}
          </tbody>
        </table>
        {editedBranch && (
          <BranchBaselineSettingModal
            branch={editedBranch}
            branchList={branchList}
            component={this.props.component.key}
            onClose={this.closeEditModal}
          />
        )}
      </>
    );
  }
}
