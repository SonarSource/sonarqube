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
import { getJSON } from 'sonar-ui-common/helpers/request';
import { getAlmOrganization } from '../alm-integration';

jest.useFakeTimers();
jest.mock('sonar-ui-common/helpers/request', () => ({
  ...jest.requireActual('sonar-ui-common/helpers/request'),
  getJSON: jest.fn()
}));
jest.mock('../../app/utils/throwGlobalError', () => ({
  default: jest.fn().mockImplementation(r => Promise.reject(r))
}));

beforeEach(() => {
  jest.clearAllTimers();
  jest.clearAllMocks();
});

describe('getAlmOrganization', () => {
  it('should return the organization', () => {
    const response = { almOrganization: { key: 'foo', name: 'Foo' } };
    (getJSON as jest.Mock).mockResolvedValue(response);
    return expect(getAlmOrganization({ installationId: 'foo' })).resolves.toEqual(response);
  });

  it('should reject with an error', () => {
    const error = { status: 401 };
    (getJSON as jest.Mock).mockRejectedValue(error);
    return expect(getAlmOrganization({ installationId: 'foo' })).rejects.toEqual(error);
  });

  it('should try until getting the organization', async () => {
    (getJSON as jest.Mock).mockRejectedValue({ status: 404 });
    const spy = jest.fn();
    getAlmOrganization({ installationId: 'foo' }).then(spy);
    for (let i = 1; i < 5; i++) {
      expect(getJSON).toBeCalledTimes(i);
      expect(spy).not.toBeCalled();
      await new Promise(setImmediate);
      jest.runAllTimers();
    }
    expect(getJSON).toBeCalledTimes(5);
    expect(spy).not.toBeCalled();

    const response = { almOrganization: { key: 'foo', name: 'Foo' } };
    (getJSON as jest.Mock).mockResolvedValue(response);
    await new Promise(setImmediate);
    jest.runAllTimers();
    expect(getJSON).toBeCalledTimes(6);
    await new Promise(setImmediate);
    expect(spy).toBeCalledWith(response);
  });
});
