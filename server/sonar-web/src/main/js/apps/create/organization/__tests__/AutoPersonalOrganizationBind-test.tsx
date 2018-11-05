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
import AutoPersonalOrganizationBind from '../AutoPersonalOrganizationBind';
import { waitAndUpdate, click } from '../../../../helpers/testUtils';

const personalOrg = { key: 'personalorg', name: 'Personal Org' };

it('should render correctly', async () => {
  const updateOrganization = jest.fn().mockResolvedValue({ key: personalOrg.key });
  const onOrgCreated = jest.fn();
  const wrapper = shallowRender({
    almInstallId: 'id-foo',
    importPersonalOrg: personalOrg,
    onOrgCreated,
    updateOrganization
  });

  expect(wrapper).toMatchSnapshot();

  wrapper.find('OrganizationDetailsForm').prop<Function>('onContinue')(personalOrg);
  await waitAndUpdate(wrapper);

  expect(updateOrganization).toBeCalledWith({ ...personalOrg, installationId: 'id-foo' });
  expect(onOrgCreated).toBeCalledWith(personalOrg.key);
});

it('should allow to cancel org import', () => {
  const updateUrlQuery = jest.fn();
  const wrapper = shallowRender({
    almInstallId: 'id-foo',
    importPersonalOrg: personalOrg,
    updateUrlQuery
  });

  click(wrapper.find('DeleteButton'));
  expect(updateUrlQuery).toBeCalledWith({ almInstallId: undefined, almKey: undefined });
});

function shallowRender(props: Partial<AutoPersonalOrganizationBind['props']> = {}) {
  return shallow(
    <AutoPersonalOrganizationBind
      almApplication={{
        backgroundColor: '#0052CC',
        iconPath: '"/static/authbitbucket/bitbucket.svg"',
        installationUrl: 'https://bitbucket.org/install/app',
        key: 'bitbucket',
        name: 'BitBucket'
      }}
      almOrganization={{
        avatar: 'http://example.com/avatar',
        description: 'description-foo',
        key: 'key-foo',
        name: 'name-foo',
        personal: true,
        url: 'http://example.com/foo'
      }}
      importPersonalOrg={{ key: 'personalorg', name: 'Personal Org' }}
      onOrgCreated={jest.fn()}
      updateOrganization={jest.fn()}
      updateUrlQuery={jest.fn()}
      {...props}
    />
  );
}
