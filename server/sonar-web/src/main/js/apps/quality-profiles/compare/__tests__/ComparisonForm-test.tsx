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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import ComparisonForm from '../ComparisonForm';

it('should render Select with right options', () => {
  const profile = mockQualityProfile();
  const profiles = [
    profile,
    mockQualityProfile({ key: 'another', name: 'another name' }),
    mockQualityProfile({ key: 'java', name: 'java', language: 'java' })
  ];

  const output = shallow(
    <ComparisonForm
      onCompare={() => true}
      profile={profile}
      profiles={profiles}
      withKey="another"
    />
  ).find('Select');
  expect(output.length).toBe(1);
  expect(output.prop('value')).toBe('another');
  expect(output.prop('options')).toEqual([{ value: 'another', label: 'another name' }]);
});
