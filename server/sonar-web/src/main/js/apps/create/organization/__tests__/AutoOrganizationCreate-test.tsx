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
import { waitAndUpdate } from '../../../../helpers/testUtils';

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
    almOrganization: {
      ...organization,
      type: 'ORGANIZATION'
    },
    createOrganization,
    onOrgCreated
  });

  expect(wrapper).toMatchSnapshot();

  wrapper.find('OrganizationDetailsStep').prop<Function>('onContinue')(organization);
  await waitAndUpdate(wrapper);

  expect(createOrganization).toBeCalledWith({ ...organization, installId: 'id-foo' });
  expect(onOrgCreated).toBeCalledWith('foo');
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
      createOrganization={jest.fn()}
      onOrgCreated={jest.fn()}
      {...props}
    />
  );
}
