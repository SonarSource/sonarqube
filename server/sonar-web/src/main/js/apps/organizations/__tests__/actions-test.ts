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
import { mockOrganization } from '../../../helpers/testMocks';
import { deleteOrganization, updateOrganization } from '../../../api/organizations';

jest.mock('../../../api/organizations', () => ({
  deleteOrganization: jest.fn().mockResolvedValue({}),
  updateOrganization: jest.fn().mockResolvedValue({})
}));

const dispatch = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
});

describe('#updateOrganization', () => {
  it('should update and dispatch', async () => {
    const org = mockOrganization();
    const { key, ...changes } = org;
    const promise = actions.updateOrganization(key, changes)(dispatch);

    expect(updateOrganization).toHaveBeenCalledWith(key, changes);
    await promise;
    expect(dispatch).toHaveBeenCalledWith({ changes, key, type: 'UPDATE_ORGANIZATION' });
  });
});

describe('#deleteOrganization', () => {
  it('should delete and dispatch', async () => {
    const key = 'foo';
    const promise = actions.deleteOrganization(key)(dispatch);

    expect(deleteOrganization).toHaveBeenCalledWith(key);
    await promise;
    expect(dispatch).toHaveBeenCalledWith({ key, type: 'DELETE_ORGANIZATION' });
  });
});
