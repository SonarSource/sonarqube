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

import { QueryClient } from '@tanstack/react-query';
import { queryClient } from '../queryClient';

jest.mock('@tanstack/react-query');

it('should return the queryClient and not retry on 4xx errors', () => {
  expect(queryClient).toBeDefined();

  expect(jest.mocked(QueryClient)).toHaveBeenCalledWith({
    defaultOptions: { queries: { retry: expect.any(Function) } },
  });

  const retryFunction = jest.mocked(QueryClient).mock.calls[0][0]?.defaultOptions?.queries
    ?.retry as Function;

  expect(retryFunction(0, undefined)).toEqual(true);
  expect(retryFunction(1, undefined)).toEqual(true);
  expect(retryFunction(2, undefined)).toEqual(false);

  expect(retryFunction(0, null)).toEqual(true);
  expect(retryFunction(0, {})).toEqual(true);
  expect(retryFunction(0, { status: 200 })).toEqual(true);

  expect(retryFunction(0, { status: 400 })).toEqual(false);
  expect(retryFunction(0, { status: 404 })).toEqual(false);
  expect(retryFunction(0, { status: 500 })).toEqual(true);
});
