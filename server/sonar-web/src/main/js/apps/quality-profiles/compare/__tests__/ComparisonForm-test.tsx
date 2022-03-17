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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockReactSelectOptionProps } from '../../../../helpers/mocks/react-select';
import Select from '../../../../components/controls/Select';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import ComparisonForm from '../ComparisonForm';

it('should render Select with right options', () => {
  const output = shallowRender().find(Select);

  expect(output.length).toBe(1);
  expect(output.prop('value')).toEqual([
    { isDefault: true, value: 'another', label: 'another name' }
  ]);
  expect(output.prop('options')).toEqual([
    { isDefault: true, value: 'another', label: 'another name' }
  ]);
});

it('should render option correctly', () => {
  const wrapper = shallowRender();
  const mockOptions = [
    {
      value: 'val',
      label: 'label',
      isDefault: undefined
    }
  ];
  const OptionRenderer = wrapper.instance().optionRenderer.bind(null, mockOptions);
  expect(
    shallow(<OptionRenderer {...mockReactSelectOptionProps({ value: 'test' })} />)
  ).toMatchSnapshot('option render');
});

it('should render value correctly', () => {
  const wrapper = shallowRender();
  const mockOptions = [
    {
      value: 'val',
      label: 'label',
      isDefault: true
    }
  ];
  const ValueRenderer = wrapper.instance().singleValueRenderer.bind(null, mockOptions);
  expect(
    shallow(<ValueRenderer {...mockReactSelectOptionProps({ value: 'test' })} />)
  ).toMatchSnapshot('value render');
});

function shallowRender(overrides: Partial<ComparisonForm['props']> = {}) {
  const profile = mockQualityProfile();
  const profiles = [
    profile,
    mockQualityProfile({ key: 'another', name: 'another name', isDefault: true }),
    mockQualityProfile({ key: 'java', name: 'java', language: 'java' })
  ];

  return shallow<ComparisonForm>(
    <ComparisonForm
      onCompare={() => true}
      profile={profile}
      profiles={profiles}
      withKey="another"
      {...overrides}
    />
  );
}
