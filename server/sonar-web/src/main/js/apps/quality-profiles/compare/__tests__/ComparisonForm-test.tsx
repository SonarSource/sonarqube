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
import SelectLegacy from '../../../../components/controls/SelectLegacy';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import ComparisonForm from '../ComparisonForm';

it('should render Select with right options', () => {
  const profile = mockQualityProfile();
  const profiles = [
    profile,
    mockQualityProfile({ key: 'another', name: 'another name' }),
    mockQualityProfile({ key: 'java', name: 'java', language: 'java' })
  ];

  const profileDefault = { value: 'c', label: 'c name', isDefault: true };

  const output = shallow(
    <ComparisonForm
      onCompare={() => true}
      profile={profile}
      profiles={profiles}
      withKey="another"
    />
  ).find(SelectLegacy);
  expect(output.props().valueRenderer!(profileDefault)).toMatchSnapshot('Render default for value');
  expect(output.props().valueRenderer!(profile)).toMatchSnapshot('Render for value');

  expect(output.props().optionRenderer!(profileDefault)).toMatchSnapshot(
    'Render default for option'
  );
  expect(output.props().optionRenderer!(profile)).toMatchSnapshot('Render for option');

  expect(output.length).toBe(1);
  expect(output.prop('value')).toBe('another');
  expect(output.prop('options')).toEqual([
    { isDefault: false, value: 'another', label: 'another name' }
  ]);
});
