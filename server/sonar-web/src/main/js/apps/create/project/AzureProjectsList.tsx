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
import { uniqBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import Link from '../../../components/common/Link';
import ListFooter from '../../../components/controls/ListFooter';
import { Alert } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { queryToSearch } from '../../../helpers/urls';
import { AzureProject, AzureRepository } from '../../../types/alm-integration';
import { Dict } from '../../../types/types';
import AzureProjectAccordion from './AzureProjectAccordion';
import { CreateProjectModes } from './types';

export interface AzureProjectsListProps {
  importing: boolean;
  loadingRepositories: Dict<boolean>;
  onOpenProject: (key: string) => void;
  onSelectRepository: (repository: AzureRepository) => void;
  projects?: AzureProject[];
  repositories: Dict<AzureRepository[]>;
  searchResults?: AzureRepository[];
  searchQuery?: string;
  selectedRepository?: AzureRepository;
}

const PAGE_SIZE = 10;

export default function AzureProjectsList(props: AzureProjectsListProps) {
  const {
    importing,
    loadingRepositories,
    projects = [],
    repositories,
    searchResults,
    searchQuery,
    selectedRepository,
  } = props;

  const [page, setPage] = React.useState(1);

  if (searchResults && searchResults.length === 0) {
    return (
      <Alert className="spacer-top" variant="warning">
        {translate('onboarding.create_project.azure.no_results')}
      </Alert>
    );
  }

  if (projects.length === 0) {
    return (
      <Alert className="spacer-top" variant="warning">
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.azure.no_projects')}
          id="onboarding.create_project.azure.no_projects"
          values={{
            link: (
              <Link
                to={{
                  pathname: '/projects/create',
                  search: queryToSearch({ mode: CreateProjectModes.AzureDevOps, resetPat: 1 }),
                }}
              >
                {translate('onboarding.create_project.update_your_token')}
              </Link>
            ),
          }}
        />
      </Alert>
    );
  }

  let filteredProjects: AzureProject[];
  if (searchResults !== undefined) {
    filteredProjects = uniqBy(
      searchResults.map((r) => {
        return (
          projects.find((p) => p.name === r.projectName) || {
            name: r.projectName,
            description: translateWithParameters(
              'onboarding.create_project.azure.search_results_for_project_X',
              r.projectName
            ),
          }
        );
      }),
      'name'
    );
  } else {
    filteredProjects = projects;
  }

  const displayedProjects = filteredProjects.slice(0, page * PAGE_SIZE);

  // Add a suffix to the key to force react to not reuse AzureProjectAccordions between
  // search results and project exploration
  const keySuffix = searchResults ? ' - result' : '';

  return (
    <div>
      {displayedProjects.map((p, i) => (
        <AzureProjectAccordion
          key={`${p.name}${keySuffix}`}
          importing={importing}
          loading={Boolean(loadingRepositories[p.name])}
          onOpen={props.onOpenProject}
          onSelectRepository={props.onSelectRepository}
          project={p}
          repositories={
            searchResults
              ? searchResults.filter((s) => s.projectName === p.name)
              : repositories[p.name]
          }
          selectedRepository={selectedRepository}
          searchQuery={searchQuery}
          startsOpen={searchResults !== undefined || i === 0}
        />
      ))}

      <ListFooter
        count={displayedProjects.length}
        loadMore={() => setPage((p) => p + 1)}
        total={filteredProjects.length}
      />
    </div>
  );
}
