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
import ProfileRules from '../ProfileRules';
import * as apiRules from '../../../../api/rules';
import * as apiQP from '../../../../api/quality-profiles';
import { waitAndUpdate } from '../../../../helpers/testUtils';

const PROFILE = {
  activeRuleCount: 68,
  activeDeprecatedRuleCount: 0,
  childrenCount: 0,
  depth: 0,
  isBuiltIn: false,
  isDefault: false,
  isInherited: false,
  key: 'foo',
  language: 'java',
  languageName: 'Java',
  name: 'Foo',
  organization: 'org',
  rulesUpdatedAt: '2017-06-28T12:58:44+0000'
};

const EDITABLE_PROFILE = { ...PROFILE, actions: { edit: true } };

const apiResponseAll = {
  total: 243,
  facets: [
    {
      property: 'types',
      values: [
        { val: 'CODE_SMELL', count: 168 },
        { val: 'BUG', count: 68 },
        { val: 'VULNERABILITY', count: 7 }
      ]
    }
  ]
};

const apiResponseActive = {
  total: 68,
  facets: [
    {
      property: 'types',
      values: [
        { val: 'BUG', count: 68 },
        { val: 'CODE_SMELL', count: 0 },
        { val: 'VULNERABILITY', count: 0 }
      ]
    }
  ]
};

// Mock api some api functions
(apiRules as any).searchRules = (data: any) =>
  Promise.resolve(data.activation === 'true' ? apiResponseActive : apiResponseAll);
(apiQP as any).getQualityProfile = () =>
  Promise.resolve({
    compareToSonarWay: {
      profile: 'sonarway',
      profileName: 'Sonar way',
      missingRuleCount: 4
    }
  });

it('should render the quality profiles rules with sonarway comparison', async () => {
  const wrapper = shallow(<ProfileRules organization="foo" profile={PROFILE} />);
  const instance = wrapper.instance() as any;
  instance.mounted = true;
  instance.loadRules();
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(1);
  expect(wrapper).toMatchSnapshot();
});

it('should show a button to activate more rules for admins', () => {
  const wrapper = shallow(<ProfileRules organization="foo" profile={EDITABLE_PROFILE} />);
  expect(wrapper.find('.js-activate-rules')).toMatchSnapshot();
});

it('should show a deprecated rules warning message', () => {
  const wrapper = shallow(
    <ProfileRules
      organization="foo"
      profile={{ ...EDITABLE_PROFILE, activeDeprecatedRuleCount: 8 }}
    />
  );
  expect(wrapper.find('ProfileRulesDeprecatedWarning')).toMatchSnapshot();
});

it('should not show a button to activate more rules on built in profiles', () => {
  const wrapper = shallow(
    <ProfileRules organization={null} profile={{ ...EDITABLE_PROFILE, isBuiltIn: true }} />
  );
  expect(wrapper.find('.js-activate-rules')).toHaveLength(0);
});

it('should not show sonarway comparison for built in profiles', async () => {
  (apiQP as any).getQualityProfile = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <ProfileRules organization={null} profile={{ ...PROFILE, isBuiltIn: true }} />
  );
  await new Promise(setImmediate);
  wrapper.update();
  expect(apiQP.getQualityProfile).toHaveBeenCalledTimes(0);
  expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(0);
});

it('should not show sonarway comparison if there is no missing rules', async () => {
  (apiQP as any).getQualityProfile = jest.fn(() =>
    Promise.resolve({
      compareToSonarWay: {
        profile: 'sonarway',
        profileName: 'Sonar way',
        missingRuleCount: 0
      }
    })
  );
  const wrapper = shallow(<ProfileRules organization={null} profile={PROFILE} />);
  await waitAndUpdate(wrapper);
  expect(apiQP.getQualityProfile).toHaveBeenCalledTimes(1);
  expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(0);
});
