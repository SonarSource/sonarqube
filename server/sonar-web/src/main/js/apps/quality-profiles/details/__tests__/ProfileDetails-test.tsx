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
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { ProfileDetails, ProfileDetailsProps } from '../ProfileDetails';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({ profile: mockQualityProfile({ actions: { edit: true } }) })
  ).toMatchSnapshot('edit permissions');
  expect(
    shallowRender({
      profile: mockQualityProfile({ activeRuleCount: 0, projectCount: 0 }),
    })
  ).toMatchSnapshot('no active rules (same as default)');
  expect(
    shallowRender({
      profile: mockQualityProfile({ projectCount: 0, isDefault: true, activeRuleCount: 0 }),
    })
  ).toMatchSnapshot('is default profile, no active rules');
  expect(
    shallowRender({ profile: mockQualityProfile({ projectCount: 10, activeRuleCount: 0 }) })
  ).toMatchSnapshot('projects associated, no active rules');
});

function shallowRender(props: Partial<ProfileDetailsProps> = {}) {
  return shallow<ProfileDetailsProps>(
    <ProfileDetails
      exporters={[]}
      profile={mockQualityProfile()}
      profiles={[]}
      updateProfiles={jest.fn()}
      {...props}
    />
  );
}
