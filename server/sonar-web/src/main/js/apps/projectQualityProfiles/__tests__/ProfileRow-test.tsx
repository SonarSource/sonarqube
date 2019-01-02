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
import * as React from 'react';
import { shallow } from 'enzyme';
import ProfileRow from '../ProfileRow';

it('renders', () => {
  expect(
    shallow(
      <ProfileRow
        onChangeProfile={jest.fn()}
        possibleProfiles={[randomProfile('bar'), randomProfile('baz')]}
        profile={randomProfile('foo')}
      />
    )
  ).toMatchSnapshot();
});

it('changes profile', async () => {
  const onChangeProfile = jest.fn(() => Promise.resolve());
  const wrapper = shallow<ProfileRow>(
    <ProfileRow
      onChangeProfile={onChangeProfile}
      possibleProfiles={[randomProfile('bar'), randomProfile('baz')]}
      profile={randomProfile('foo')}
    />
  );
  (wrapper.instance() as ProfileRow).mounted = true;
  wrapper.find('Select').prop<Function>('onChange')({ value: 'baz' });
  expect(onChangeProfile).toBeCalledWith('foo', 'baz');
  expect(wrapper.state().loading).toBeTruthy();
  await new Promise(setImmediate);
  expect(wrapper.state().loading).toBeFalsy();
});

function randomProfile(key: string) {
  return {
    activeRuleCount: 17,
    activeDeprecatedRuleCount: 0,
    key,
    name: key,
    language: 'xoo',
    languageName: 'xoo',
    organization: 'org'
  };
}
