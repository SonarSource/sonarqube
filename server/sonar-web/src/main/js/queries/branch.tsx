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

import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { debounce, flatten } from 'lodash';
import * as React from 'react';
import { useCallback, useContext } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import { isBranch, isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { searchParamsToQuery } from '~sonar-aligned/helpers/router';
import { LightComponent } from '~sonar-aligned/types/component';
import {
  deleteBranch,
  deletePullRequest,
  excludeBranchFromPurge,
  getBranches,
  getPullRequests,
  renameBranch,
  setMainBranch,
} from '../api/branches';
import { dismissAnalysisWarning, getAnalysisStatus } from '../api/ce';
import { getQualityGateProjectStatus } from '../api/quality-gates';
import { AvailableFeaturesContext } from '../app/components/available-features/AvailableFeaturesContext';
import { useComponent } from '../app/components/componentContext/withComponentContext';
import { extractStatusConditionsFromProjectStatus } from '../helpers/qualityGates';
import { isDefined } from '../helpers/types';
import { Branch, BranchLike } from '../types/branch-like';
import { isApplication, isProject } from '../types/component';
import { Feature } from '../types/features';
import { Component } from '../types/types';
import { StaleTime } from './common';

enum InnerState {
  Details = 'details',
  Warning = 'warning',
  Status = 'status',
}

/**
 * @deprecated This is a legacy way of organizing branch keys
 * It was introduced as a step to remove branch fetching from ComponentContainer.
 */
function useBranchesQueryKey(innerState: InnerState, componentKey?: string) {
  // Currently, we do not have the component in a react-state ready
  // Once we refactor we will be able to fetch it from query state.
  // We will be able to make sure that the component is not a portfolio.
  // Mixing query param and react-state is dangerous.
  // It should be avoided as much as possible.
  const { search } = useLocation();
  const searchParams = new URLSearchParams(search);

  if (!isDefined(componentKey)) {
    return ['branches'];
  }

  if (searchParams.has('pullRequest')) {
    return [
      'branches',
      componentKey,
      'pull-request',
      searchParams.get('pullRequest') as string,
      innerState,
    ] as const;
  }

  if (searchParams.has('branch')) {
    return [
      'branches',
      componentKey,
      'branch',
      searchParams.get('branch') as string,
      innerState,
    ] as const;
  }

  if (searchParams.has('fixedInPullRequest')) {
    return [
      'branches',
      componentKey,
      'fixedInPullRequest',
      searchParams.get('fixedInPullRequest') as string,
      innerState,
    ] as const;
  }

  return ['branches', componentKey, innerState] as const;
}

function branchesQuery(
  component: LightComponent | undefined,
  branchSupportFeatureEnabled: boolean,
) {
  return queryOptions({
    // we don't care about branchSupportFeatureEnabled in the key, as it never changes during a user session
    queryKey: ['branches', 'list', component?.key],
    queryFn: async ({ queryKey: [, , key] }) => {
      if (component === undefined || key === undefined || isPortfolioLike(component.qualifier)) {
        return [] as BranchLike[];
      }

      // Pull Requests exist only for projects and if [branch-support] is enabled
      const branchLikesPromise =
        isProject(component.qualifier) && branchSupportFeatureEnabled
          ? [getBranches(key), getPullRequests(key)]
          : [getBranches(key)];
      const branchLikes = await Promise.all(branchLikesPromise).then(flatten<BranchLike>);

      return branchLikes;
    },
    enabled: isDefined(component),
  });
}

/**
 * @deprecated This is a legacy way of organizing branch keys
 * It was introduce as a step to remove branch fetching from ComponentContainer.
 */
function useMutateBranchQueryKey() {
  const { search } = useLocation();
  const searchParams = new URLSearchParams(search);

  if (searchParams.has('id')) {
    return ['branches', searchParams.get('id') as string] as const;
  }
  return ['branches'];
}

function getContext(key: ReturnType<typeof useBranchesQueryKey>, branchLike?: BranchLike) {
  const [_b, componentKey, prOrBranch, branchKey] = key;
  if (prOrBranch === 'pull-request') {
    return { componentKey, query: { pullRequest: branchKey } };
  }
  if (prOrBranch === 'branch') {
    return { componentKey, query: { branch: branchKey } };
  }
  if (prOrBranch === 'fixedInPullRequest') {
    return { componentKey, query: { branch: (branchLike as Branch)?.name } };
  }
  return { componentKey, query: {} };
}

export function useBranchesQuery(component: LightComponent | undefined) {
  const features = useContext(AvailableFeaturesContext);

  return useQuery({
    ...branchesQuery(component, features.includes(Feature.BranchSupport)),
    initialData: [],
    staleTime: StaleTime.SHORT,
  });
}

export function useCurrentBranchQuery(component: LightComponent | undefined) {
  const features = useContext(AvailableFeaturesContext);
  const { search } = useLocation();

  const select = useCallback(
    (branchLikes: BranchLike[]) => {
      const searchParams = new URLSearchParams(search);
      if (searchParams.has('branch')) {
        return branchLikes.find((b) => isBranch(b) && b.name === searchParams.get('branch'));
      } else if (searchParams.has('pullRequest')) {
        return branchLikes.find(
          (b) => isPullRequest(b) && b.key === searchParams.get('pullRequest'),
        );
      } else if (searchParams.has('fixedInPullRequest')) {
        const targetBranch = branchLikes
          .filter(isPullRequest)
          .find((b) => b.key === searchParams.get('fixedInPullRequest'))?.target;
        return branchLikes.find((b) => isBranch(b) && b.name === targetBranch);
      }

      return branchLikes.find((b) => isBranch(b) && b.isMain);
    },
    [search],
  );

  return useQuery({
    ...branchesQuery(component, features.includes(Feature.BranchSupport)),
    select,
    staleTime: StaleTime.LIVE,
  });
}

export function useBranchStatusQuery(component: Component) {
  const { data: branchLike } = useCurrentBranchQuery(component);
  const key = useBranchesQueryKey(InnerState.Status, component.key);
  return useQuery({
    queryKey: key,
    queryFn: async ({ queryKey }) => {
      const { query } = getContext(queryKey, branchLike);
      if (!isProject(component.qualifier)) {
        return {};
      }
      const projectStatus = await getQualityGateProjectStatus({
        projectKey: component.key,
        ...query,
      }).catch(() => undefined);
      if (projectStatus === undefined) {
        return {};
      }

      const { ignoredConditions, status } = projectStatus;
      const conditions = extractStatusConditionsFromProjectStatus(projectStatus);
      return {
        conditions,
        ignoredConditions,
        status,
      };
    },
    enabled: isProject(component.qualifier) || isApplication(component.qualifier),
    staleTime: StaleTime.SHORT,
  });
}

export function useBranchWarningQuery(component: Component) {
  const { data: branchLike } = useCurrentBranchQuery(component);
  const key = useBranchesQueryKey(InnerState.Warning, component.key);
  return useQuery({
    queryKey: key,
    queryFn: async ({ queryKey }) => {
      const { query, componentKey } = getContext(queryKey, branchLike);
      const { component: branchStatus } = await getAnalysisStatus({
        component: componentKey,
        ...query,
      });
      return branchStatus.warnings;
    },
    enabled: !!branchLike && isProject(component.qualifier) && component.key === key[1],
    staleTime: StaleTime.SHORT,
  });
}

export function useDismissBranchWarningMutation(componentKey: string | undefined) {
  type DismissArg = { component: Component; key: string };
  const queryClient = useQueryClient();
  const invalidateKey = useBranchesQueryKey(InnerState.Warning, componentKey);

  return useMutation({
    mutationFn: async ({ component, key }: DismissArg) => {
      await dismissAnalysisWarning(component.key, key);
    },
    onSuccess(_1, { component }) {
      queryClient.invalidateQueries({ queryKey: ['branches', 'list', component.key] });
      queryClient.invalidateQueries({ queryKey: invalidateKey });
    },
  });
}

export function useExcludeFromPurgeMutation() {
  const queryClient = useQueryClient();
  const invalidateKey = useMutateBranchQueryKey();

  type ExcludeFromPurgeArg = { component: Component; exclude: boolean; key: string };

  return useMutation({
    mutationFn: async ({ component, key, exclude }: ExcludeFromPurgeArg) => {
      await excludeBranchFromPurge(component.key, key, exclude);
    },
    onSuccess(_1, { component }) {
      queryClient.invalidateQueries({ queryKey: ['branches', 'list', component.key] });
      queryClient.invalidateQueries({ queryKey: invalidateKey });
    },
  });
}

export function useDeletBranchMutation() {
  type DeleteArg = { branchLike: BranchLike; component: Component };
  const queryClient = useQueryClient();
  const [params, setSearhParam] = useSearchParams();
  const invalidateKey = useMutateBranchQueryKey();

  return useMutation({
    mutationFn: async ({ branchLike, component }: DeleteArg) => {
      await (isPullRequest(branchLike)
        ? deletePullRequest({
            project: component.key,
            pullRequest: branchLike.key,
          })
        : deleteBranch({
            branch: branchLike.name,
            project: component.key,
          }));

      if (
        isBranch(branchLike) &&
        params.has('branch') &&
        params.get('branch') === branchLike.name
      ) {
        setSearhParam(searchParamsToQuery(params, ['branch']));
        return { navigate: true };
      }

      if (
        isPullRequest(branchLike) &&
        params.has('pullRequest') &&
        params.get('pullRequest') === branchLike.key
      ) {
        setSearhParam(searchParamsToQuery(params, ['pullRequest']));
        return { navigate: true };
      }
      return { navigate: false };
    },
    onSuccess({ navigate }, { component }) {
      if (!navigate) {
        queryClient.invalidateQueries({ queryKey: ['branches', 'list', component.key] });
        queryClient.invalidateQueries({ queryKey: invalidateKey });
      }
    },
  });
}

export function useRenameMainBranchMutation() {
  type RenameMainBranchArg = { component: Component; name: string };
  const queryClient = useQueryClient();
  const invalidateKey = useMutateBranchQueryKey();

  return useMutation({
    mutationFn: async ({ component, name }: RenameMainBranchArg) => {
      await renameBranch(component.key, name);
    },
    onSuccess(_1, { component }) {
      queryClient.invalidateQueries({ queryKey: ['branches', 'list', component.key] });
      queryClient.invalidateQueries({ queryKey: invalidateKey });
    },
  });
}

export function useSetMainBranchMutation() {
  type SetAsMainBranchArg = { branchName: string; component: Component };
  const queryClient = useQueryClient();
  const invalidateKey = useMutateBranchQueryKey();

  return useMutation({
    mutationFn: async ({ component, branchName }: SetAsMainBranchArg) => {
      await setMainBranch(component.key, branchName);
    },
    onSuccess(_1, { component }) {
      queryClient.invalidateQueries({ queryKey: ['branches', 'list', component.key] });
      queryClient.invalidateQueries({ queryKey: invalidateKey });
    },
  });
}

/**
 * Helper functions that sould be avoided. Instead convert the component into a functional one
 * and/or use proper react-query
 */
const REFRESH_INTERVAL = 1_000;

export function useRefreshBranchStatus(componentKey: string | undefined): () => void {
  const queryClient = useQueryClient();
  const invalidateStatusKey = useBranchesQueryKey(InnerState.Status, componentKey);
  const invalidateDetailsKey = useBranchesQueryKey(InnerState.Details, componentKey);

  return useCallback(
    debounce(() => {
      queryClient.invalidateQueries({ queryKey: ['branches', 'list', componentKey] });
      queryClient.invalidateQueries({
        queryKey: invalidateStatusKey,
      });
      queryClient.invalidateQueries({
        queryKey: invalidateDetailsKey,
      });
    }, REFRESH_INTERVAL),
    [invalidateDetailsKey, invalidateStatusKey],
  );
}

export function useRefreshBranches(componentKey: string | undefined) {
  const queryClient = useQueryClient();
  const invalidateKey = useMutateBranchQueryKey();

  return () => {
    queryClient.invalidateQueries({ queryKey: ['branches', 'list', componentKey] });
    queryClient.invalidateQueries({ queryKey: invalidateKey });
  };
}

export interface WithBranchLikesProps {
  branchLike?: BranchLike;
  branchLikes?: BranchLike[];
  isFetchingBranch?: boolean;
}

export function withBranchLikes<P extends { component?: Component }>(
  WrappedComponent: React.ComponentType<React.PropsWithChildren<P & WithBranchLikesProps>>,
): React.ComponentType<React.PropsWithChildren<Omit<P, 'branchLike' | 'branchLikes'>>> {
  return function WithBranchLike(p: P) {
    const { data: branchLikes, isLoading } = useBranchesQuery(p.component);
    const { data: branchLike, isFetching } = useCurrentBranchQuery(p.component);
    return (
      <WrappedComponent
        branchLikes={branchLikes ?? []}
        branchLike={branchLike}
        isFetchingBranch={!isPortfolioLike(p.component?.qualifier) && (isFetching || isLoading)}
        {...p}
      />
    );
  };
}

export function withBranchStatusRefresh<
  P extends { refreshBranchStatus: ReturnType<typeof useRefreshBranchStatus> },
>(
  WrappedComponent: React.ComponentType<React.PropsWithChildren<P>>,
): React.ComponentType<React.PropsWithChildren<Omit<P, 'refreshBranchStatus'>>> {
  return function WithBranchStatusRefresh(props: P) {
    const { component } = useComponent();
    const refresh = useRefreshBranchStatus(component?.key);

    return <WrappedComponent {...props} refreshBranchStatus={refresh} />;
  };
}
