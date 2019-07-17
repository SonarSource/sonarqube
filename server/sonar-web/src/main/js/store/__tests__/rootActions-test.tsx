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
import { mockLongLivingBranch, mockQualityGateStatusCondition } from '../../helpers/testMocks';
import { registerBranchStatusAction } from '../branches';
import { fetchBranchStatus, registerBranchStatus } from '../rootActions';

jest.mock('../branches', () => ({
  ...require.requireActual('../branches'),
  registerBranchStatusAction: jest.fn()
}));

jest.mock('../../api/quality-gates', () => {
  const { mockQualityGateProjectStatus } = require.requireActual('../../helpers/testMocks');
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
  const branchLike = mockLongLivingBranch();
  const component = 'foo';
  const status = 'OK';

  it('correctly registers a new branch status', () => {
    const dispatch = jest.fn();

    registerBranchStatus(branchLike, component, status)(dispatch);
    expect(registerBranchStatusAction).toBeCalledWith(branchLike, component, status);
    expect(dispatch).toBeCalled();
  });

  it('correctly fetches a branch status', async () => {
    const dispatch = jest.fn();

    fetchBranchStatus(branchLike, component)(dispatch);
    await new Promise(setImmediate);

    expect(registerBranchStatusAction).toBeCalledWith(
      branchLike,
      component,
      status,
      [
        mockQualityGateStatusCondition({
          period: 1
        })
      ],
      false
    );
    expect(dispatch).toBeCalled();
  });
});
