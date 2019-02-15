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
import * as actions from '../actions';
import { mockOrganization, mockOrganizationWithAlm } from '../../../../helpers/testMocks';
import { createOrganization, syncMembers, updateOrganization } from '../../../../api/organizations';
import { bindAlmOrganization } from '../../../../api/alm-integration';

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
});

describe('#updateOrganization', () => {
  it('should update and dispatch', async () => {
    const org = mockOrganization();
    const { key, ...changes } = org;
    const promise = actions.updateOrganization(org)(dispatch);

    expect(updateOrganization).toHaveBeenCalledWith(key, changes);
    const returnValue = await promise;
    expect(dispatch).toHaveBeenCalledWith({ changes, key, type: 'UPDATE_ORGANIZATION' });
    expect(returnValue).toBe(key);
  });

  it('should update and bind', () => {
    const org = { ...mockOrganization(), installationId: '1' };
    const { key, installationId, ...changes } = org;
    const promise = actions.updateOrganization(org)(dispatch);

    expect(updateOrganization).toHaveBeenCalledWith(key, changes);
    expect(bindAlmOrganization).toHaveBeenCalledWith({ organization: key, installationId });
    return promise;
  });
});
