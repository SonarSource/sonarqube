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
import { FlagMessage, Spinner } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { BitbucketProject, BitbucketRepository } from '../../../../types/alm-integration';
import BitbucketProjectAccordion from './BitbucketProjectAccordion';

export interface BitbucketSearchResultsProps {
  onImportRepository: (repo: BitbucketRepository) => void;
  projects: BitbucketProject[];
  searching: boolean;
  searchResults?: BitbucketRepository[];
}

export default function BitbucketSearchResults(props: BitbucketSearchResultsProps) {
  const { projects, searching, searchResults = [] } = props;

  if (searchResults.length === 0 && !searching) {
    return (
      <FlagMessage variant="warning">
        {translate('onboarding.create_project.no_bbs_repos.filter')}
      </FlagMessage>
    );
  }

  const filteredProjects = projects.filter((p) =>
    searchResults.some((r) => r.projectKey === p.key),
  );
  const filteredProjectKeys = filteredProjects.map((p) => p.key);
  const filteredSearchResults = searchResults.filter(
    (r) => !filteredProjectKeys.includes(r.projectKey),
  );

  return (
    <Spinner loading={searching}>
      {filteredSearchResults.length > 0 && (
        <BitbucketProjectAccordion
          onImportRepository={props.onImportRepository}
          open
          repositories={filteredSearchResults}
          showingAllRepositories
        />
      )}

      {filteredProjects.map((project) => {
        const repositories = searchResults.filter((r) => r.projectKey === project.key);

        return (
          <BitbucketProjectAccordion
            onImportRepository={props.onImportRepository}
            key={project.key}
            open
            project={project}
            repositories={repositories}
            showingAllRepositories
          />
        );
      })}
    </Spinner>
  );
}
