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
import RuleDetails from '../RuleDetails';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { updateRule } from '../../../../api/rules';

jest.mock('../../../../api/rules', () => ({
  deleteRule: jest.fn(),
  getRuleDetails: jest.fn().mockResolvedValue({
    rule: getMockHelpers().mockRuleDetails(),
    actives: [
      {
        qProfile: 'key',
        inherit: 'NONE',
        severity: 'MAJOR',
        params: [],
        createdAt: '2017-06-16T16:13:38+0200',
        updatedAt: '2017-06-16T16:13:38+0200'
      }
    ]
  }),
  updateRule: jest.fn().mockResolvedValue({})
}));

const { mockQualityProfile } = getMockHelpers();
const profile = mockQualityProfile();

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle tag changes', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.instance().handleTagsChange(['foo', 'bar']);
  const ruleDetails = wrapper.state('ruleDetails');
  expect(ruleDetails && ruleDetails.tags).toEqual(['foo', 'bar']);
  await waitAndUpdate(wrapper);
  expect(updateRule).toHaveBeenCalledWith({
    key: 'squid:S1337',
    organization: undefined,
    tags: 'foo,bar'
  });
});

function getMockHelpers() {
  // We use this little "force-requiring" instead of an import statement in
  // order to prevent a hoisting race condition while mocking. If we want to use
  // a mock helper in a Jest mock, we have to require it like this. Otherwise,
  // we get errors like:
  //     ReferenceError: testMocks_1 is not defined
  return require.requireActual('../../../../helpers/testMocks');
}

function shallowRender(props: Partial<RuleDetails['props']> = {}) {
  return shallow<RuleDetails>(
    <RuleDetails
      onActivate={jest.fn()}
      onDeactivate={jest.fn()}
      onDelete={jest.fn()}
      onFilterChange={jest.fn()}
      organization={undefined}
      referencedProfiles={{ key: profile }}
      referencedRepositories={{
        javascript: { key: 'javascript', language: 'js', name: 'SonarAnalyzer' }
      }}
      ruleKey="squid:S1337"
      selectedProfile={profile}
      {...props}
    />
  );
}
