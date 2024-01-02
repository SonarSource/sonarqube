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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getQualityGateProjectStatus } from '../../../../api/quality-gates';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { BranchStatusData } from '../../../../types/branch-like';
import BranchStatusContextProvider from '../BranchStatusContextProvider';

jest.mock('../../../../api/quality-gates', () => ({
  getQualityGateProjectStatus: jest.fn().mockResolvedValue({}),
}));

describe('fetchBranchStatus', () => {
  it('should get the branch status', async () => {
    const projectKey = 'projectKey';
    const branchName = 'branch-6.7';
    const status: BranchStatusData = {
      status: 'OK',
      conditions: [],
      ignoredConditions: false,
    };
    (getQualityGateProjectStatus as jest.Mock).mockResolvedValueOnce(status);
    const wrapper = shallowRender();

    wrapper.instance().fetchBranchStatus(mockBranch({ name: branchName }), projectKey);

    expect(getQualityGateProjectStatus).toHaveBeenCalledWith({ projectKey, branch: branchName });

    await waitAndUpdate(wrapper);

    expect(wrapper.state().branchStatusByComponent).toEqual({
      [projectKey]: { [`branch-${branchName}`]: status },
    });
  });

  it('should ignore errors', async () => {
    (getQualityGateProjectStatus as jest.Mock).mockRejectedValueOnce('error');
    const wrapper = shallowRender();

    wrapper.instance().fetchBranchStatus(mockBranch(), 'project');

    await waitAndUpdate(wrapper);

    expect(wrapper.state().branchStatusByComponent).toEqual({});
  });
});

function shallowRender() {
  return shallow<BranchStatusContextProvider>(
    <BranchStatusContextProvider>
      <div />
    </BranchStatusContextProvider>
  );
}
