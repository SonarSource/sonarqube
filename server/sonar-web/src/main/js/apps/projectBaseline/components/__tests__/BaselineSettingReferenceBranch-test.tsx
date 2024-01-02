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
import { OptionProps, Props as ReactSelectProps } from 'react-select';
import RadioCard from '../../../../components/controls/RadioCard';
import Select from '../../../../components/controls/Select';
import BaselineSettingReferenceBranch, {
  BaselineSettingReferenceBranchProps,
  BranchOption,
  renderBranchOption,
} from '../BaselineSettingReferenceBranch';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('Project level');
  expect(shallowRender({ settingLevel: 'branch' })).toMatchSnapshot('Branch level');
  expect(
    shallowRender({
      branchList: [{ label: 'master', value: 'master', isMain: true }],
      settingLevel: 'branch',
    })
  ).toMatchSnapshot('Branch level - no other branches');
});

it('should not display input when not selected', () => {
  const wrapper = shallowRender({ selected: false });
  expect(wrapper.find('SearchSelect')).toHaveLength(0);
});

it('should callback when clicked', () => {
  const onSelect = jest.fn();
  const wrapper = shallowRender({ onSelect, selected: false });

  wrapper.find(RadioCard).first().simulate('click');
  expect(onSelect).toHaveBeenCalledWith('REFERENCE_BRANCH');
});

it('should callback when changing selection', () => {
  const onChangeReferenceBranch = jest.fn();
  const wrapper = shallowRender({ onChangeReferenceBranch });

  wrapper.find(Select).first().simulate('change', { value: 'branch-6.9' });
  expect(onChangeReferenceBranch).toHaveBeenCalledWith('branch-6.9');
});

it('should handle an invalid branch', () => {
  const unknownBranchName = 'branch-unknown';
  const wrapper = shallowRender({ referenceBranch: unknownBranchName });

  expect(wrapper.find<ReactSelectProps>(Select).first().props().value).toEqual({
    label: unknownBranchName,
    value: unknownBranchName,
    isMain: false,
    isInvalid: true,
  });
});

describe('renderOption', () => {
  // fake props injected by the Select itself
  const props = {} as OptionProps<BranchOption, false>;

  it('should render correctly', () => {
    expect(
      renderBranchOption({ ...props, data: { label: 'master', value: 'master', isMain: true } })
    ).toMatchSnapshot('main');
    expect(
      renderBranchOption({
        ...props,
        data: { label: 'branch-7.4', value: 'branch-7.4', isMain: false },
      })
    ).toMatchSnapshot('branch');
    expect(
      renderBranchOption({
        ...props,
        data: { label: 'disabled', value: 'disabled', isMain: false, isDisabled: true },
      })
    ).toMatchSnapshot('disabled');
    expect(
      renderBranchOption({
        ...props,
        data: { value: 'branch-nope', isMain: false, isInvalid: true },
      })
    ).toMatchSnapshot("branch doesn't exist");
  });
});

function shallowRender(props: Partial<BaselineSettingReferenceBranchProps> = {}) {
  const branchOptions = [
    { label: 'master', value: 'master', isMain: true },
    { label: 'branch-7.9', value: 'branch-7.9', isMain: false },
  ];

  return shallow(
    <BaselineSettingReferenceBranch
      branchList={branchOptions}
      settingLevel="project"
      onChangeReferenceBranch={jest.fn()}
      onSelect={jest.fn()}
      referenceBranch="master"
      selected={true}
      {...props}
    />
  );
}
