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
import { CreateOrganization } from '../CreateOrganization';
import { mockRouter, waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/billing', () => ({
  getSubscriptionPlans: jest
    .fn()
    .mockResolvedValue([{ maxNcloc: 100000, price: 10 }, { maxNcloc: 250000, price: 75 }])
}));

const organization = {
  avatar: 'http://example.com/avatar',
  description: 'description-foo',
  key: 'key-foo',
  name: 'name-foo',
  url: 'http://example.com/foo'
};

it('should render and create organization', async () => {
  const createOrganization = jest.fn().mockResolvedValue({ key: 'foo' });
  const router = mockRouter();
  const wrapper = shallow(
    // @ts-ignore avoid passing everything from WithRouterProps
    <CreateOrganization createOrganization={createOrganization} location={{}} router={router} />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper.find('OrganizationDetailsStep').prop<Function>('onContinue')(organization);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper.find('PlanStep').prop<Function>('onFreePlanChoose')();
  await waitAndUpdate(wrapper);
  expect(createOrganization).toBeCalledWith(organization);
  expect(router.push).toBeCalledWith({
    pathname: '/organizations/foo',
    state: { justCreated: true }
  });
});

it('should preselect paid plan', async () => {
  const router = mockRouter();
  const location = { state: { paid: true } };
  const wrapper = shallow(
    // @ts-ignore avoid passing everything from WithRouterProps
    <CreateOrganization createOrganization={jest.fn()} location={location} router={router} />
  );
  await waitAndUpdate(wrapper);
  wrapper.find('OrganizationDetailsStep').prop<Function>('onContinue')(organization);
  await waitAndUpdate(wrapper);

  expect(wrapper.find('PlanStep').prop('onlyPaid')).toBe(true);
});

it('should roll back after upgrade failure', async () => {
  const createOrganization = jest.fn().mockResolvedValue({ key: 'foo' });
  const deleteOrganization = jest.fn().mockResolvedValue(undefined);
  const router = mockRouter();
  const wrapper = shallow(
    <CreateOrganization
      createOrganization={createOrganization}
      deleteOrganization={deleteOrganization}
      // @ts-ignore avoid passing everything from WithRouterProps
      location={{}}
      // @ts-ignore avoid passing everything from WithRouterProps
      router={router}
    />
  );
  await waitAndUpdate(wrapper);

  wrapper.find('OrganizationDetailsStep').prop<Function>('onContinue')(organization);
  await waitAndUpdate(wrapper);

  wrapper.find('PlanStep').prop<Function>('createOrganization')();
  expect(createOrganization).toBeCalledWith(organization);

  wrapper.find('PlanStep').prop<Function>('deleteOrganization')();
  expect(deleteOrganization).toBeCalledWith(organization.key);
});
