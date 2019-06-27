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
import { createOrganization, syncMembers } from '../../../../api/organizations';
import { mockOrganization, mockOrganizationWithAlm } from '../../../../helpers/testMocks';
import * as actions from '../actions';

jest.mock('../../../../api/alm-integration', () => ({
  bindAlmOrganization: jest.fn().mockResolvedValue({})
}));

jest.mock('../../../../api/organizations', () => ({
  createOrganization: jest.fn().mockResolvedValue({ key: 'foo', name: 'Foo' }),
  updateOrganization: jest.fn().mockResolvedValue({}),
  syncMembers: jest.fn()
}));

const dispatch = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
});

describe('#createOrganization', () => {
  it('should create and return an org key', async () => {
    const org = mockOrganization();
    const promise = actions.createOrganization(org)(dispatch);

    expect(createOrganization).toHaveBeenCalledWith(org);
    const returnValue = await promise;
    expect(dispatch).toHaveBeenCalledWith({ organization: org, type: 'CREATE_ORGANIZATION' });
    expect(syncMembers).not.toBeCalled();
    expect(returnValue).toBe(org.key);
  });

  it('should create and sync members', async () => {
    const { alm, ...org } = mockOrganizationWithAlm(
      {},
      { key: 'github', membersSync: true, url: 'https://github.com/foo' }
    );

    (createOrganization as jest.Mock).mockResolvedValueOnce(org);
    const promise = actions.createOrganization({ alm, ...org })(dispatch);

    expect(createOrganization).toHaveBeenCalledWith(org);
    await promise;
    expect(syncMembers).toHaveBeenCalledWith(org.key);
  });

  it('should not sync members for personal Github orgs', async () => {
    const { alm, ...org } = mockOrganizationWithAlm(
      {},
      { key: 'github', membersSync: true, personal: true, url: 'https://github.com/foo' }
    );

    (createOrganization as jest.Mock).mockResolvedValueOnce(org);
    const promise = actions.createOrganization({ alm, ...org })(dispatch);

    expect(createOrganization).toHaveBeenCalledWith(org);
    await promise;
    expect(syncMembers).not.toBeCalled();
  });
});
