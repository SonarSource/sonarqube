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
import { ActionsDropdownItem } from '../../../../components/controls/ActionsDropdown';
import { mockBranch, mockMainBranch } from '../../../../helpers/mocks/branch-like';
import BranchListRow, { BranchListRowProps } from '../BranchListRow';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('main branch with default');
  expect(
    shallowRender({
      branch: mockBranch({ name: 'branch-7.3' }),
      inheritedSetting: { type: 'REFERENCE_BRANCH', value: 'branch-7.3' },
    })
  ).toMatchSnapshot('faulty branch');
  expect(
    shallowRender({
      branch: { ...mockBranch(), newCodePeriod: { type: 'NUMBER_OF_DAYS', value: '21' } },
    })
  ).toMatchSnapshot('branch with number of days');
  expect(
    shallowRender({
      branch: { ...mockBranch(), newCodePeriod: { type: 'PREVIOUS_VERSION' } },
    })
  ).toMatchSnapshot('branch with previous version');
  expect(
    shallowRender({
      branch: {
        ...mockBranch(),
        newCodePeriod: {
          type: 'SPECIFIC_ANALYSIS',
          value: 'A85835',
          effectiveValue: '2018-12-02T13:01:12',
        },
      },
    })
  ).toMatchSnapshot('branch with specific analysis');
  expect(
    shallowRender({
      branch: { ...mockBranch(), newCodePeriod: { type: 'REFERENCE_BRANCH', value: 'master' } },
    })
  ).toMatchSnapshot('branch with reference branch');
});

it('should callback to open modal when clicked', () => {
  const openEditModal = jest.fn();
  const branch = mockBranch();
  const wrapper = shallowRender({ branch, onOpenEditModal: openEditModal });

  wrapper.find(ActionsDropdownItem).first().simulate('click');

  expect(openEditModal).toHaveBeenCalledWith(branch);
});

it('should callback to reset when clicked', () => {
  const resetToDefault = jest.fn();
  const branchName = 'branch-6.5';
  const wrapper = shallowRender({
    branch: { ...mockBranch({ name: branchName }), newCodePeriod: { type: 'REFERENCE_BRANCH' } },
    onResetToDefault: resetToDefault,
  });

  wrapper.find(ActionsDropdownItem).at(1).simulate('click');

  expect(resetToDefault).toHaveBeenCalledWith(branchName);
});

function shallowRender(props: Partial<BranchListRowProps> = {}) {
  return shallow(
    <BranchListRow
      branch={mockMainBranch()}
      existingBranches={['master']}
      inheritedSetting={{}}
      onOpenEditModal={jest.fn()}
      onResetToDefault={jest.fn()}
      {...props}
    />
  );
}
