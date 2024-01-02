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
import { getQualityProfile } from '../../../../api/quality-profiles';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import ProfileRules from '../ProfileRules';

const PROFILE = mockQualityProfile({
  activeRuleCount: 68,
  activeDeprecatedRuleCount: 0,
  depth: 0,
  language: 'js',
  rulesUpdatedAt: '2017-06-28T12:58:44+0000',
});

const EDITABLE_PROFILE = { ...PROFILE, actions: { edit: true } };

const apiResponseAll = {
  total: 253,
  facets: [
    {
      property: 'types',
      values: [
        { val: 'CODE_SMELL', count: 168 },
        { val: 'BUG', count: 68 },
        { val: 'VULNERABILITY', count: 7 },
        { val: 'SECURITY_HOTSPOT', count: 10 },
      ],
    },
  ],
};

const apiResponseActive = {
  total: 68,
  facets: [
    {
      property: 'types',
      values: [
        { val: 'BUG', count: 68 },
        { val: 'CODE_SMELL', count: 0 },
        { val: 'VULNERABILITY', count: 0 },
        { val: 'SECURITY_HOTSPOT', count: 0 },
      ],
    },
  ],
};

jest.mock('../../../../api/rules', () => ({
  ...jest.requireActual('../../../../api/rules'),
  searchRules: jest
    .fn()
    .mockImplementation((data: any) =>
      Promise.resolve(data.activation === 'true' ? apiResponseActive : apiResponseAll)
    ),
}));

jest.mock('../../../../api/quality-profiles', () => ({
  ...jest.requireActual('../../../../api/quality-profiles'),
  getQualityProfile: jest.fn().mockImplementation(() =>
    Promise.resolve({
      compareToSonarWay: {
        profile: 'sonarway',
        profileName: 'Sonar way',
        missingRuleCount: 4,
      },
    })
  ),
}));

beforeEach(jest.clearAllMocks);

it('should render the quality profiles rules with sonarway comparison', async () => {
  const wrapper = shallow(<ProfileRules profile={PROFILE} />);
  const instance = wrapper.instance() as any;
  instance.mounted = true;
  instance.loadRules();
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(1);
  expect(wrapper).toMatchSnapshot();
});

it('should show a button to activate more rules for admins', () => {
  const wrapper = shallow(<ProfileRules profile={EDITABLE_PROFILE} />);
  expect(wrapper.find('.js-activate-rules')).toMatchSnapshot();
});

it('should show a disabled button to activate more rules for built-in profiles', () => {
  const wrapper = shallow(
    <ProfileRules profile={{ ...EDITABLE_PROFILE, actions: { copy: true }, isBuiltIn: true }} />
  );
  expect(wrapper.find('.js-activate-rules')).toMatchSnapshot();
});

it('should show a deprecated rules warning message', () => {
  const wrapper = shallow(
    <ProfileRules profile={{ ...EDITABLE_PROFILE, activeDeprecatedRuleCount: 8 }} />
  );
  expect(wrapper.find('ProfileRulesDeprecatedWarning')).toMatchSnapshot();
});

it('should not show a button to activate more rules on built in profiles', () => {
  const wrapper = shallow(<ProfileRules profile={{ ...EDITABLE_PROFILE, isBuiltIn: true }} />);
  expect(wrapper.find('.js-activate-rules').exists()).toBe(false);
});

it('should not show sonarway comparison for built in profiles', async () => {
  (getQualityProfile as jest.Mock).mockReturnValueOnce({});
  const wrapper = shallow(<ProfileRules profile={{ ...PROFILE, isBuiltIn: true }} />);
  await new Promise(setImmediate);
  wrapper.update();
  expect(getQualityProfile).toHaveBeenCalledTimes(0);
  expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(0);
});

it('should not show sonarway comparison if there is no missing rules', async () => {
  (getQualityProfile as jest.Mock).mockReturnValueOnce({
    compareToSonarWay: {
      profile: 'sonarway',
      profileName: 'Sonar way',
      missingRuleCount: 0,
    },
  });
  const wrapper = shallow(<ProfileRules profile={PROFILE} />);
  await waitAndUpdate(wrapper);
  expect(getQualityProfile).toHaveBeenCalledTimes(1);
  expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(0);
});
