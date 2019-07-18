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
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { bindAlmOrganization } from '../../../../api/alm-integration';
import { mockAlmApplication, mockAlmOrganization } from '../../../../helpers/testMocks';
import AutoOrganizationCreate from '../AutoOrganizationCreate';
import { Step } from '../utils';

jest.mock('../../../../api/alm-integration', () => ({
  bindAlmOrganization: jest.fn().mockResolvedValue({})
}));

const organization = mockAlmOrganization();

it('should render prefilled and create org', async () => {
  const createOrganization = jest.fn().mockResolvedValue({ key: 'foo' });
  const handleOrgDetailsFinish = jest.fn();
  const almOrganization = mockAlmOrganization({ almUrl: 'http://github.com/thing' });
  const wrapper = shallowRender({
    almOrganization,
    createOrganization,
    handleOrgDetailsFinish
  });

  expect(wrapper).toMatchSnapshot();

  wrapper.find('OrganizationDetailsForm').prop<Function>('onContinue')(organization);
  await waitAndUpdate(wrapper);
  expect(handleOrgDetailsFinish).toBeCalled();

  wrapper.setProps({ organization });
  wrapper.find('PlanStep').prop<Function>('createOrganization')();

  const alm: T.Organization['alm'] = {
    key: 'github',
    membersSync: true,
    personal: false,
    url: 'http://github.com/thing'
  };
  expect(createOrganization).toBeCalledWith({ ...organization, alm, installationId: 'id-foo' });
});

it('should allow to cancel org import', () => {
  const handleCancelImport = jest.fn().mockResolvedValue({ key: 'foo' });
  const wrapper = shallowRender({ handleCancelImport });

  click(wrapper.find('ClearButton'));
  expect(handleCancelImport).toBeCalled();
});

it('should display choice between import or creation', () => {
  const wrapper = shallowRender({ unboundOrganizations: [organization] });
  expect(wrapper).toMatchSnapshot();

  wrapper.find('RadioToggle').prop<Function>('onCheck')('create');
  wrapper.update();
  expect(wrapper.find('OrganizationDetailsForm').exists()).toBe(true);

  wrapper.find('RadioToggle').prop<Function>('onCheck')('bind');
  wrapper.update();
  expect(wrapper.find('AutoOrganizationBind').exists()).toBe(true);
});

it('should bind existing organization', async () => {
  const onOrgCreated = jest.fn();
  const wrapper = shallowRender({ onOrgCreated, unboundOrganizations: [organization] });

  wrapper.find('RadioToggle').prop<Function>('onCheck')('bind');
  wrapper.update();
  wrapper.find('AutoOrganizationBind').prop<Function>('onBindOrganization')('foo');
  expect(bindAlmOrganization as jest.Mock<any>).toHaveBeenCalledWith({
    installationId: 'id-foo',
    organization: 'foo'
  });
  await waitAndUpdate(wrapper);
  expect(onOrgCreated).toHaveBeenCalledWith('foo');
});

it('should not show member sync info box for Bitbucket', () => {
  expect(
    shallowRender({ almApplication: mockAlmApplication({ key: 'bitbucket-cloud' }) })
      .find('Alert')
      .exists()
  ).toBe(false);
});

function shallowRender(props: Partial<AutoOrganizationCreate['props']> = {}) {
  return shallow(
    <AutoOrganizationCreate
      almApplication={mockAlmApplication()}
      almInstallId="id-foo"
      almOrganization={organization}
      createOrganization={jest.fn()}
      handleCancelImport={jest.fn()}
      handleOrgDetailsFinish={jest.fn()}
      handleOrgDetailsStepOpen={jest.fn()}
      onDone={jest.fn()}
      onOrgCreated={jest.fn()}
      onUpgradeFail={jest.fn()}
      step={Step.OrganizationDetails}
      subscriptionPlans={[{ maxNcloc: 100000, price: 10 }, { maxNcloc: 250000, price: 75 }]}
      unboundOrganizations={[]}
      {...props}
    />
  );
}
