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
import { getBranchLikeKey } from '../helpers/branches';
import { ActionType } from './utils/actions';

export interface BranchStatusData {
  conditions?: T.QualityGateStatusCondition[];
  ignoredConditions?: boolean;
  status?: T.Status;
}

export interface State {
  byComponent: T.Dict<T.Dict<BranchStatusData>>;
}

const enum Actions {
  RegisterBranchStatus = 'REGISTER_BRANCH_STATUS'
}

type Action = ActionType<typeof registerBranchStatusAction, Actions.RegisterBranchStatus>;

export function registerBranchStatusAction(
  branchLike: T.BranchLike,
  component: string,
  status: T.Status,
  conditions?: T.QualityGateStatusCondition[],
  ignoredConditions?: boolean
) {
  return {
    type: Actions.RegisterBranchStatus,
    branchLike,
    component,
    conditions,
    ignoredConditions,
    status
  };
}

export default function(state: State = { byComponent: {} }, action: Action): State {
  if (action.type === Actions.RegisterBranchStatus) {
    const { component, conditions, branchLike, ignoredConditions, status } = action;
    const branchLikeKey = getBranchLikeKey(branchLike);
    return {
      byComponent: {
        ...state.byComponent,
        [component]: {
          ...(state.byComponent[component] || {}),
          [branchLikeKey]: {
            conditions,
            ignoredConditions,
            status
          }
        }
      }
    };
  }

  return state;
}

export function getBranchStatusByBranchLike(
  state: State,
  component: string,
  branchLike: T.BranchLike
): BranchStatusData {
  const branchLikeKey = getBranchLikeKey(branchLike);
  return state.byComponent[component] && state.byComponent[component][branchLikeKey];
}
