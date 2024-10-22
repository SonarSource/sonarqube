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

import { isEmpty } from 'lodash';
import { useCallback, useEffect, useRef, useState } from 'react';
import { AzureRepository, BitbucketRepository } from '../../../types/alm-integration';
import { DopSetting } from '../../../types/dop-translation';
import { REPOSITORY_SEARCH_DEBOUNCE_TIME } from './constants';

type RepoTypes = AzureRepository | BitbucketRepository;

export function useProjectRepositorySearch<RepoType extends RepoTypes>({
  defaultRepositorySelect,
  fetchData,
  fetchSearchResults,
  getRepositoryKey,
  isMonorepoSetup,
  selectedDopSetting,
  setSearchQuery,
  setSelectedRepository,
  setShowPersonalAccessTokenForm,
}: {
  defaultRepositorySelect: (repositoryKey: string) => void;
  fetchData: () => void;
  fetchSearchResults: (query: string, dopKey: string) => Promise<{ repositories: RepoType[] }>;
  getRepositoryKey: (repo: RepoType) => string;
  isMonorepoSetup: boolean;
  selectedDopSetting: DopSetting | undefined;
  setSearchQuery: (query: string) => void;
  setSelectedRepository: (repo: RepoType) => void;
  setShowPersonalAccessTokenForm: (show: boolean) => void;
}) {
  const repositorySearchDebounceId = useRef<NodeJS.Timeout | undefined>();
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<RepoType[] | undefined>();

  const onSearch = useCallback(
    (query: string) => {
      setSearchQuery(query);
      if (!selectedDopSetting) {
        return;
      }

      if (isEmpty(query)) {
        setSearchQuery('');
        setSearchResults(undefined);
        return;
      }

      clearTimeout(repositorySearchDebounceId.current);
      repositorySearchDebounceId.current = setTimeout(() => {
        setIsSearching(true);
        fetchSearchResults(query, selectedDopSetting.key).then(
          ({ repositories }) => {
            setIsSearching(false);
            setSearchResults(repositories);
          },
          () => setIsSearching(false),
        );
      }, REPOSITORY_SEARCH_DEBOUNCE_TIME);
    },
    [fetchSearchResults, selectedDopSetting, setSearchQuery],
  );

  const onSelectRepository = useCallback(
    (repositoryKey: string) => {
      const repo = searchResults?.find((o) => getRepositoryKey(o) === repositoryKey);
      if (searchResults && repo) {
        setSelectedRepository(repo);
      } else {
        // If we dont have a set of search results we should look for the repository in the base set of repositories
        defaultRepositorySelect(repositoryKey);
      }
    },
    [defaultRepositorySelect, getRepositoryKey, searchResults, setSelectedRepository],
  );

  useEffect(() => {
    setSearchResults(undefined);
    setSearchQuery('');
    setShowPersonalAccessTokenForm(true);
  }, [isMonorepoSetup, selectedDopSetting, setSearchQuery, setShowPersonalAccessTokenForm]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return {
    isSearching,
    onSearch,
    onSelectRepository,
    searchResults,
  };
}
