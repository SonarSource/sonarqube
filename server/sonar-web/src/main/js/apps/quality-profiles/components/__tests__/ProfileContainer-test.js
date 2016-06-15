/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { expect } from 'chai';
import { shallow } from 'enzyme';
import sinon from 'sinon';
import React from 'react';
import Helmet from 'react-helmet';
import ProfileContainer from '../ProfileContainer';
import ProfileNotFound from '../ProfileNotFound';
import ProfileHeader from '../../details/ProfileHeader';
import { createFakeProfile } from '../../utils';

describe('Quality Profiles :: ProfileContainer', () => {
  it('should render ProfileHeader', () => {
    const targetProfile = createFakeProfile({ key: 'profile1' });
    const profiles = [
      targetProfile,
      createFakeProfile({ key: 'profile2' })
    ];
    const updateProfiles = sinon.spy();
    const output = shallow(
        <ProfileContainer
            location={{ query: { key: 'profile1' } }}
            profiles={profiles}
            canAdmin={false}
            updateProfiles={updateProfiles}>
          <div/>
        </ProfileContainer>
    );
    const header = output.find(ProfileHeader);
    expect(header).to.have.length(1);
    expect(header.prop('profile')).to.equal(targetProfile);
    expect(header.prop('canAdmin')).to.equal(false);
    expect(header.prop('updateProfiles')).to.equal(updateProfiles);
  });

  it('should render ProfileNotFound', () => {
    const profiles = [
      createFakeProfile({ key: 'profile1' }),
      createFakeProfile({ key: 'profile2' })
    ];
    const output = shallow(
        <ProfileContainer
            location={{ query: { key: 'random' } }}
            profiles={profiles}
            canAdmin={false}
            updateProfiles={() => true}>
          <div/>
        </ProfileContainer>
    );
    expect(output.is(ProfileNotFound)).to.equal(true);
  });

  it('should render Helmet', () => {
    const profiles = [
      createFakeProfile({ key: 'profile1', name: 'First Profile' })
    ];
    const updateProfiles = sinon.spy();
    const output = shallow(
        <ProfileContainer
            location={{ query: { key: 'profile1' } }}
            profiles={profiles}
            canAdmin={false}
            updateProfiles={updateProfiles}>
          <div/>
        </ProfileContainer>
    );
    const helmet = output.find(Helmet);
    expect(helmet).to.have.length(1);
    expect(helmet.prop('title')).to.include('First Profile');
  });
});
