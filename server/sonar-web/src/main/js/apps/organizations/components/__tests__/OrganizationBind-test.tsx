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
import * as React from 'react';
import { shallow } from 'enzyme';
import OrganizationBind from '../OrganizationBind';
import {
  BIND_ORGANIZATION_REDIRECT_TO_ORG_TIMESTAMP,
  BIND_ORGANIZATION_KEY
} from '../../../create/organization/utils';
import { getAlmAppInfo } from '../../../../api/alm-integration';
import { save } from '../../../../helpers/storage';
import {
  mockAlmApplication,
  mockLoggedInUser,
  mockOrganization
} from '../../../../helpers/testMocks';

jest.mock('../../../../api/alm-integration', () => ({
  getAlmAppInfo: jest.fn(() => Promise.resolve({ application: mockAlmApplication() }))
}));
jest.mock('../../../../helpers/storage', () => ({
  save: jest.fn()
}));

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

beforeEach(() => {
  (getAlmAppInfo as jest.Mock<any>).mockClear();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should save state when handling Install App click', () => {
  const orgKey = '56346';
  shallowRender({ organization: mockOrganization({ key: orgKey }) })
    .instance()
    .handleInstallAppClick();

  expect(save).toBeCalledTimes(2);
  expect(save).nthCalledWith(1, BIND_ORGANIZATION_KEY, orgKey);
  const secondCallArguments = (save as jest.Mock<any>).mock.calls[1];
  expect(secondCallArguments[0]).toBe(BIND_ORGANIZATION_REDIRECT_TO_ORG_TIMESTAMP);
});

function shallowRender(props: Partial<OrganizationBind['props']> = {}) {
  return shallow<OrganizationBind>(
    <OrganizationBind
      currentUser={mockLoggedInUser()}
      organization={mockOrganization()}
      {...props}
    />
  );
}
