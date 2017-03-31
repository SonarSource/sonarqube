/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import Helmet from 'react-helmet';
import ProfileContainer from '../ProfileContainer';
import ProfileNotFound from '../ProfileNotFound';
import ProfileHeader from '../../details/ProfileHeader';
import { createFakeProfile } from '../../utils';

it('should render ProfileHeader', () => {
  const targetProfile = createFakeProfile({ key: 'profile1' });
  const profiles = [targetProfile, createFakeProfile({ key: 'profile2' })];
  const updateProfiles = jest.fn();
  const output = shallow(
    <ProfileContainer
      location={{ query: { key: 'profile1' } }}
      profiles={profiles}
      canAdmin={false}
      updateProfiles={updateProfiles}>
      <div />
    </ProfileContainer>
  );
  const header = output.find(ProfileHeader);
  expect(header.length).toBe(1);
  expect(header.prop('profile')).toBe(targetProfile);
  expect(header.prop('canAdmin')).toBe(false);
  expect(header.prop('updateProfiles')).toBe(updateProfiles);
});

it('should render ProfileNotFound', () => {
  const profiles = [createFakeProfile({ key: 'profile1' }), createFakeProfile({ key: 'profile2' })];
  const output = shallow(
    <ProfileContainer
      location={{ query: { key: 'random' } }}
      profiles={profiles}
      canAdmin={false}
      updateProfiles={() => true}>
      <div />
    </ProfileContainer>
  );
  expect(output.is(ProfileNotFound)).toBe(true);
});

it('should render Helmet', () => {
  const profiles = [createFakeProfile({ key: 'profile1', name: 'First Profile' })];
  const updateProfiles = jest.fn();
  const output = shallow(
    <ProfileContainer
      location={{ query: { key: 'profile1' } }}
      profiles={profiles}
      canAdmin={false}
      updateProfiles={updateProfiles}>
      <div />
    </ProfileContainer>
  );
  const helmet = output.find(Helmet);
  expect(helmet.length).toBe(1);
  expect(helmet.prop('title')).toContain('First Profile');
});
