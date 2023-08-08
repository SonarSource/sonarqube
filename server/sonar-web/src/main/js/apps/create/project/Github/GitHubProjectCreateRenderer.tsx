/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
/* eslint-disable react/no-unused-prop-types */

import {
  DarkLabel,
  DeferredSpinner,
  FlagMessage,
  InputSearch,
  InputSelect,
  LightPrimary,
  Link,
  Title,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import ListFooter from '../../../../components/controls/ListFooter';
import { LabelValueSelectOption } from '../../../../components/controls/Select';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { GithubOrganization, GithubRepository } from '../../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import { Paging } from '../../../../types/types';
import AlmRepoItem from '../components/AlmRepoItem';
import AlmSettingsInstanceDropdown from '../components/AlmSettingsInstanceDropdown';

export interface GitHubProjectCreateRendererProps {
  canAdmin: boolean;
  error: boolean;
  loadingBindings: boolean;
  loadingOrganizations: boolean;
  loadingRepositories: boolean;
  onImportRepository: (key: string) => void;
  onLoadMore: () => void;
  onSearch: (q: string) => void;
  onSelectOrganization: (key: string) => void;
  organizations: GithubOrganization[];
  repositories?: GithubRepository[];
  repositoryPaging: Paging;
  searchQuery: string;
  selectedOrganization?: GithubOrganization;
  almInstances: AlmSettingsInstance[];
  selectedAlmInstance?: AlmSettingsInstance;
  onSelectedAlmInstanceChange: (instance: AlmSettingsInstance) => void;
}

function orgToOption({ key, name }: GithubOrganization) {
  return { value: key, label: name };
}

function renderRepositoryList(props: GitHubProjectCreateRendererProps) {
  const { loadingRepositories, repositories, repositoryPaging, searchQuery, selectedOrganization } =
    props;

  return (
    selectedOrganization &&
    repositories && (
      <div>
        <div className="sw-flex sw-items-center sw-mb-6">
          <InputSearch
            size="large"
            onChange={props.onSearch}
            placeholder={translate('onboarding.create_project.search_repositories')}
            value={searchQuery}
            clearIconAriaLabel={translate('clear')}
          />
          <DeferredSpinner loading={loadingRepositories} className="sw-ml-2" />
        </div>

        {repositories.length === 0 ? (
          <div className="sw-py-6 sw-px-2">
            <LightPrimary className="sw-body-sm">{translate('no_results')}</LightPrimary>
          </div>
        ) : (
          repositories.map((r) => (
            <AlmRepoItem
              key={r.key}
              almKey={r.key}
              almUrl={r.url}
              almUrlText={translate('onboarding.create_project.see_on_github')}
              almIconSrc={`${getBaseUrl()}/images/tutorials/github-actions.svg`}
              sqProjectKey={r.sqProjectKey}
              onImport={props.onImportRepository}
              primaryTextNode={<span title={r.name}>{r.name}</span>}
            />
          ))
        )}

        <div className="display-flex-justify-center width-100">
          <ListFooter
            count={repositories.length}
            total={repositoryPaging.total}
            loadMore={props.onLoadMore}
            loading={loadingRepositories}
            useMIUIButtons
          />
        </div>
      </div>
    )
  );
}

export default function GitHubProjectCreateRenderer(props: GitHubProjectCreateRendererProps) {
  const {
    canAdmin,
    error,
    loadingBindings,
    loadingOrganizations,
    organizations,
    selectedOrganization,
    almInstances,
    selectedAlmInstance,
  } = props;

  if (loadingBindings) {
    return <DeferredSpinner />;
  }

  return (
    <>
      <header className="sw-mb-10">
        <Title className="sw-mb-4">{translate('onboarding.create_project.github.title')}</Title>
        <LightPrimary className="sw-body-sm">
          {translate('onboarding.create_project.github.subtitle')}
        </LightPrimary>
      </header>

      <AlmSettingsInstanceDropdown
        almKey={AlmKeys.GitHub}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onChangeConfig={props.onSelectedAlmInstanceChange}
      />

      {error && selectedAlmInstance && (
        <FlagMessage variant="warning" className="sw-my-2">
          <span>
            {canAdmin ? (
              <FormattedMessage
                id="onboarding.create_project.github.warning.message_admin"
                defaultMessage={translate('onboarding.create_project.github.warning.message_admin')}
                values={{
                  link: (
                    <Link to="/admin/settings?category=almintegration">
                      {translate('onboarding.create_project.github.warning.message_admin.link')}
                    </Link>
                  ),
                }}
              />
            ) : (
              translate('onboarding.create_project.github.warning.message')
            )}
          </span>
        </FlagMessage>
      )}

      <DeferredSpinner loading={loadingOrganizations && !error}>
        {!error && (
          <div className="sw-flex sw-flex-col">
            <DarkLabel htmlFor="github-choose-organization" className="sw-mb-2">
              {translate('onboarding.create_project.github.choose_organization')}
            </DarkLabel>
            {organizations.length > 0 ? (
              <InputSelect
                className="sw-w-abs-300 sw-mb-9"
                size="full"
                isSearchable
                inputId="github-choose-organization"
                options={organizations.map(orgToOption)}
                onChange={({ value }: LabelValueSelectOption) => props.onSelectOrganization(value)}
                value={selectedOrganization ? orgToOption(selectedOrganization) : null}
              />
            ) : (
              !loadingOrganizations && (
                <FlagMessage variant="error" className="sw-mb-2">
                  <span>
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
                          ),
                        }}
                      />
                    ) : (
                      translate('onboarding.create_project.github.no_orgs')
                    )}
                  </span>
                </FlagMessage>
              )
            )}
          </div>
        )}
      </DeferredSpinner>

      {renderRepositoryList(props)}
    </>
  );
}
