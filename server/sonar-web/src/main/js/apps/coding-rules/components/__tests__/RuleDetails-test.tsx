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
import { deleteRule, getRuleDetails, updateRule } from '../../../../api/rules';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { RuleType } from '../../../../types/types';
import RuleDetails from '../RuleDetails';

jest.mock('../../../../api/rules', () => {
  const { mockRuleDetails } = jest.requireActual('../../../../helpers/testMocks');
  return {
    deleteRule: jest.fn().mockResolvedValue(null),
    getRuleDetails: jest.fn().mockResolvedValue({
      rule: mockRuleDetails(),
      actives: [
        {
          qProfile: 'foo',
          inherit: 'NONE',
          severity: 'MAJOR',
          params: [],
          createdAt: '2017-06-16T16:13:38+0200',
          updatedAt: '2017-06-16T16:13:38+0200',
        },
      ],
    }),
    updateRule: jest.fn().mockResolvedValue(null),
  };
});

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('loaded');

  expect(getRuleDetails).toHaveBeenCalledWith(
    expect.objectContaining({
      actives: true,
      key: 'squid:S1337',
    })
  );
});

it('should correctly handle prop changes', async () => {
  const ruleKey = 'foo:bar';
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  jest.clearAllMocks();

  wrapper.setProps({ ruleKey });
  expect(getRuleDetails).toHaveBeenCalledWith(
    expect.objectContaining({
      actives: true,
      key: ruleKey,
    })
  );
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
    tags: 'foo,bar',
  });
});

it('should correctly handle rule changes', () => {
  const wrapper = shallowRender();
  const ruleChange = {
    createdAt: '2019-02-01',
    descriptionSections: [],
    key: 'foo',
    name: 'Foo',
    repo: 'bar',
    severity: 'MAJOR',
    status: 'READY',
    type: 'BUG' as RuleType,
  };

  wrapper.instance().handleRuleChange(ruleChange);
  expect(wrapper.state().ruleDetails).toBe(ruleChange);
});

it('should correctly handle activation', async () => {
  const onActivate = jest.fn();
  const wrapper = shallowRender({ onActivate });
  await waitAndUpdate(wrapper);

  wrapper.instance().handleActivate();
  await waitAndUpdate(wrapper);
  expect(onActivate).toHaveBeenCalledWith(
    'foo',
    'squid:S1337',
    expect.objectContaining({
      inherit: 'NONE',
      severity: 'MAJOR',
    })
  );
});

it('should correctly handle deactivation', async () => {
  const onDeactivate = jest.fn();
  const selectedProfile = mockQualityProfile({ key: 'bar' });
  const wrapper = shallowRender({ onDeactivate, selectedProfile });
  await waitAndUpdate(wrapper);

  wrapper.instance().handleDeactivate();
  await waitAndUpdate(wrapper);
  expect(onDeactivate).toHaveBeenCalledWith(selectedProfile.key, 'squid:S1337');
});

it('should correctly handle deletion', async () => {
  const onDelete = jest.fn();
  const wrapper = shallowRender({ onDelete });
  await waitAndUpdate(wrapper);

  wrapper.instance().handleDelete();
  await waitAndUpdate(wrapper);
  expect(deleteRule).toHaveBeenCalledWith(expect.objectContaining({ key: 'squid:S1337' }));
  expect(onDelete).toHaveBeenCalledWith('squid:S1337');
});

function shallowRender(props: Partial<RuleDetails['props']> = {}) {
  const profile = mockQualityProfile({ key: 'foo' });

  return shallow<RuleDetails>(
    <RuleDetails
      onActivate={jest.fn()}
      onDeactivate={jest.fn()}
      onDelete={jest.fn()}
      onFilterChange={jest.fn()}
      referencedProfiles={{ key: profile }}
      referencedRepositories={{
        javascript: { key: 'javascript', language: 'js', name: 'SonarAnalyzer' },
      }}
      ruleKey="squid:S1337"
      selectedProfile={profile}
      {...props}
    />
  );
}
