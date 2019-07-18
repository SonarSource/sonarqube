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
import { getOrganizationBilling } from '../../../../api/organizations';
import { isSonarCloud } from '../../../../helpers/system';
import { OrganizationDelete } from '../OrganizationDelete';

jest.mock('../../../../api/organizations', () => ({
  getOrganizationBilling: jest.fn(() =>
    Promise.resolve({ nclocCount: 1000, subscription: { status: 'active', trial: true } })
  )
}));

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

beforeEach(() => {
  (getOrganizationBilling as jest.Mock<any>).mockClear();
});

it('smoke test', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should redirect the page', async () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => false);
  const deleteOrganization = jest.fn(() => Promise.resolve());
  const replace = jest.fn();
  const wrapper = getWrapper({ deleteOrganization, router: { replace } });
  (wrapper.instance() as OrganizationDelete).onDelete();
  await waitAndUpdate(wrapper);
  expect(deleteOrganization).toHaveBeenCalledWith('foo');
  expect(replace).toHaveBeenCalledWith('/');
});

it('should show a info message for paying organization', async () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  const wrapper = getWrapper({});
  await waitAndUpdate(wrapper);
  expect(getOrganizationBilling).toHaveBeenCalledWith('foo');
  expect(wrapper).toMatchSnapshot();
});

function getWrapper(props: Partial<OrganizationDelete['props']> = {}) {
  return shallow(
    <OrganizationDelete
      deleteOrganization={jest.fn(() => Promise.resolve())}
      organization={{ key: 'foo', name: 'Foo' }}
      router={{ replace: jest.fn() }}
      {...props}
    />
  );
}
