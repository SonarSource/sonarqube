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
import WorkspaceRuleDetails from '../WorkspaceRuleDetails';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { OrganizationSubscription, Visibility } from '../../../app/types';
import { hasPrivateAccess } from '../../../helpers/organizations';

jest.mock('../../../helpers/organizations', () => ({
  hasPrivateAccess: jest.fn().mockReturnValue(true)
}));

jest.mock('../../../api/rules', () => ({
  getRulesApp: jest.fn(() =>
    Promise.resolve({ repositories: [{ key: 'repo', language: 'xoo', name: 'Xoo Repository' }] })
  ),
  getRuleDetails: jest.fn(() => Promise.resolve({ rule: { key: 'foo', name: 'Foo' } }))
}));

const organization = {
  key: 'foo',
  name: 'Foo',
  projectVisibility: Visibility.Public,
  subscription: OrganizationSubscription.Paid
};

beforeEach(() => {
  (hasPrivateAccess as jest.Mock<any>).mockClear();
});

it('should render', async () => {
  const wrapper = shallow(
    <WorkspaceRuleDetails onLoad={jest.fn()} organizationKey={undefined} ruleKey="foo" />
  );
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should call back on load', async () => {
  const onLoad = jest.fn();
  const wrapper = shallow(
    <WorkspaceRuleDetails onLoad={onLoad} organizationKey={undefined} ruleKey="foo" />
  );
  await waitAndUpdate(wrapper);
  expect(onLoad).toBeCalledWith({ name: 'Foo' });
});

it('should render without permalink', async () => {
  (hasPrivateAccess as jest.Mock<any>).mockReturnValueOnce(false);
  const wrapper = shallow(
    <WorkspaceRuleDetails onLoad={jest.fn()} organizationKey={organization.key} ruleKey="foo" />
  );

  await waitAndUpdate(wrapper);
  expect(wrapper.find('RuleDetailsMeta').prop('hidePermalink')).toBeTruthy();
});
