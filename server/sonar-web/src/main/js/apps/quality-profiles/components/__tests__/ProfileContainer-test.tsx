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
import { Helmet } from 'react-helmet-async';
import { WithRouterProps } from 'react-router';
import { mockLocation, mockQualityProfile, mockRouter } from '../../../../helpers/testMocks';
import ProfileHeader from '../../details/ProfileHeader';
import ProfileContainer from '../ProfileContainer';
import ProfileNotFound from '../ProfileNotFound';

it('should render ProfileHeader', () => {
  const targetProfile = mockQualityProfile({ name: 'fake' });
  const profiles = [targetProfile, mockQualityProfile({ name: 'another' })];
  const updateProfiles = jest.fn();
  const location = mockLocation({ pathname: '', query: { language: 'js', name: 'fake' } });

  const output = shallowRender({ profiles, updateProfiles, location });

  const header = output.find(ProfileHeader);
  expect(header.length).toBe(1);
  expect(header.prop('profile')).toBe(targetProfile);
  expect(header.prop('updateProfiles')).toBe(updateProfiles);
});

it('should render ProfileNotFound', () => {
  const profiles = [mockQualityProfile({ name: 'fake' }), mockQualityProfile({ name: 'another' })];
  const location = mockLocation({ pathname: '', query: { language: 'js', name: 'random' } });

  const output = shallowRender({ profiles, location });

  expect(output.is(ProfileNotFound)).toBe(true);
});

it('should render Helmet', () => {
  const name = 'First Profile';
  const profiles = [mockQualityProfile({ name })];
  const updateProfiles = jest.fn();
  const location = mockLocation({ pathname: '', query: { language: 'js', name } });

  const output = shallowRender({ profiles, updateProfiles, location });

  const helmet = output.find(Helmet);
  expect(helmet.length).toBe(1);
  expect(helmet.prop('title')).toContain(name);
});

function shallowRender(overrides: Partial<ProfileContainer['props']> = {}) {
  const routerProps = { router: mockRouter(), ...overrides } as WithRouterProps;

  return shallow(
    <ProfileContainer profiles={[]} updateProfiles={jest.fn()} {...routerProps} {...overrides}>
      <div />
    </ProfileContainer>
  );
}
