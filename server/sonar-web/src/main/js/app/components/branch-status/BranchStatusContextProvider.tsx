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
import { getQualityGateProjectStatus } from '../../../api/quality-gates';
import { getBranchLikeKey, getBranchLikeQuery } from '../../../helpers/branch-like';
import { extractStatusConditionsFromProjectStatus } from '../../../helpers/qualityGates';
import { BranchLike, BranchStatusData } from '../../../types/branch-like';
import { QualityGateStatusCondition } from '../../../types/quality-gates';
import { Dict, Status } from '../../../types/types';
import { BranchStatusContext } from './BranchStatusContext';

interface State {
  branchStatusByComponent: Dict<Dict<BranchStatusData>>;
}

export default class BranchStatusContextProvider extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {
    branchStatusByComponent: {},
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchBranchStatus = async (branchLike: BranchLike, projectKey: string) => {
    const projectStatus = await getQualityGateProjectStatus({
      projectKey,
      ...getBranchLikeQuery(branchLike),
    }).catch(() => undefined);

    if (!this.mounted || projectStatus === undefined) {
      return;
    }

    const { ignoredConditions, status } = projectStatus;
    const conditions = extractStatusConditionsFromProjectStatus(projectStatus);

    this.updateBranchStatus(branchLike, projectKey, status, conditions, ignoredConditions);
  };

  updateBranchStatus = (
    branchLike: BranchLike,
    projectKey: string,
    status: Status,
    conditions?: QualityGateStatusCondition[],
    ignoredConditions?: boolean
  ) => {
    const branchLikeKey = getBranchLikeKey(branchLike);

    this.setState(({ branchStatusByComponent }) => ({
      branchStatusByComponent: {
        ...branchStatusByComponent,
        [projectKey]: {
          ...(branchStatusByComponent[projectKey] || {}),
          [branchLikeKey]: {
            conditions,
            ignoredConditions,
            status,
          },
        },
      },
    }));
  };

  render() {
    return (
      <BranchStatusContext.Provider
        value={{
          branchStatusByComponent: this.state.branchStatusByComponent,
          fetchBranchStatus: this.fetchBranchStatus,
          updateBranchStatus: this.updateBranchStatus,
        }}
      >
        {this.props.children}
      </BranchStatusContext.Provider>
    );
  }
}
