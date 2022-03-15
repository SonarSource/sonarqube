/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { getBranchLikeKey } from '../../helpers/branch-like';
import { mockBranch, mockPullRequest } from '../../helpers/mocks/branch-like';
import { mockQualityGateStatusCondition } from '../../helpers/mocks/quality-gates';
import { BranchLike } from '../../types/branch-like';
import { QualityGateStatusCondition } from '../../types/quality-gates';
import { Status } from '../../types/types';
import reducer, {
  fetchBranchStatus,
  getBranchStatusByBranchLike,
  registerBranchStatus,
  registerBranchStatusAction,
  State
} from '../branches';

type TestArgs = [BranchLike, string, Status, QualityGateStatusCondition[], boolean?];

const FAILING_CONDITION = mockQualityGateStatusCondition();
const COMPONENT = 'foo';
const BRANCH_STATUS_1: TestArgs = [mockPullRequest(), COMPONENT, 'ERROR', [FAILING_CONDITION]];
const BRANCH_STATUS_2: TestArgs = [mockBranch(), 'bar', 'OK', [], true];
const BRANCH_STATUS_3: TestArgs = [mockBranch(), COMPONENT, 'OK', []];

it('should allow to register new branche statuses', () => {
  const initialState: State = convertToState();

  const newState = reducer(initialState, registerBranchStatusAction(...BRANCH_STATUS_1));
  expect(newState).toEqual(convertToState([BRANCH_STATUS_1]));

  const newerState = reducer(newState, registerBranchStatusAction(...BRANCH_STATUS_2));
  expect(newerState).toEqual(convertToState([BRANCH_STATUS_1, BRANCH_STATUS_2]));
  expect(newState).toEqual(convertToState([BRANCH_STATUS_1]));
});

it('should allow to update branche statuses', () => {
  const initialState: State = convertToState([BRANCH_STATUS_1, BRANCH_STATUS_2, BRANCH_STATUS_3]);
  const branchLike: BranchLike = { ...BRANCH_STATUS_1[0], status: { qualityGateStatus: 'OK' } };
  const branchStatus: TestArgs = [branchLike, COMPONENT, 'OK', []];

  const newState = reducer(initialState, registerBranchStatusAction(...branchStatus));
  expect(newState).toEqual(convertToState([branchStatus, BRANCH_STATUS_2, BRANCH_STATUS_3]));
  expect(initialState).toEqual(convertToState([BRANCH_STATUS_1, BRANCH_STATUS_2, BRANCH_STATUS_3]));
});

it('should get the branche statuses from state', () => {
  const initialState: State = convertToState([BRANCH_STATUS_1, BRANCH_STATUS_2]);

  const [branchLike, component] = BRANCH_STATUS_1;
  expect(getBranchStatusByBranchLike(initialState, component, branchLike)).toEqual({
    conditions: [FAILING_CONDITION],
    status: 'ERROR'
  });
  expect(getBranchStatusByBranchLike(initialState, component, BRANCH_STATUS_2[0])).toBeUndefined();
});

function convertToState(items: TestArgs[] = []) {
  const state: State = { byComponent: {} };

  items.forEach(item => {
    const [branchLike, component, status, conditions, ignoredConditions] = item;
    state.byComponent[component] = {
      ...(state.byComponent[component] || {}),
      [getBranchLikeKey(branchLike)]: { conditions, ignoredConditions, status }
    };
  });

  return state;
}

jest.mock('../../app/utils/addGlobalErrorMessage', () => ({
  __esModule: true,
  default: jest.fn()
}));

jest.mock('../../api/quality-gates', () => {
  const { mockQualityGateProjectStatus } = jest.requireActual('../../helpers/mocks/quality-gates');
  return {
    getQualityGateProjectStatus: jest.fn().mockResolvedValue(
      mockQualityGateProjectStatus({
        conditions: [
          {
            actualValue: '10',
            comparator: 'GT',
            errorThreshold: '0',
            metricKey: 'foo',
            periodIndex: 1,
            status: 'ERROR'
          }
        ]
      })
    )
  };
});

describe('branch store actions', () => {
  const branchLike = mockBranch();
  const component = 'foo';
  const status = 'OK';

  it('correctly registers a new branch status', () => {
    const dispatch = jest.fn();

    registerBranchStatus(branchLike, component, status)(dispatch);
    expect(dispatch).toBeCalledWith({
      branchLike,
      component,
      status,
      type: 'REGISTER_BRANCH_STATUS'
    });
  });

  it('correctly fetches a branch status', async () => {
    const dispatch = jest.fn();

    fetchBranchStatus(branchLike, component)(dispatch);
    await new Promise(setImmediate);

    expect(dispatch).toBeCalledWith({
      branchLike,
      component,
      status,
      conditions: [
        mockQualityGateStatusCondition({
          period: 1
        })
      ],
      ignoredConditions: false,
      type: 'REGISTER_BRANCH_STATUS'
    });
  });
});
