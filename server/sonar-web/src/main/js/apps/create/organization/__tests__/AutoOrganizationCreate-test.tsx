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
import AutoOrganizationCreate from '../AutoOrganizationCreate';
import { waitAndUpdate, click } from '../../../../helpers/testUtils';
import { bindAlmOrganization } from '../../../../api/alm-integration';

jest.mock('../../../../api/alm-integration', () => ({
  bindAlmOrganization: jest.fn().mockResolvedValue({})
}));

const organization = {
  avatar: 'http://example.com/avatar',
  description: 'description-foo',
  key: 'key-foo',
  name: 'name-foo',
  url: 'http://example.com/foo'
};

it('should render with import org button', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render prefilled and create org', async () => {
  const createOrganization = jest.fn().mockResolvedValue({ key: 'foo' });
  const onOrgCreated = jest.fn();
  const wrapper = shallowRender({
    almInstallId: 'id-foo',
    almOrganization: { ...organization, personal: false },
    createOrganization,
    onOrgCreated
  });

  expect(wrapper).toMatchSnapshot();

  wrapper.find('OrganizationDetailsForm').prop<Function>('onContinue')(organization);
  await waitAndUpdate(wrapper);

  expect(createOrganization).toBeCalledWith({ ...organization, installationId: 'id-foo' });
  expect(onOrgCreated).toBeCalledWith('foo');
});

it('should allow to cancel org import', () => {
  const updateUrlQuery = jest.fn().mockResolvedValue({ key: 'foo' });
  const wrapper = shallowRender({
    almInstallId: 'id-foo',
    almOrganization: { ...organization, personal: false },
    updateUrlQuery
  });

  click(wrapper.find('DeleteButton'));
  expect(updateUrlQuery).toBeCalledWith({ almInstallId: undefined, almKey: undefined });
});

it('should display choice between import or creation', () => {
  const wrapper = shallowRender({
    almInstallId: 'id-foo',
    almOrganization: { ...organization, personal: false },
    unboundOrganizations: [organization]
  });
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
  const wrapper = shallowRender({
    almInstallId: 'id-foo',
    almOrganization: { ...organization, personal: false },
    onOrgCreated,
    unboundOrganizations: [organization]
  });

  wrapper.find('RadioToggle').prop<Function>('onCheck')('bind');
  wrapper.update();
  wrapper.find('AutoOrganizationBind').prop<Function>('onBindOrganization')('foo');
  expect(bindAlmOrganization as jest.Mock<any>).toHaveBeenCalledWith({
    installationId: 'id-foo',
    organization: 'foo'
  });
  await waitAndUpdate(wrapper);
  expect(onOrgCreated).toHaveBeenCalledWith('foo', false);
});

function shallowRender(props: Partial<AutoOrganizationCreate['props']> = {}) {
  return shallow(
    <AutoOrganizationCreate
      almApplication={{
        backgroundColor: '#0052CC',
        iconPath: '"/static/authbitbucket/bitbucket.svg"',
        installationUrl: 'https://bitbucket.org/install/app',
        key: 'bitbucket',
        name: 'BitBucket'
      }}
      almUnboundApplications={[]}
      createOrganization={jest.fn()}
      onOrgCreated={jest.fn()}
      unboundOrganizations={[]}
      updateUrlQuery={jest.fn()}
      {...props}
    />
  );
}
