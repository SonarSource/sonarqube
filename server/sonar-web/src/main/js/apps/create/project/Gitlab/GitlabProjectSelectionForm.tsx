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

import { Link } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { FlagMessage, InputSearch, LightPrimary } from '~design-system';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import ListFooter from '../../../../components/controls/ListFooter';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { GitlabProject } from '../../../../types/alm-integration';
import { Paging } from '../../../../types/types';
import AlmRepoItem from '../components/AlmRepoItem';
import { CreateProjectModes } from '../types';

export interface GitlabProjectSelectionFormProps {
  loadingMore: boolean;
  onImport: (gitlabProjectId: string) => void;
  onLoadMore: () => void;
  onSearch: (searchQuery: string) => void;
  projects?: GitlabProject[];
  projectsPaging: Paging;
  searchQuery: string;
  searching: boolean;
}

export default function GitlabProjectSelectionForm(
  props: Readonly<GitlabProjectSelectionFormProps>,
) {
  const { loadingMore, projects = [], projectsPaging, searching, searchQuery } = props;

  if (projects.length === 0 && searchQuery.length === 0 && !searching) {
    return (
      <FlagMessage className="sw-mt-2" variant="warning">
        <span>
          <FormattedMessage
            defaultMessage={translate('onboarding.create_project.gitlab.no_projects')}
            id="onboarding.create_project.gitlab.no_projects"
            values={{
              link: (
                <Link
                  to={{
                    pathname: '/projects/create',
                    search: queryToSearchString({ mode: CreateProjectModes.GitLab, resetPat: 1 }),
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
    <>
      <InputSearch
        size="large"
        className="sw-mb-6"
        onChange={props.onSearch}
        placeholder={translate('onboarding.create_project.search_repositories')}
        value={searchQuery}
        minLength={3}
        loading={searching}
      />

      {projects.length === 0 ? (
        <div className="sw-py-6 sw-px-2">
          <LightPrimary className="sw-typo-default">{translate('no_results')}</LightPrimary>
        </div>
      ) : (
        <ul className="sw-flex sw-flex-col sw-gap-3">
          {projects.map((project) => (
            <AlmRepoItem
              key={project.id}
              almKey={project.id}
              almUrl={project.url}
              almUrlText={translate('onboarding.create_project.gitlab.link')}
              almIconSrc={`${getBaseUrl()}/images/alm/gitlab.svg`}
              sqProjectKey={project.sqProjectKey}
              onImport={props.onImport}
              primaryTextNode={
                <Tooltip content={project.slug}>
                  <span>{project.name}</span>
                </Tooltip>
              }
              secondaryTextNode={
                <Tooltip content={project.pathSlug}>
                  <span>{project.pathName}</span>
                </Tooltip>
              }
            />
          ))}
        </ul>
      )}
      <ListFooter
        className="sw-mb-10"
        count={projects.length}
        loadMore={props.onLoadMore}
        loading={loadingMore}
        pageSize={projectsPaging.pageSize}
        total={projectsPaging.total}
      />
    </>
  );
}
