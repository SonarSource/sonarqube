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
import { FormattedMessage } from 'react-intl';
import Link from '../../../components/common/Link';
import SearchBox from '../../../components/controls/SearchBox';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { queryToSearch } from '../../../helpers/urls';
import {
  BitbucketProject,
  BitbucketProjectRepositories,
  BitbucketRepository,
} from '../../../types/alm-integration';
import BitbucketRepositories from './BitbucketRepositories';
import BitbucketSearchResults from './BitbucketSearchResults';
import { CreateProjectModes } from './types';

export interface BitbucketImportRepositoryFormProps {
  disableRepositories: boolean;
  onSearch: (query: string) => void;
  onSelectRepository: (repo: BitbucketRepository) => void;
  projects?: BitbucketProject[];
  projectRepositories?: BitbucketProjectRepositories;
  searching: boolean;
  searchResults?: BitbucketRepository[];
  selectedRepository?: BitbucketRepository;
}

export default function BitbucketImportRepositoryForm(props: BitbucketImportRepositoryFormProps) {
  const {
    disableRepositories,
    projects = [],
    projectRepositories = {},
    searchResults,
    searching,
    selectedRepository,
  } = props;

  if (projects.length === 0) {
    return (
      <Alert className="spacer-top" variant="warning">
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.no_bbs_projects')}
          id="onboarding.create_project.no_bbs_projects"
          values={{
            link: (
              <Link
                to={{
                  pathname: '/projects/create',
                  search: queryToSearch({ mode: CreateProjectModes.BitbucketServer, resetPat: 1 }),
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

  return (
    <div className="create-project-import-bbs">
      <SearchBox
        onChange={props.onSearch}
        placeholder={translate('onboarding.create_project.search_repositories_by_name')}
      />

      {searching || searchResults ? (
        <BitbucketSearchResults
          disableRepositories={disableRepositories}
          onSelectRepository={props.onSelectRepository}
          projects={projects}
          searchResults={searchResults}
          searching={searching}
          selectedRepository={selectedRepository}
        />
      ) : (
        <BitbucketRepositories
          disableRepositories={disableRepositories}
          onSelectRepository={props.onSelectRepository}
          projectRepositories={projectRepositories}
          projects={projects}
          selectedRepository={selectedRepository}
        />
      )}
    </div>
  );
}
