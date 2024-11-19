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

import { UseQueryResult } from '@tanstack/react-query';
import { Paging } from '../types/types';

const notUndefined = <T>(x: T | undefined): x is T => x !== undefined;

export const mapReactQueryResult = <T, R>(
  res: UseQueryResult<T>,
  mapper: (data: T) => R,
): UseQueryResult<R> => {
  return {
    ...res,
    refetch: (...args: Parameters<typeof res.refetch>) =>
      res.refetch(...args).then((val) => mapReactQueryResult(val, mapper)),
    data: notUndefined(res.data) ? mapper(res.data) : res.data,
  } as UseQueryResult<R>;
};

export const getNextPageParam = <T extends { page: Paging }>(params: T) =>
  params.page.total <= params.page.pageIndex * params.page.pageSize
    ? undefined
    : params.page.pageIndex + 1;

export const getPreviousPageParam = <T extends { page: Paging }>(params: T) =>
  params.page.pageIndex === 1 ? undefined : params.page.pageIndex - 1;

export const getNextPagingParam = <T extends { paging: Paging }>(params: T) =>
  params.paging.total <= params.paging.pageIndex * params.paging.pageSize
    ? undefined
    : params.paging.pageIndex + 1;

export const getPreviousPagingParam = <T extends { paging: Paging }>(params: T) =>
  params.paging.pageIndex === 1 ? undefined : params.paging.pageIndex - 1;
