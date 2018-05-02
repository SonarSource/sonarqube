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
import { OrganizationDelete } from '../OrganizationDelete';
import { getOrganizationBilling } from '../../../../api/organizations';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/organizations', () => ({
  getOrganizationBilling: jest.fn(() =>
    Promise.resolve({ nclocCount: 1000, subscription: { status: 'active', trial: true } })
  )
}));

beforeEach(() => {
  (getOrganizationBilling as jest.Mock<any>).mockClear();
});

it('smoke test', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should redirect the page', async () => {
  const deleteOrganization = jest.fn(() => Promise.resolve());
  const replace = jest.fn();
  const wrapper = getWrapper({ deleteOrganization }, { router: { replace } });
  (wrapper.instance() as OrganizationDelete).onDelete();
  await waitAndUpdate(wrapper);
  expect(deleteOrganization).toHaveBeenCalledWith('foo');
  expect(replace).toHaveBeenCalledWith('/');
});

it('should show a info message for paying organization', async () => {
  const wrapper = getWrapper({}, { onSonarCloud: true });
  await waitAndUpdate(wrapper);
  expect(getOrganizationBilling).toHaveBeenCalledWith('foo');
  expect(wrapper).toMatchSnapshot();
});

function getWrapper(props = {}, context = {}) {
  return shallow(
    <OrganizationDelete
      deleteOrganization={jest.fn(() => Promise.resolve())}
      organization={{ key: 'foo', name: 'Foo' }}
      {...props}
    />,

    { context: { router: { replace: jest.fn() }, ...context } }
  );
}
