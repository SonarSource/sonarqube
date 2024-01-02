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
import SearchBox from '../../../components/controls/SearchBox';
import Tooltip from '../../../components/controls/Tooltip';
import CheckIcon from '../../../components/icons/CheckIcon';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { getProjectUrl, queryToSearch } from '../../../helpers/urls';
import { BitbucketCloudRepository } from '../../../types/alm-integration';
import { ComponentQualifier } from '../../../types/component';
import { CreateProjectModes } from './types';

export interface BitbucketCloudSearchFormProps {
  importingSlug?: string;
  isLastPage: boolean;
  loadingMore: boolean;
  onImport: (repositorySlug: string) => void;
  onLoadMore: () => void;
  onSearch: (searchQuery: string) => void;
  repositories?: BitbucketCloudRepository[];
  searching: boolean;
  searchQuery: string;
}

function getRepositoryUrl(workspace: string, slug: string) {
  return `https://bitbucket.org/${workspace}/${slug}`;
}

export default function BitbucketCloudSearchForm(props: BitbucketCloudSearchFormProps) {
  const {
    importingSlug,
    isLastPage,
    loadingMore,
    repositories = [],
    searching,
    searchQuery,
  } = props;

  if (repositories.length === 0 && searchQuery.length === 0 && !searching) {
    return (
      <Alert className="spacer-top" variant="warning">
        <FormattedMessage
          defaultMessage={translate('onboarding.create_project.bitbucketcloud.no_projects')}
          id="onboarding.create_project.bitbucketcloud.no_projects"
          values={{
            link: (
              <Link
                to={{
                  pathname: '/projects/create',
                  search: queryToSearch({ mode: CreateProjectModes.BitbucketCloud, resetPat: 1 }),
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

      {repositories.length === 0 ? (
        <div className="padded">{translate('no_results')}</div>
      ) : (
        <table className="data zebra zebra-hover">
          <tbody>
            {repositories.map((repository) => (
              <tr key={repository.uuid}>
                <td>
                  <Tooltip overlay={repository.slug}>
                    <strong className="project-name display-inline-block text-ellipsis">
                      {repository.sqProjectKey ? (
                        <Link to={getProjectUrl(repository.sqProjectKey)}>
                          <QualifierIcon
                            className="spacer-right"
                            qualifier={ComponentQualifier.Project}
                          />
                          {repository.name}
                        </Link>
                      ) : (
                        repository.name
                      )}
                    </strong>
                  </Tooltip>
                  <br />
                  <Tooltip overlay={repository.projectKey}>
                    <span className="text-muted project-path display-inline-block text-ellipsis">
                      {repository.projectKey}
                    </span>
                  </Tooltip>
                </td>
                <td>
                  <Link
                    className="display-inline-flex-center big-spacer-right"
                    to={getRepositoryUrl(repository.workspace, repository.slug)}
                    target="_blank"
                  >
                    {translate('onboarding.create_project.bitbucketcloud.link')}
                  </Link>
                </td>
                {repository.sqProjectKey ? (
                  <td>
                    <span className="display-flex-center display-flex-justify-end already-set-up">
                      <CheckIcon className="little-spacer-right" size={12} />
                      {translate('onboarding.create_project.repository_imported')}
                    </span>
                  </td>
                ) : (
                  <td className="text-right">
                    <Button
                      disabled={Boolean(importingSlug)}
                      onClick={() => {
                        props.onImport(repository.slug);
                      }}
                    >
                      {translate('onboarding.create_project.set_up')}
                      {importingSlug === repository.slug && (
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
      <footer className="spacer-top note text-center">
        {isLastPage &&
          translateWithParameters(
            'x_of_y_shown',
            formatMeasure(repositories.length, 'INT', null),
            formatMeasure(repositories.length, 'INT', null)
          )}
        {!isLastPage && (
          <Button
            className="spacer-left"
            disabled={loadingMore}
            data-test="show-more"
            onClick={props.onLoadMore}
          >
            {translate('show_more')}
          </Button>
        )}
        {loadingMore && <DeferredSpinner className="text-bottom spacer-left position-absolute" />}
      </footer>
    </div>
  );
}
