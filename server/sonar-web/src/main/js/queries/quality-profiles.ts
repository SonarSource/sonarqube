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
  infiniteQueryOptions,
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import {
  ActivateRuleParameters,
  AddRemoveGroupParameters,
  AddRemoveUserParameters,
  DeactivateRuleParameters,
  Profile,
  activateRule,
  addGroup,
  addUser,
  compareProfiles,
  deactivateRule,
  getProfileChangelog,
  getProfileInheritance,
  getQualityProfile,
} from '../api/quality-profiles';
import { getNextPageParam, getPreviousPageParam } from '../helpers/react-query';
import { isDefined } from '../helpers/types';
import { QualityProfileChangelogFilterMode } from '../types/quality-profiles';
import { createInfiniteQueryHook, createQueryHook } from './common';

const qualityProfileQueryKeys = {
  all: () => ['quality-profiles'],
  inheritance: (language?: string, name?: string, parentKey?: string) => [
    ...qualityProfileQueryKeys.all(),
    'inheritance',
    language,
    name,
    parentKey,
  ],
  profile: (profile: Profile, compareToSonarWay = false) => [
    ...qualityProfileQueryKeys.all(),
    'details',
    profile,
    compareToSonarWay,
  ],
  changelog: (
    language: string,
    name: string,
    since: string,
    to: string,
    filterMode: QualityProfileChangelogFilterMode,
  ) => [...qualityProfileQueryKeys.all(), 'changelog', language, name, since, to, filterMode],
  compare: (leftKey: string, rightKey: string) => [
    ...qualityProfileQueryKeys.all(),
    'compare',
    leftKey,
    rightKey,
  ],
};

export const useProfileInheritanceQuery = createQueryHook(
  (profile?: Pick<Profile, 'language' | 'name' | 'parentKey'>) => {
    const { language, name, parentKey } = profile ?? {};
    return queryOptions({
      queryKey: qualityProfileQueryKeys.inheritance(language, name, parentKey),
      queryFn: () => {
        if (!isDefined(language) || !isDefined(name)) {
          return { ancestors: [], children: [], profile: null };
        }
        return getProfileInheritance({ language, name });
      },
    });
  },
);

export const useGetQualityProfile = createQueryHook(
  (data: Parameters<typeof getQualityProfile>[0]) => {
    return queryOptions({
      queryKey: qualityProfileQueryKeys.profile(data.profile, data.compareToSonarWay),
      queryFn: () => {
        return getQualityProfile(data);
      },
    });
  },
);

export const useGetQualityProfileChangelog = createInfiniteQueryHook(
  (data: Parameters<typeof getProfileChangelog>[0]) => {
    return infiniteQueryOptions({
      queryKey: qualityProfileQueryKeys.changelog(
        data.profile.language,
        data.profile.name,
        data.since,
        data.to,
        data.filterMode,
      ),
      queryFn: ({ pageParam }) => {
        return getProfileChangelog({ ...data, page: pageParam });
      },
      getNextPageParam: (data) => getNextPageParam({ page: data.paging }),
      getPreviousPageParam: (data) => getPreviousPageParam({ page: data.paging }),
      initialPageParam: 1,
    });
  },
);

export function useProfilesCompareQuery(leftKey: string, rightKey: string) {
  return useQuery({
    queryKey: qualityProfileQueryKeys.compare(leftKey, rightKey),
    queryFn: ({ queryKey: [_1, _2, leftKey, rightKey] }) => {
      if (!isDefined(leftKey) || !isDefined(rightKey)) {
        return null;
      }

      return compareProfiles(leftKey, rightKey);
    },
  });
}

export function useActivateRuleMutation(onSuccess: (data: ActivateRuleParameters) => unknown) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: activateRule,
    onSuccess: (_, data) => {
      queryClient.invalidateQueries({ queryKey: ['rules', 'details', data.rule] });
      onSuccess(data);
    },
  });
}

export function useDeactivateRuleMutation(onSuccess: (data: DeactivateRuleParameters) => unknown) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deactivateRule,
    onSuccess: (_, data) => {
      queryClient.invalidateQueries({ queryKey: ['rules', 'details', data.rule] });
      onSuccess(data);
    },
  });
}

export function useAddUserMutation(onSuccess: () => unknown) {
  return useMutation({
    mutationFn: (data: AddRemoveUserParameters) => addUser(data),
    onSuccess,
  });
}

export function useAddGroupMutation(onSuccess: () => unknown) {
  return useMutation({
    mutationFn: (data: AddRemoveGroupParameters) => addGroup(data),
    onSuccess,
  });
}
