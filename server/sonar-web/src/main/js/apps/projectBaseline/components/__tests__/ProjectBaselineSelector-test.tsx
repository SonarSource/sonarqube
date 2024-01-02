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
import { mockBranch, mockMainBranch } from '../../../../helpers/mocks/branch-like';
import ProjectBaselineSelector, { ProjectBaselineSelectorProps } from '../ProjectBaselineSelector';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({
      branchesEnabled: false,
      generalSetting: { type: 'NUMBER_OF_DAYS', value: '23' },
    })
  ).toMatchSnapshot();
  expect(
    shallowRender({ branchesEnabled: false, generalSetting: { type: 'NUMBER_OF_DAYS', value: '' } })
  ).toMatchSnapshot();
});

it('should not show save button when unchanged', () => {
  const wrapper = shallowRender({
    currentSetting: 'PREVIOUS_VERSION',
    selected: 'PREVIOUS_VERSION',
    overrideGeneralSetting: true,
  });
  expect(wrapper.find('SubmitButton').parent().hasClass('invisible')).toBe(true);
});

it('should show save button when changed', () => {
  const wrapper = shallowRender({
    currentSetting: 'PREVIOUS_VERSION',
    selected: 'NUMBER_OF_DAYS',
    overrideGeneralSetting: true,
  });
  expect(wrapper.find('SubmitButton')).toHaveLength(1);
});

it('should show save button when value changed', () => {
  const wrapper = shallowRender({
    currentSetting: 'NUMBER_OF_DAYS',
    currentSettingValue: '23',
    days: '25',
    selected: 'NUMBER_OF_DAYS',
    overrideGeneralSetting: true,
  });
  expect(wrapper.find('SubmitButton')).toHaveLength(1);
});

it('should disable the save button when saving', () => {
  const wrapper = shallowRender({
    currentSetting: 'NUMBER_OF_DAYS',
    currentSettingValue: '25',
    saving: true,
    selected: 'PREVIOUS_VERSION',
    overrideGeneralSetting: true,
  });

  expect(wrapper.find('SubmitButton').first().prop('disabled')).toBe(true);
});

it('should disable the save button when date is invalid', () => {
  const wrapper = shallowRender({
    currentSetting: 'PREVIOUS_VERSION',
    days: 'hello',
    selected: 'NUMBER_OF_DAYS',
    overrideGeneralSetting: true,
  });

  expect(wrapper.find('SubmitButton').first().prop('disabled')).toBe(true);
});

function shallowRender(props: Partial<ProjectBaselineSelectorProps> = {}) {
  return shallow(
    <ProjectBaselineSelector
      branch={mockBranch()}
      branchList={[mockMainBranch()]}
      branchesEnabled={true}
      component=""
      days="12"
      generalSetting={{}}
      onCancel={jest.fn()}
      onSelectAnalysis={jest.fn()}
      onSelectDays={jest.fn()}
      onSelectReferenceBranch={jest.fn()}
      onSelectSetting={jest.fn()}
      onSubmit={jest.fn()}
      onToggleSpecificSetting={jest.fn()}
      overrideGeneralSetting={false}
      referenceBranch="master"
      saving={false}
      {...props}
    />
  );
}
