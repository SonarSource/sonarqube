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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { mockOrganization } from '../../../../helpers/testMocks';
import ManualOrganizationCreate from '../ManualOrganizationCreate';
import { Step } from '../utils';

it('should render and create organization', async () => {
  const createOrganization = jest.fn().mockResolvedValue({ key: 'foo' });
  const onDone = jest.fn();
  const handleOrgDetailsFinish = jest.fn();
  const wrapper = shallowRender({ createOrganization, handleOrgDetailsFinish, onDone });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper.find('OrganizationDetailsForm').prop<Function>('onContinue')(mockOrganization());
  await waitAndUpdate(wrapper);
  expect(handleOrgDetailsFinish).toHaveBeenCalled();
  wrapper.setProps({ step: Step.Plan });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<ManualOrganizationCreate['props']> = {}) {
  return shallow(
    <ManualOrganizationCreate
      createOrganization={jest.fn()}
      handleOrgDetailsFinish={jest.fn()}
      handleOrgDetailsStepOpen={jest.fn()}
      onDone={jest.fn()}
      onUpgradeFail={jest.fn()}
      step={Step.OrganizationDetails}
      subscriptionPlans={[{ maxNcloc: 100000, price: 10 }, { maxNcloc: 250000, price: 75 }]}
      {...props}
    />
  );
}
