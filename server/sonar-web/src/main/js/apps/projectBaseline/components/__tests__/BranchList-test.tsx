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
import { listBranchesNewCodePeriod, resetNewCodePeriod } from '../../../../api/newCodePeriod';
import { mockBranch, mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import BranchBaselineSettingModal from '../BranchBaselineSettingModal';
import BranchList from '../BranchList';

jest.mock('../../../../api/newCodePeriod', () => ({
  listBranchesNewCodePeriod: jest.fn().mockResolvedValue({ newCodePeriods: [] }),
  resetNewCodePeriod: jest.fn().mockResolvedValue(null),
}));

const newCodePeriods = [
  {
    projectKey: '',
    branchKey: 'master',
    type: 'NUMBER_OF_DAYS',
    value: '27',
  },
];

it('should render correctly', async () => {
  (listBranchesNewCodePeriod as jest.Mock).mockResolvedValueOnce({ newCodePeriods });
  const wrapper = shallowRender({
    branchList: [mockMainBranch(), mockBranch(), mockBranch({ name: 'branch-7.0' })],
  });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().branches).toHaveLength(3);
  expect(wrapper).toMatchSnapshot();
});

it('should handle reset', () => {
  const component = mockComponent();
  const wrapper = shallowRender({ component });

  wrapper.instance().resetToDefault('master');

  expect(resetNewCodePeriod).toHaveBeenCalledWith({
    project: component.key,
    branch: 'master',
  });
});

it('should toggle popup', async () => {
  const wrapper = shallowRender({ branchList: [mockMainBranch(), mockBranch()] });

  wrapper.setState({ editedBranch: mockMainBranch() });

  await waitAndUpdate(wrapper);

  const nodes = wrapper.find(BranchBaselineSettingModal);
  expect(nodes).toHaveLength(1);
  expect(nodes.first().props().branch).toEqual(mockMainBranch());

  wrapper.instance().closeEditModal('master', { type: 'NUMBER_OF_DAYS', value: '23' });

  expect(wrapper.find('BranchBaselineSettingModal')).toHaveLength(0);
  expect(wrapper.state().branches.find((b) => b.name === 'master')).toEqual({
    analysisDate: '2018-01-01',
    excludedFromPurge: true,
    isMain: true,
    name: 'master',
    newCodePeriod: {
      type: 'NUMBER_OF_DAYS',
      value: '23',
    },
  });
});

function shallowRender(props: Partial<BranchList['props']> = {}) {
  return shallow<BranchList>(
    <BranchList
      branchList={[]}
      component={mockComponent()}
      inheritedSetting={{ type: 'PREVIOUS_VERSION' }}
      {...props}
    />
  );
}
