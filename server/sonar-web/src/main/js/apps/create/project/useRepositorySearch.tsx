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
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AlmKeys } from '../../../types/alm-settings';
import { DopSetting } from '../../../types/dop-translation';
import { REPOSITORY_SEARCH_DEBOUNCE_TIME } from './constants';

export function useRepositorySearch(
  almKey: AlmKeys,
  fetchRepositories: (
    organizationKey?: string,
    query?: string,
    pageIndex?: number,
    more?: boolean,
  ) => Promise<void>,
  isInitialized: boolean,
  selectedDopSetting: DopSetting | undefined,
  selectedOrganizationKey: string | undefined,
  setSearchQuery: (query: string) => void,
  showPersonalAccessTokenForm = false,
) {
  const repositorySearchDebounceId = useRef<NodeJS.Timeout | undefined>();
  const [isSearching, setIsSearching] = useState<boolean>(false);

  const orgValid = useMemo(
    () =>
      almKey !== AlmKeys.GitHub ||
      (almKey === AlmKeys.GitHub && selectedOrganizationKey !== undefined),
    [almKey, selectedOrganizationKey],
  );

  useEffect(() => {
    if (selectedDopSetting && !showPersonalAccessTokenForm && orgValid) {
      if (almKey === AlmKeys.GitHub) {
        fetchRepositories(selectedOrganizationKey);
      } else if (!isInitialized) {
        fetchRepositories();
      }
    }
  }, [
    almKey,
    fetchRepositories,
    isInitialized,
    orgValid,
    selectedDopSetting,
    selectedOrganizationKey,
    showPersonalAccessTokenForm,
  ]);

  const onSearch = useCallback(
    (query: string) => {
      setSearchQuery(query);
      if (!isInitialized || !orgValid) {
        return;
      }

      clearTimeout(repositorySearchDebounceId.current);
      repositorySearchDebounceId.current = setTimeout(() => {
        setIsSearching(true);
        fetchRepositories(
          almKey === AlmKeys.GitHub ? selectedOrganizationKey : undefined,
          query,
        ).then(
          () => setIsSearching(false),
          () => setIsSearching(false),
        );
      }, REPOSITORY_SEARCH_DEBOUNCE_TIME);
    },
    [
      almKey,
      fetchRepositories,
      isInitialized,
      orgValid,
      repositorySearchDebounceId,
      selectedOrganizationKey,
      setIsSearching,
      setSearchQuery,
    ],
  );

  return {
    isSearching,
    onSearch,
  };
}
