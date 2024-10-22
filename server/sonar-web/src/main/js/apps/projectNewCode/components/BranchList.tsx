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
import { ActionCell, ContentCell, Spinner, Table, TableRow } from '~design-system';
import { isBranch } from '~sonar-aligned/helpers/branch-like';
import {
  listBranchesNewCodeDefinition,
  resetNewCodeDefinition,
} from '../../../api/newCodeDefinition';
import BranchNCDAutoUpdateMessage from '../../../components/new-code-definition/BranchNCDAutoUpdateMessage';
import {
  PreviouslyNonCompliantBranchNCD,
  isPreviouslyNonCompliantDaysNCD,
} from '../../../components/new-code-definition/utils';
import { sortBranches } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { DEFAULT_NEW_CODE_DEFINITION_TYPE } from '../../../helpers/new-code-definition';
import { Branch, BranchLike, BranchWithNewCodePeriod } from '../../../types/branch-like';
import { NewCodeDefinition } from '../../../types/new-code-definition';
import { Component } from '../../../types/types';
import BranchListRow from './BranchListRow';
import BranchNewCodeDefinitionSettingModal from './BranchNewCodeDefinitionSettingModal';

interface Props {
  branchList: Branch[];
  component: Component;
  globalNewCodeDefinition: NewCodeDefinition;
  inheritedSetting: NewCodeDefinition;
}

interface State {
  branches: BranchWithNewCodePeriod[];
  editedBranch?: BranchWithNewCodePeriod;
  loading: boolean;
  previouslyNonCompliantBranchNCDs?: PreviouslyNonCompliantBranchNCD[];
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

  componentDidUpdate(prevProps: Props) {
    if (prevProps.branchList !== this.props.branchList) {
      this.fetchBranches();
    }
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

    listBranchesNewCodeDefinition({ project }).then(
      (branchSettings) => {
        const newCodePeriods = branchSettings.newCodePeriods
          ? branchSettings.newCodePeriods.filter((ncp) => !ncp.inherited)
          : [];

        const branchesWithBaseline = this.props.branchList.map((b) => {
          const newCodePeriod = newCodePeriods.find((ncp) => ncp.branchKey === b.name);
          if (!newCodePeriod) {
            return b;
          }
          const { type = DEFAULT_NEW_CODE_DEFINITION_TYPE, value, effectiveValue } = newCodePeriod;
          return {
            ...b,
            newCodePeriod: { type, value, effectiveValue },
          };
        });

        const previouslyNonCompliantBranchNCDs = newCodePeriods.filter(
          isPreviouslyNonCompliantDaysNCD,
        );

        this.setState({
          branches: branchesWithBaseline,
          loading: false,
          previouslyNonCompliantBranchNCDs,
        });
      },
      () => {
        this.setState({ loading: false });
      },
    );
  }

  updateBranchNewCodePeriod = (branch: string, newSetting: NewCodeDefinition | undefined) => {
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

  closeEditModal = (branch?: string, newSetting?: NewCodeDefinition) => {
    if (branch !== undefined) {
      this.setState(({ previouslyNonCompliantBranchNCDs }) => ({
        branches: this.updateBranchNewCodePeriod(branch, newSetting),
        previouslyNonCompliantBranchNCDs: previouslyNonCompliantBranchNCDs?.filter(
          ({ branchKey }) => branchKey !== branch,
        ),
        editedBranch: undefined,
      }));
    } else {
      this.setState({ editedBranch: undefined });
    }
  };

  resetToDefault = (branch: string) => {
    return resetNewCodeDefinition({
      project: this.props.component.key,
      branch,
    }).then(() => {
      this.setState({ branches: this.updateBranchNewCodePeriod(branch, undefined) });
    });
  };

  render() {
    const { branchList, component, inheritedSetting, globalNewCodeDefinition } = this.props;
    const { branches, editedBranch, loading, previouslyNonCompliantBranchNCDs } = this.state;

    if (branches.length < 1) {
      return null;
    }

    if (loading) {
      return <Spinner />;
    }

    const header = (
      <TableRow>
        <ContentCell>{translate('branch_list.branch')}</ContentCell>
        <ContentCell>{translate('branch_list.current_setting')}</ContentCell>
        <ActionCell>{translate('branch_list.actions')}</ActionCell>
      </TableRow>
    );

    return (
      <div>
        {previouslyNonCompliantBranchNCDs && (
          <BranchNCDAutoUpdateMessage
            component={component}
            previouslyNonCompliantBranchNCDs={previouslyNonCompliantBranchNCDs}
          />
        )}
        <Table columnCount={3} header={header}>
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
        </Table>
        {editedBranch && (
          <BranchNewCodeDefinitionSettingModal
            branch={editedBranch}
            branchList={branchList}
            component={this.props.component.key}
            onClose={this.closeEditModal}
            inheritedSetting={inheritedSetting}
            globalNewCodeDefinition={globalNewCodeDefinition}
          />
        )}
      </div>
    );
  }
}
