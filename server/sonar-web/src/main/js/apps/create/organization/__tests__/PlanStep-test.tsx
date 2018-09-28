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
import PlanStep from '../PlanStep';
import { waitAndUpdate, click } from '../../../../helpers/testUtils';
import { Plan } from '../PlanSelect';

jest.mock('../../../../app/components/extensions/utils', () => ({
  getExtensionStart: jest.fn().mockResolvedValue(undefined)
}));

it('should render and use free plan', async () => {
  const onFreePlanChoose = jest.fn().mockResolvedValue(undefined);
  const wrapper = shallow(
    <PlanStep
      createOrganization={jest.fn().mockResolvedValue('org')}
      deleteOrganization={jest.fn().mockResolvedValue(undefined)}
      onFreePlanChoose={onFreePlanChoose}
      onPaidPlanChoose={jest.fn()}
      open={true}
      startingPrice="10"
      subscriptionPlans={[]}
    />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.dive()).toMatchSnapshot();

  click(wrapper.dive().find('SubmitButton'));
  expect(onFreePlanChoose).toBeCalled();
});

it('should upgrade', async () => {
  const onPaidPlanChoose = jest.fn();
  const wrapper = shallow(
    <PlanStep
      createOrganization={jest.fn().mockResolvedValue('org')}
      deleteOrganization={jest.fn().mockResolvedValue(undefined)}
      onFreePlanChoose={jest.fn().mockResolvedValue(undefined)}
      onPaidPlanChoose={onPaidPlanChoose}
      open={true}
      startingPrice="10"
      subscriptionPlans={[]}
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
  expect(onPaidPlanChoose).toBeCalled();
});

it('should preselect paid plan', async () => {
  const wrapper = shallow(
    <PlanStep
      createOrganization={jest.fn()}
      deleteOrganization={jest.fn().mockResolvedValue(undefined)}
      onFreePlanChoose={jest.fn().mockResolvedValue(undefined)}
      onPaidPlanChoose={jest.fn()}
      onlyPaid={true}
      open={true}
      startingPrice="10"
      subscriptionPlans={[]}
    />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.dive()).toMatchSnapshot();
});
