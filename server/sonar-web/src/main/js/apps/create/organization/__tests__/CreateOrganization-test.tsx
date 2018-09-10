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
import { mockRouter } from '../../../../helpers/testUtils';

it('should render and create organization', async () => {
  const createOrganization = jest.fn().mockResolvedValue({ key: 'foo' });
  const router = mockRouter();
  const wrapper = shallow(
    // @ts-ignore avoid passing everything from WithRouterProps
    <CreateOrganization createOrganization={createOrganization} router={router} />
  );
  expect(wrapper).toMatchSnapshot();

  const organization = {
    avatar: 'http://example.com/avatar',
    description: 'description-foo',
    key: 'key-foo',
    name: 'name-foo',
    url: 'http://example.com/foo'
  };
  wrapper.find('OrganizationDetailsStep').prop<Function>('onContinue')(organization);
  await new Promise(setImmediate);
  expect(createOrganization).toBeCalledWith(organization);
  expect(router.push).toBeCalledWith('/organizations/foo');
});
