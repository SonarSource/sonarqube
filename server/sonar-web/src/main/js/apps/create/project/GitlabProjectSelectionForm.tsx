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
import { Button } from '../../../components/controls/buttons';
import ListFooter from '../../../components/controls/ListFooter';
import SearchBox from '../../../components/controls/SearchBox';
import Tooltip from '../../../components/controls/Tooltip';
import CheckIcon from '../../../components/icons/CheckIcon';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl, queryToSearch } from '../../../helpers/urls';
import { GitlabProject } from '../../../types/alm-integration';
import { ComponentQualifier } from '../../../types/component';
import { Paging } from '../../../types/types';
import { CreateProjectModes } from './types';

export interface GitlabProjectSelectionFormProps {
  importingGitlabProjectId?: string;
  loadingMore: boolean;
  onImport: (gitlabProjectId: string) => void;
  onLoadMore: () => void;
  onSearch: (searchQuery: string) => void;
  projects?: GitlabProject[];
  projectsPaging: Paging;
  searching: boolean;
  searchQuery: string;
}

export default function GitlabProjectSelectionForm(props: GitlabProjectSelectionFormProps) {
  const {
    importingGitlabProjectId,
    loadingMore,
    projects = [],
    projectsPaging,
    searching,
    searchQuery,
  } = props;

  if (projects.length === 0 && searchQuery.length === 0 && !searching) {
    return (
      <Alert className="spacer-top" variant="warning">
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.gitlab.no_projects')}
          id="onboarding.create_project.gitlab.no_projects"
          values={{
            link: (
              <Link
                to={{
                  pathname: '/projects/create',
                  search: queryToSearch({ mode: CreateProjectModes.GitLab, resetPat: 1 }),
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
    <div className="boxed-group big-padded create-project-import">
      <SearchBox
        className="spacer"
        loading={searching}
        minLength={3}
        onChange={props.onSearch}
        placeholder={translate('onboarding.create_project.search_prompt')}
      />

      <hr />

      {projects.length === 0 ? (
        <div className="padded">{translate('no_results')}</div>
      ) : (
        <table className="data zebra zebra-hover">
          <tbody>
            {projects.map((project) => (
              <tr key={project.id}>
                <td>
                  <Tooltip overlay={project.slug}>
                    <strong className="project-name display-inline-block text-ellipsis">
                      {project.sqProjectKey ? (
                        <Link to={getProjectUrl(project.sqProjectKey)}>
                          <QualifierIcon
                            className="spacer-right"
                            qualifier={ComponentQualifier.Project}
                          />
                          {project.sqProjectName}
                        </Link>
                      ) : (
                        project.name
                      )}
                    </strong>
                  </Tooltip>
                  <br />
                  <Tooltip overlay={project.pathSlug}>
                    <span className="text-muted project-path display-inline-block text-ellipsis">
                      {project.pathName}
                    </span>
                  </Tooltip>
                </td>
                <td>
                  <Link
                    className="display-inline-flex-center big-spacer-right"
                    to={project.url}
                    target="_blank"
                  >
                    {translate('onboarding.create_project.gitlab.link')}
                  </Link>
                </td>
                {project.sqProjectKey ? (
                  <td>
                    <span className="display-flex-center display-flex-justify-end already-set-up">
                      <CheckIcon className="little-spacer-right" size={12} />
                      {translate('onboarding.create_project.repository_imported')}
                    </span>
                  </td>
                ) : (
                  <td className="text-right">
                    <Button
                      disabled={!!importingGitlabProjectId}
                      onClick={() => props.onImport(project.id)}
                    >
                      {translate('onboarding.create_project.set_up')}
                      {importingGitlabProjectId === project.id && (
                        <DeferredSpinner className="spacer-left" />
                      )}
                    </Button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <ListFooter
        count={projects.length}
        loadMore={props.onLoadMore}
        loading={loadingMore}
        pageSize={projectsPaging.pageSize}
        total={projectsPaging.total}
      />
    </div>
  );
}
