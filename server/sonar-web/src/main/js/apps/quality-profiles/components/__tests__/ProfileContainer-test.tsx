/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import Helmet from 'react-helmet';
import { WithRouterProps } from 'react-router';
import ProfileContainer from '../ProfileContainer';
import ProfileNotFound from '../ProfileNotFound';
import ProfileHeader from '../../details/ProfileHeader';
import { createFakeProfile } from '../../utils';

const routerProps = { router: {} } as WithRouterProps;

it('should render ProfileHeader', () => {
  const targetProfile = createFakeProfile({ language: 'js', name: 'fake' });
  const profiles = [targetProfile, createFakeProfile({ language: 'js', name: 'another' })];
  const updateProfiles = jest.fn();
  const output = shallow(
    <ProfileContainer
      location={{ pathname: '', query: { language: 'js', name: 'fake' } }}
      onRequestFail={jest.fn()}
      organization={null}
      profiles={profiles}
      updateProfiles={updateProfiles}
      {...routerProps}>
      <div />
    </ProfileContainer>
  );
  const header = output.find(ProfileHeader);
  expect(header.length).toBe(1);
  expect(header.prop('profile')).toBe(targetProfile);
  expect(header.prop('updateProfiles')).toBe(updateProfiles);
});

it('should render ProfileNotFound', () => {
  const profiles = [
    createFakeProfile({ language: 'js', name: 'fake' }),
    createFakeProfile({ language: 'js', name: 'another' })
  ];
  const output = shallow(
    <ProfileContainer
      location={{ pathname: '', query: { language: 'js', name: 'random' } }}
      onRequestFail={jest.fn()}
      organization={null}
      profiles={profiles}
      updateProfiles={jest.fn()}
      {...routerProps}>
      <div />
    </ProfileContainer>
  );
  expect(output.is(ProfileNotFound)).toBe(true);
});

it('should render Helmet', () => {
  const profiles = [createFakeProfile({ language: 'js', name: 'First Profile' })];
  const updateProfiles = jest.fn();
  const output = shallow(
    <ProfileContainer
      location={{ pathname: '', query: { language: 'js', name: 'First Profile' } }}
      onRequestFail={jest.fn()}
      organization={null}
      profiles={profiles}
      updateProfiles={updateProfiles}
      {...routerProps}>
      <div />
    </ProfileContainer>
  );
  const helmet = output.find(Helmet);
  expect(helmet.length).toBe(1);
  expect(helmet.prop('title')).toContain('First Profile');
});
