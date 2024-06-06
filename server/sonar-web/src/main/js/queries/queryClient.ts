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

import { QueryClient, QueryOptions } from '@tanstack/react-query';

export const queryClientDefaultRetryFn: QueryOptions['retry'] = (failureCount, error) => {
  if (typeof error === 'object' && error !== null && 'status' in error) {
    const { status } = error as unknown as Response;

    if (status >= 400 && status < 500) {
      // no point in retrying on 4xx errors
      return false;
    }
  }

  return failureCount < 2;
};

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: queryClientDefaultRetryFn,
    },
  },
});
