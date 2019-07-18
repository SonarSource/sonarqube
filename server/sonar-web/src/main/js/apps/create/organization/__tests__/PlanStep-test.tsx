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
import { submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { mockAlmOrganization } from '../../../../helpers/testMocks';
import { Plan } from '../PlanSelect';
import PlanStep from '../PlanStep';

jest.mock('../../../../helpers/extensions', () => ({
  getExtensionStart: jest.fn().mockResolvedValue(undefined)
}));
const subscriptionPlans = [{ maxNcloc: 1000, price: 100 }];

it('should render and use free plan', async () => {
  const onDone = jest.fn();
  const createOrganization = jest.fn().mockResolvedValue('org');
  const wrapper = shallow(
    <PlanStep
      createOrganization={createOrganization}
      onDone={onDone}
      onUpgradeFail={jest.fn()}
      open={true}
      subscriptionPlans={subscriptionPlans}
    />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.dive()).toMatchSnapshot();

  submit(wrapper.dive().find('form'));
  await waitAndUpdate(wrapper);
  expect(createOrganization).toBeCalled();
  expect(onDone).toBeCalled();
});

it('should upgrade', async () => {
  const onDone = jest.fn();
  const wrapper = shallow(
    <PlanStep
      createOrganization={jest.fn().mockResolvedValue('org')}
      onDone={onDone}
      onUpgradeFail={jest.fn()}
      open={true}
      subscriptionPlans={subscriptionPlans}
    />
  );
  await waitAndUpdate(wrapper);

  wrapper
    .dive()
    .find('PlanSelect')
    .prop<Function>('onChange')(Plan.Paid);
  expect(wrapper.dive()).toMatchSnapshot();

  wrapper
    .dive()
    .find('Connect(withCurrentUser(BillingFormShim))')
    .prop<Function>('onCommit')();
  expect(onDone).toBeCalled();
});

it('should preselect paid plan', async () => {
  const wrapper = shallow(
    <PlanStep
      almOrganization={mockAlmOrganization({ privateRepos: 5, publicRepos: 0 })}
      createOrganization={jest.fn()}
      onDone={jest.fn()}
      onUpgradeFail={jest.fn()}
      open={true}
      subscriptionPlans={subscriptionPlans}
    />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper.dive()).toMatchSnapshot();
});
