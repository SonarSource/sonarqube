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
import { mockQualityProfile, mockQualityProfileInheritance } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import ProfileInheritance from '../ProfileInheritance';

beforeEach(() => jest.clearAllMocks());

jest.mock('../../../../api/quality-profiles', () => ({
  getProfileInheritance: jest.fn().mockResolvedValue({
    children: [mockQualityProfileInheritance()],
    ancestors: [mockQualityProfileInheritance()],
  }),
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
});

it('should render modal correctly', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleChangeParentClick();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
});

it('should handle parent change correctly', async () => {
  const updateProfiles = jest.fn().mockResolvedValueOnce({});

  const wrapper = shallowRender({ updateProfiles });
  wrapper.instance().handleParentChange();
  await waitAndUpdate(wrapper);

  expect(updateProfiles).toHaveBeenCalled();
});

function shallowRender(props: Partial<ProfileInheritance['props']> = {}) {
  return shallow<ProfileInheritance>(
    <ProfileInheritance
      profile={mockQualityProfile()}
      profiles={[mockQualityProfile()]}
      updateProfiles={jest.fn()}
      {...props}
    />
  );
}
