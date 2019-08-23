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
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { mockQualityProfile, mockRouter } from '../../../../helpers/testMocks';
import { ProfileActions } from '../ProfileActions';

const PROFILE = mockQualityProfile({
  activeRuleCount: 68,
  activeDeprecatedRuleCount: 0,
  depth: 0,
  language: 'js',
  organization: 'org',
  rulesUpdatedAt: '2017-06-28T12:58:44+0000'
});

it('renders with no permissions', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders with permission to edit only', () => {
  expect(shallowRender({ profile: { ...PROFILE, actions: { edit: true } } })).toMatchSnapshot();
});

it('renders with all permissions', () => {
  expect(
    shallowRender({
      profile: {
        ...PROFILE,
        actions: {
          copy: true,
          edit: true,
          delete: true,
          setAsDefault: true,
          associateProjects: true
        }
      }
    })
  ).toMatchSnapshot();
});

it('should copy profile', async () => {
  const name = 'new-name';
  const updateProfiles = jest.fn(() => Promise.resolve());
  const push = jest.fn();
  const wrapper = shallowRender({
    profile: { ...PROFILE, actions: { copy: true } },
    router: { push, replace: jest.fn() },
    updateProfiles
  });

  click(wrapper.find('[data-test="quality-profiles__copy"]').parent());
  expect(wrapper.find('CopyProfileForm').exists()).toBe(true);

  wrapper.find('CopyProfileForm').prop<Function>('onCopy')(name);
  expect(updateProfiles).toBeCalled();
  await waitAndUpdate(wrapper);

  expect(push).toBeCalledWith({
    pathname: '/organizations/org/quality_profiles/show',
    query: { language: 'js', name }
  });
  expect(wrapper.find('CopyProfileForm').exists()).toBe(false);
});

it('should extend profile', async () => {
  const name = 'new-name';
  const updateProfiles = jest.fn(() => Promise.resolve());
  const push = jest.fn();
  const wrapper = shallowRender({
    profile: { ...PROFILE, actions: { copy: true } },
    router: { push, replace: jest.fn() },
    updateProfiles
  });

  click(wrapper.find('[data-test="quality-profiles__extend"]').parent());
  expect(wrapper.find('ExtendProfileForm').exists()).toBe(true);

  wrapper.find('ExtendProfileForm').prop<Function>('onExtend')(name);
  expect(updateProfiles).toBeCalled();
  await waitAndUpdate(wrapper);

  expect(push).toBeCalledWith({
    pathname: '/organizations/org/quality_profiles/show',
    query: { language: 'js', name }
  });
  expect(wrapper.find('ExtendProfileForm').exists()).toBe(false);
});

function shallowRender(props: Partial<ProfileActions['props']> = {}) {
  const router = mockRouter();
  return shallow(
    <ProfileActions
      organization="org"
      profile={PROFILE}
      router={router}
      updateProfiles={jest.fn()}
      {...props}
    />
  );
}
