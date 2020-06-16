/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Link } from 'react-router';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import Radio from 'sonar-ui-common/components/controls/Radio';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import SearchSelect from 'sonar-ui-common/components/controls/SearchSelect';
import CheckIcon from 'sonar-ui-common/components/icons/CheckIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { GithubOrganization, GithubRepository } from '../../../types/alm-integration';
import CreateProjectPageHeader from './CreateProjectPageHeader';

export interface GitHubProjectCreateRendererProps {
  canAdmin: boolean;
  error: boolean;
  loading: boolean;
  loadingRepositories: boolean;
  onLoadMore: () => void;
  onSearch: (q: string) => void;
  onSelectOrganization: (key: string) => void;
  onSelectRepository: (key: string) => void;
  organizations: GithubOrganization[];
  repositories?: GithubRepository[];
  repositoryPaging: T.Paging;
  searchQuery: string;
  selectedOrganization?: GithubOrganization;
  selectedRepository?: GithubRepository;
}

function orgToOption({ key, name }: GithubOrganization) {
  return { value: key, label: name };
}

export default function GitHubProjectCreateRenderer(props: GitHubProjectCreateRendererProps) {
  const {
    canAdmin,
    error,
    loading,
    loadingRepositories,
    organizations,
    repositories,
    repositoryPaging,
    searchQuery,
    selectedOrganization,
    selectedRepository
  } = props;

  return (
    <div>
      <CreateProjectPageHeader
        title={
          <span className="text-middle display-flex-center">
            <img
              alt="" // Should be ignored by screen readers
              className="spacer-right"
              height={24}
              src={`${getBaseUrl()}/images/alm/github.svg`}
            />
            {translate('onboarding.create_project.github.title')}
          </span>
        }
      />

      {error ? (
        <div className="display-flex-justify-center">
          <div className="boxed-group padded width-50 huge-spacer-top">
            <h2 className="big-spacer-bottom">
              {translate('onboarding.create_project.github.warning.title')}
            </h2>
            <Alert variant="warning">
              {canAdmin ? (
                <FormattedMessage
                  id="onboarding.create_project.github.warning.message_admin"
                  defaultMessage={translate(
                    'onboarding.create_project.github.warning.message_admin'
                  )}
                  values={{
                    link: (
                      <Link to="/admin/settings?category=almintegration">
                        {translate('onboarding.create_project.github.warning.message_admin.link')}
                      </Link>
                    )
                  }}
                />
              ) : (
                translate('onboarding.create_project.github.warning.message')
              )}
            </Alert>
          </div>
        </div>
      ) : (
        <DeferredSpinner loading={loading}>
          <div className="form-field">
            <label>{translate('onboarding.create_project.github.choose_organization')}</label>
            {organizations.length > 0 ? (
              <SearchSelect
                defaultOptions={organizations.slice(0, 10).map(orgToOption)}
                onSearch={(q: string) =>
                  Promise.resolve(
                    organizations.filter(o => !q || o.name.includes(q)).map(orgToOption)
                  )
                }
                minimumQueryLength={0}
                onSelect={({ value }) => props.onSelectOrganization(value)}
                value={selectedOrganization && orgToOption(selectedOrganization)}
              />
            ) : (
              !loading && (
                <Alert className="spacer-top" variant="error">
                  {canAdmin ? (
                    <FormattedMessage
                      id="onboarding.create_project.github.no_orgs_admin"
                      defaultMessage={translate('onboarding.create_project.github.no_orgs_admin')}
                      values={{
                        link: (
                          <Link to="/admin/settings?category=almintegration">
                            {translate(
                              'onboarding.create_project.github.warning.message_admin.link'
                            )}
                          </Link>
                        )
                      }}
                    />
                  ) : (
                    translate('onboarding.create_project.github.no_orgs')
                  )}
                </Alert>
              )
            )}
          </div>
        </DeferredSpinner>
      )}

      {selectedOrganization && repositories && (
        <div className="boxed-group padded display-flex-wrap">
          <div className="width-100">
            <SearchBox
              className="big-spacer-bottom"
              onChange={props.onSearch}
              placeholder={translate('onboarding.create_project.search_repositories')}
              value={searchQuery}
            />
          </div>

          {repositories.length === 0 ? (
            <div className="padded">
              <DeferredSpinner loading={loadingRepositories}>
                {translate('no_results')}
              </DeferredSpinner>
            </div>
          ) : (
            repositories.map(r => (
              <Radio
                className="spacer-top spacer-bottom padded create-project-github-repository"
                key={r.key}
                checked={
                  !!r.sqProjectKey || (!!selectedRepository && selectedRepository.key === r.key)
                }
                disabled={!!r.sqProjectKey || loadingRepositories || importing}
                value={r.key}
                onCheck={props.onSelectRepository}>
                <div className="big overflow-hidden" title={r.name}>
                  <div className="overflow-hidden text-ellipsis">{r.name}</div>
                  {r.sqProjectKey && (
                    <em className="notice text-muted-2 small display-flex-center">
                      {translate('onboarding.create_project.repository_imported')}
                      <CheckIcon className="little-spacer-left" size={12} />
                    </em>
                  )}
                </div>
              </Radio>
            ))
          )}

          <div className="display-flex-justify-center width-100">
            <ListFooter
              count={repositories.length}
              total={repositoryPaging.total}
              loadMore={props.onLoadMore}
              loading={loadingRepositories}
            />
          </div>
        </div>
      )}
    </div>
  );
}
