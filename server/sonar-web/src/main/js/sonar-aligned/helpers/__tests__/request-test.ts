/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { setImmediate } from 'timers';
import { HttpStatus } from '../../../helpers/request';
import { Dict } from '../../../types/types';
import { getJSON } from '../request';

const url = '/my-url';

beforeEach(() => {
  jest.clearAllMocks();
  window.fetch = jest.fn().mockResolvedValue(mockResponse({}, HttpStatus.Ok, {}));
});

describe('getJSON', () => {
  it('should get json without parameters', async () => {
    const response = mockResponse({}, HttpStatus.Ok, {});
    window.fetch = jest.fn().mockResolvedValue(response);
    getJSON(url);
    await new Promise(setImmediate);

    expect(window.fetch).toHaveBeenCalledWith(url, expect.objectContaining({ method: 'GET' }));
    expect(response.json).toHaveBeenCalled();
  });

  it('should get json with parameters', () => {
    getJSON(url, { data: 'test' });
    expect(window.fetch).toHaveBeenCalledWith(
      url + '?data=test',
      expect.objectContaining({ method: 'GET' }),
    );
  });
});

function mockResponse(headers: Dict<string> = {}, status = HttpStatus.Ok, value?: any): Response {
  const body = value && value instanceof Object ? JSON.stringify(value) : value;
  const response = new Response(body, { headers, status });
  response.json = jest.fn().mockResolvedValue(value);
  response.text = jest.fn().mockResolvedValue(value);
  return response;
}
