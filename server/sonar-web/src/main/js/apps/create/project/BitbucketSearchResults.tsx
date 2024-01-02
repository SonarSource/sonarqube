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
import * as React from 'react';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { BitbucketProject, BitbucketRepository } from '../../../types/alm-integration';
import BitbucketProjectAccordion from './BitbucketProjectAccordion';

export interface BitbucketSearchResultsProps {
  disableRepositories: boolean;
  onSelectRepository: (repo: BitbucketRepository) => void;
  projects: BitbucketProject[];
  searching: boolean;
  searchResults?: BitbucketRepository[];
  selectedRepository?: BitbucketRepository;
}

export default function BitbucketSearchResults(props: BitbucketSearchResultsProps) {
  const {
    disableRepositories,
    projects,
    searching,
    searchResults = [],
    selectedRepository,
  } = props;

  if (searchResults.length === 0 && !searching) {
    return (
      <Alert className="big-spacer-top" variant="warning">
        {translate('onboarding.create_project.no_bbs_repos.filter')}
      </Alert>
    );
  }

  const filteredProjects = projects.filter((p) =>
    searchResults.some((r) => r.projectKey === p.key)
  );
  const filteredProjectKeys = filteredProjects.map((p) => p.key);
  const filteredSearchResults = searchResults.filter(
    (r) => !filteredProjectKeys.includes(r.projectKey)
  );

  return (
    <div className="big-spacer-top">
      <DeferredSpinner loading={searching}>
        {filteredSearchResults.length > 0 && (
          <BitbucketProjectAccordion
            disableRepositories={disableRepositories}
            onSelectRepository={props.onSelectRepository}
            open={true}
            repositories={filteredSearchResults}
            selectedRepository={selectedRepository}
            showingAllRepositories={true}
          />
        )}

        {filteredProjects.map((project) => {
          const repositories = searchResults.filter((r) => r.projectKey === project.key);

          return (
            <BitbucketProjectAccordion
              disableRepositories={disableRepositories}
              key={project.key}
              onSelectRepository={props.onSelectRepository}
              open={true}
              project={project}
              repositories={repositories}
              selectedRepository={selectedRepository}
              showingAllRepositories={true}
            />
          );
        })}
      </DeferredSpinner>
    </div>
  );
}
