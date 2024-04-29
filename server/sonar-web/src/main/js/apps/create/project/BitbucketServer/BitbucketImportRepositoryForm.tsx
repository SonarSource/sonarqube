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
import { FlagMessage, InputSearch, Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { translate } from '../../../../helpers/l10n';
import {
  BitbucketProject,
  BitbucketProjectRepositories,
  BitbucketRepository,
} from '../../../../types/alm-integration';
import { CreateProjectModes } from '../types';
import BitbucketRepositories from './BitbucketRepositories';
import BitbucketSearchResults from './BitbucketSearchResults';

export interface BitbucketImportRepositoryFormProps {
  onSearch: (query: string) => void;
  onImportRepository: (repo: BitbucketRepository) => void;
  projects?: BitbucketProject[];
  projectRepositories?: BitbucketProjectRepositories;
  searching: boolean;
  searchResults?: BitbucketRepository[];
}

export default function BitbucketImportRepositoryForm(props: BitbucketImportRepositoryFormProps) {
  const { projects = [], projectRepositories = {}, searchResults, searching } = props;

  if (projects.length === 0) {
    return (
      <FlagMessage variant="warning">
        <span>
          <FormattedMessage
            defaultMessage={translate('onboarding.create_project.no_bbs_projects')}
            id="onboarding.create_project.no_bbs_projects"
            values={{
              link: (
                <Link
                  to={{
                    pathname: '/projects/create',
                    search: queryToSearchString({
                      mode: CreateProjectModes.BitbucketServer,
                      resetPat: 1,
                    }),
                  }}
                >
                  {translate('onboarding.create_project.update_your_token')}
                </Link>
              ),
            }}
          />
        </span>
      </FlagMessage>
    );
  }

  return (
    <div>
      <div className="sw-mb-10 sw-w-abs-400">
        <InputSearch
          searchInputAriaLabel={translate('onboarding.create_project.search_repositories_by_name')}
          onChange={props.onSearch}
          placeholder={translate('onboarding.create_project.search_repositories_by_name')}
          size="full"
        />
      </div>

      {searching || searchResults ? (
        <BitbucketSearchResults
          onImportRepository={props.onImportRepository}
          projects={projects}
          searchResults={searchResults}
          searching={searching}
        />
      ) : (
        <BitbucketRepositories
          onImportRepository={props.onImportRepository}
          projectRepositories={projectRepositories}
          projects={projects}
        />
      )}
    </div>
  );
}
