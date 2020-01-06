/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import Toggle from 'sonar-ui-common/components/controls/Toggle';
import { excludeBranchFromPurge } from '../../../../api/branches';
import { mockBranch, mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/testMocks';
import BranchPurgeSetting from '../BranchPurgeSetting';

jest.mock('../../../../api/branches', () => ({
  excludeBranchFromPurge: jest.fn().mockResolvedValue({})
}));

beforeEach(() => jest.clearAllMocks());

it('should render correctly for a non-main branch', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.state().excludedFromPurge).toBe(true);
});

it('should render correctly for a main branch', () => {
  const wrapper = shallowRender({ branch: mockMainBranch({ excludedFromPurge: true }) });
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.state().excludedFromPurge).toBe(true);
});

it('should correctly call the webservice if the user changes the value', () => {
  const wrapper = shallowRender();
  expect(wrapper.state().excludedFromPurge).toBe(true);

  const { onChange } = wrapper.find(Toggle).props();

  if (!onChange) {
    fail();
  } else {
    onChange(false);
    expect(excludeBranchFromPurge).toHaveBeenCalled();
    expect(wrapper.state().excludedFromPurge).toBe(true);
  }
});

function shallowRender(props?: Partial<BranchPurgeSetting['props']>) {
  return shallow<BranchPurgeSetting>(
    <BranchPurgeSetting
      branch={mockBranch({ excludedFromPurge: true })}
      component={mockComponent()}
      {...props}
    />
  );
}
