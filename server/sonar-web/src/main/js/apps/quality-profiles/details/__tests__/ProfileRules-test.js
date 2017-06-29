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
import React from 'react';
import { shallow } from 'enzyme';
import ProfileRules from '../ProfileRules';
import { doAsync } from '../../../../helpers/testUtils';
import * as apiRules from '../../../../api/rules';
import * as apiQP from '../../../../api/quality-profiles';

const PROFILE = {
  key: 'foo',
  name: 'Foo',
  isBuiltIn: false,
  isDefault: false,
  isInherited: false,
  language: 'java',
  languageName: 'Java',
  activeRuleCount: 68,
  activeDeprecatedRuleCount: 0,
  rulesUpdatedAt: '2017-06-28T12:58:44+0000',
  depth: 0,
  childrenCount: 0
};

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
// eslint-disable-next-line
apiRules.searchRules = data =>
  Promise.resolve(data.activation === 'true' ? apiResponseActive : apiResponseAll);
// eslint-disable-next-line
apiQP.getQualityProfiles = () =>
  Promise.resolve({
    compareToSonarWay: {
      profile: 'sonarway',
      profileName: 'Sonar way',
      missingRuleCount: 4
    }
  });

it('should render the quality profiles rules with sonarway comparison', () => {
  const wrapper = shallow(<ProfileRules canAdmin={false} organization="foo" profile={PROFILE} />);
  wrapper.instance().mounted = true;
  wrapper.instance().loadRules();
  return doAsync(() => {
    wrapper.update();
    expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(1);
    expect(wrapper).toMatchSnapshot();
  });
});

it('should show a button to activate more rules for admins', () => {
  const wrapper = shallow(<ProfileRules canAdmin={true} organization="foo" profile={PROFILE} />);
  expect(wrapper.find('.js-activate-rules')).toMatchSnapshot();
});

it('should show a deprecated rules warning message', () => {
  const wrapper = shallow(
    <ProfileRules
      canAdmin={true}
      organization="foo"
      profile={{ ...PROFILE, activeDeprecatedRuleCount: 8 }}
    />
  );
  expect(wrapper.find('ProfileRulesDeprecatedWarning')).toMatchSnapshot();
});

it('should not show a button to activate more rules on built in profiles', () => {
  const wrapper = shallow(
    <ProfileRules canAdmin={true} organization={null} profile={{ ...PROFILE, isBuiltIn: true }} />
  );
  expect(wrapper.find('.js-activate-rules')).toHaveLength(0);
});

it('should not show a button to activate more rules on built in profiles', () => {
  const wrapper = shallow(
    <ProfileRules canAdmin={true} organization={null} profile={{ ...PROFILE, isBuiltIn: true }} />
  );
  expect(wrapper.find('.js-activate-rules')).toHaveLength(0);
});

it('should not show sonarway comparison for built in profiles', () => {
  // eslint-disable-next-line
  apiQP.getQualityProfiles = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <ProfileRules canAdmin={true} organization={null} profile={{ ...PROFILE, isBuiltIn: true }} />
  );
  wrapper.instance().mounted = true;
  wrapper.instance().loadRules();
  return doAsync(() => {
    wrapper.update();
    expect(apiQP.getQualityProfiles).toHaveBeenCalledTimes(0);
    expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(0);
  });
});

it('should not show sonarway comparison if there is no missing rules', () => {
  // eslint-disable-next-line
  apiQP.getQualityProfiles = jest.fn(() =>
    Promise.resolve({
      compareToSonarWay: {
        profile: 'sonarway',
        profileName: 'Sonar way',
        missingRuleCount: 0
      }
    })
  );
  const wrapper = shallow(<ProfileRules canAdmin={true} organization={null} profile={PROFILE} />);
  wrapper.instance().mounted = true;
  wrapper.instance().loadRules();
  return doAsync(() => {
    wrapper.update();
    expect(apiQP.getQualityProfiles).toHaveBeenCalledTimes(1);
    expect(wrapper.find('ProfileRulesSonarWayComparison')).toHaveLength(0);
  });
});
