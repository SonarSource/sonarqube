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

import {
  InfiniteData,
  QueryKey,
  UseInfiniteQueryOptions,
  UseInfiniteQueryResult,
  UseQueryOptions,
  UseQueryResult,
  useInfiniteQuery,
  useQuery,
} from '@tanstack/react-query';

const isFnWithoutParams = <T>(fn: ((data: any) => T) | (() => T)): fn is () => T => fn.length === 0;

export function createQueryHook<
  T = unknown,
  TQueryData = unknown,
  TError extends Error = Error,
  TData = TQueryData,
  TQueryKey extends QueryKey = QueryKey,
>(
  fn:
    | ((data: T) => UseQueryOptions<TQueryData, TError, TData, TQueryKey>)
    | (() => UseQueryOptions<TQueryData, TError, TData, TQueryKey>),
): unknown extends T
  ? <SelectType = TQueryData>(
      options?: Omit<
        UseQueryOptions<TQueryData, TError, SelectType, TQueryKey>,
        'queryKey' | 'queryFn'
      >,
    ) => UseQueryResult<SelectType, TError>
  : <SelectType = TQueryData>(
      data: T,
      options?: Omit<
        UseQueryOptions<TQueryData, TError, SelectType, TQueryKey>,
        'queryKey' | 'queryFn'
      >,
    ) => UseQueryResult<SelectType, TError>;

export function createQueryHook(fn: ((data: any) => UseQueryOptions) | (() => UseQueryOptions)) {
  if (isFnWithoutParams(fn)) {
    return (options?: Omit<UseQueryOptions, 'queryKey' | 'queryFn'>) =>
      useQuery({ ...fn(), ...options });
  }
  return (data: any, options?: Omit<UseQueryOptions, 'queryKey' | 'queryFn'>) =>
    useQuery({ ...fn(data), ...options });
}

export function createInfiniteQueryHook<
  T = unknown,
  TQueryFnData = unknown,
  TError = Error,
  TData = InfiniteData<TQueryFnData>,
  TQueryData = TQueryFnData,
  TQueryKey extends QueryKey = QueryKey,
  TPageParam = unknown,
>(
  fn:
    | ((
        data: T,
      ) => UseInfiniteQueryOptions<TQueryFnData, TError, TData, TQueryData, TQueryKey, TPageParam>)
    | (() => UseInfiniteQueryOptions<
        TQueryFnData,
        TError,
        TData,
        TQueryData,
        TQueryKey,
        TPageParam
      >),
): unknown extends T
  ? <SelectType = TData>(
      options?: Omit<
        UseInfiniteQueryOptions<
          TQueryFnData,
          TError,
          SelectType,
          TQueryData,
          TQueryKey,
          TPageParam
        >,
        'queryKey' | 'queryFn' | 'getNextPageParam' | 'getPreviousPageParam' | 'initialPageParam'
      >,
    ) => UseInfiniteQueryResult<SelectType, TError>
  : <SelectType = TData>(
      data: T,
      options?: Omit<
        UseInfiniteQueryOptions<
          TQueryFnData,
          TError,
          SelectType,
          TQueryData,
          TQueryKey,
          TPageParam
        >,
        'queryKey' | 'queryFn' | 'getNextPageParam' | 'getPreviousPageParam' | 'initialPageParam'
      >,
    ) => UseInfiniteQueryResult<SelectType, TError>;

export function createInfiniteQueryHook(
  fn: ((data?: any) => UseInfiniteQueryOptions) | (() => UseInfiniteQueryOptions),
) {
  if (isFnWithoutParams(fn)) {
    return (
      options?: Omit<
        UseInfiniteQueryOptions,
        'queryKey' | 'queryFn' | 'getNextPageParam' | 'getPreviousPageParam' | 'initialPageParam'
      >,
    ) => useInfiniteQuery({ ...fn(), ...options });
  }
  return (
    data: any,
    options?: Omit<
      UseInfiniteQueryOptions,
      'queryKey' | 'queryFn' | 'getNextPageParam' | 'getPreviousPageParam' | 'initialPageParam'
    >,
  ) => useInfiniteQuery({ ...fn(data), ...options });
}

export enum StaleTime {
  /** Use it when the data doesn't change during the user's session or the data doesn't need to be update-to-date in the UI. */
  NEVER = Infinity,
  /** Use it when the data can change at any time because of user interactions or background tasks, and it's critical to reflect it live in the UI. */
  LIVE = 0,
  /** Use it when the data changes often and you want to be able to see it refreshed quickly but it's critical to see it live. */
  SHORT = 10000,
  /** Use it when the data rarely changes, anything bigger than 60s doesn't change much in term of network load or UX. */
  LONG = 60000,
  /** Use it for ambiguous cases where you can't decide between {@link StaleTime.SHORT} or {@link StaleTime.LONG}. It should rarely be used. */
  MEDIUM = 30000,
}
