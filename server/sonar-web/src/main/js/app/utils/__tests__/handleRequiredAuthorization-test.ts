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
import handleRequiredAuthorization from '../handleRequiredAuthorization';

const originalLocation = window.location;

const replace = jest.fn();

beforeAll(() => {
  const location = {
    ...window.location,
    pathname: '/path',
    search: '?id=12',
    hash: '#tag',
    replace,
  };
  Object.defineProperty(window, 'location', {
    writable: true,
    value: location,
  });
});

afterAll(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation,
  });
});

it('should not render for anonymous user', () => {
  handleRequiredAuthorization();
  expect(replace).toHaveBeenCalledWith(
    '/sessions/new?return_to=%2Fpath%3Fid%3D12%23tag&authorizationError=true'
  );
});
