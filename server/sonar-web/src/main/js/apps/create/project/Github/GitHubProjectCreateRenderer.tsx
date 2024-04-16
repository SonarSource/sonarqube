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
/* eslint-disable react/no-unused-prop-types */

import styled from '@emotion/styled';
import { Link, Spinner } from '@sonarsource/echoes-react';
import {
  ButtonPrimary,
  Checkbox,
  DarkLabel,
  FlagMessage,
  InputSearch,
  InputSelect,
  LightPrimary,
  Title,
  themeBorder,
  themeColor,
} from 'design-system';
import React, { useContext, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import ListFooter from '../../../../components/controls/ListFooter';
import { translate } from '../../../../helpers/l10n';
import { LabelValueSelectOption } from '../../../../helpers/search';
import { getBaseUrl } from '../../../../helpers/system';
import { queryToSearch } from '../../../../helpers/urls';
import { GithubOrganization, GithubRepository } from '../../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { Paging } from '../../../../types/types';
import AlmRepoItem from '../components/AlmRepoItem';
import AlmSettingsInstanceDropdown from '../components/AlmSettingsInstanceDropdown';
import { CreateProjectModes } from '../types';

interface GitHubProjectCreateRendererProps {
  canAdmin: boolean;
  error: boolean;
  loadingBindings: boolean;
  loadingOrganizations: boolean;
  loadingRepositories: boolean;
  onImportRepository: (key: string[]) => void;
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

type RepositoryListProps = Pick<
  GitHubProjectCreateRendererProps,
  | 'loadingRepositories'
  | 'repositories'
  | 'repositoryPaging'
  | 'searchQuery'
  | 'selectedOrganization'
  | 'onLoadMore'
  | 'onSearch'
> & {
  selected: Set<string>;
  checkAll: () => void;
  uncheckAll: () => void;
  onCheck: (key: string) => void;
};

function orgToOption({ key, name }: GithubOrganization) {
  return { value: key, label: name };
}

function RepositoryList(props: RepositoryListProps) {
  const {
    loadingRepositories,
    repositories,
    repositoryPaging,
    searchQuery,
    selectedOrganization,
    selected,
  } = props;

  const areAllRepositoriesChecked = () => {
    const nonImportedRepos = repositories?.filter((r) => !r.sqProjectKey) ?? [];
    return nonImportedRepos.length > 0 && selected.size === nonImportedRepos.length;
  };

  const onCheckAllRepositories = () => {
    const allSelected = areAllRepositoriesChecked();
    if (allSelected) {
      props.uncheckAll();
    } else {
      props.checkAll();
    }
  };

  if (!selectedOrganization || !repositories) {
    return null;
  }

  return (
    <div>
      <div className="sw-mb-2 sw-py-2 sw-flex sw-items-center sw-justify-between sw-w-full">
        <div>
          <Checkbox
            className="sw-ml-5"
            checked={areAllRepositoriesChecked()}
            disabled={repositories.length === 0}
            onCheck={onCheckAllRepositories}
          >
            <span className="sw-ml-2">
              {translate('onboarding.create_project.select_all_repositories')}
            </span>
          </Checkbox>
        </div>
        <InputSearch
          size="medium"
          loading={loadingRepositories}
          onChange={props.onSearch}
          placeholder={translate('onboarding.create_project.search_repositories')}
          value={searchQuery}
        />
      </div>

      {repositories.length === 0 ? (
        <div className="sw-py-6 sw-px-2">
          <LightPrimary className="sw-body-sm">{translate('no_results')}</LightPrimary>
        </div>
      ) : (
        <ul className="sw-flex sw-flex-col sw-gap-3">
          {repositories.map(({ key, url, sqProjectKey, name }) => (
            <AlmRepoItem
              key={key}
              almKey={key}
              almUrl={url}
              almUrlText={translate('onboarding.create_project.see_on_github')}
              almIconSrc={`${getBaseUrl()}/images/tutorials/github-actions.svg`}
              sqProjectKey={sqProjectKey}
              multiple
              selected={selected.has(key)}
              onCheck={(key: string) => props.onCheck(key)}
              primaryTextNode={<span title={name}>{name}</span>}
            />
          ))}
        </ul>
      )}

      <ListFooter
        className="sw-mb-10"
        count={repositories.length}
        total={repositoryPaging.total}
        loadMore={props.onLoadMore}
        loading={loadingRepositories}
      />
    </div>
  );
}

export default function GitHubProjectCreateRenderer(props: GitHubProjectCreateRendererProps) {
  const isMonorepoSupported = useContext(AvailableFeaturesContext).includes(
    Feature.MonoRepositoryPullRequestDecoration,
  );

  const {
    canAdmin,
    error,
    loadingBindings,
    loadingOrganizations,
    organizations,
    selectedOrganization,
    almInstances,
    selectedAlmInstance,
    repositories,
  } = props;
  const [selected, setSelected] = useState<Set<string>>(new Set());

  if (loadingBindings) {
    return <Spinner />;
  }

  const handleImport = () => {
    props.onImportRepository(Array.from(selected));
  };

  const handleCheckAll = () => {
    setSelected(new Set(repositories?.filter((r) => !r.sqProjectKey).map((r) => r.key) ?? []));
  };

  const handleUncheckAll = () => {
    setSelected(new Set());
  };

  const handleCheck = (key: string) => {
    setSelected((prev) => new Set(prev.delete(key) ? prev : prev.add(key)));
  };

  return (
    <>
      <header className="sw-mb-10">
        <Title className="sw-mb-4">{translate('onboarding.create_project.github.title')}</Title>
        <LightPrimary className="sw-body-sm">
          {isMonorepoSupported ? (
            <FormattedMessage
              id="onboarding.create_project.github.subtitle.with_monorepo"
              values={{
                monorepoSetupLink: (
                  <Link
                    to={{
                      pathname: '/projects/create',
                      search: queryToSearch({
                        mode: CreateProjectModes.GitHub,
                        mono: true,
                      }),
                    }}
                  >
                    <FormattedMessage id="onboarding.create_project.subtitle_monorepo_setup_link" />
                  </Link>
                ),
              }}
            />
          ) : (
            <FormattedMessage id="onboarding.create_project.github.subtitle" />
          )}
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

      <div className="sw-flex sw-gap-12">
        <LargeColumn>
          <Spinner isLoading={loadingOrganizations && !error}>
            {!error && (
              <div className="sw-flex sw-flex-col">
                <DarkLabel htmlFor="github-choose-organization" className="sw-mb-2">
                  {translate('onboarding.create_project.github.choose_organization')}
                </DarkLabel>
                {organizations.length > 0 ? (
                  <InputSelect
                    className="sw-w-full sw-mb-9"
                    size="full"
                    isSearchable
                    inputId="github-choose-organization"
                    options={organizations.map(orgToOption)}
                    onChange={({ value }: LabelValueSelectOption) =>
                      props.onSelectOrganization(value)
                    }
                    value={selectedOrganization ? orgToOption(selectedOrganization) : null}
                  />
                ) : (
                  !loadingOrganizations && (
                    <FlagMessage variant="error" className="sw-mb-2">
                      <span>
                        {canAdmin ? (
                          <FormattedMessage
                            id="onboarding.create_project.github.no_orgs_admin"
                            defaultMessage={translate(
                              'onboarding.create_project.github.no_orgs_admin',
                            )}
                            values={{
                              link: (
                                <Link to="/admin/settings?category=almintegration">
                                  {translate(
                                    'onboarding.create_project.github.warning.message_admin.link',
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
          </Spinner>
          <RepositoryList
            {...props}
            selected={selected}
            checkAll={handleCheckAll}
            uncheckAll={handleUncheckAll}
            onCheck={handleCheck}
          />
        </LargeColumn>
        <SideColumn>
          {selected.size > 0 && (
            <SetupBox className="sw-rounded-2 sw-p-8 sw-mb-0">
              <SetupBoxTitle className="sw-mb-2 sw-heading-md">
                <FormattedMessage
                  id="onboarding.create_project.x_repositories_selected"
                  values={{ count: selected.size }}
                />
              </SetupBoxTitle>
              <div>
                <SetupBoxContent className="sw-pb-4">
                  <FormattedMessage
                    id="onboarding.create_project.x_repository_created"
                    values={{ count: selected.size }}
                  />
                </SetupBoxContent>
                <div className="sw-mt-4">
                  <ButtonPrimary onClick={handleImport} className="js-set-up-projects">
                    {translate('onboarding.create_project.import')}
                  </ButtonPrimary>
                </div>
              </div>
            </SetupBox>
          )}
        </SideColumn>
      </div>
    </>
  );
}

const LargeColumn = styled.div`
  flex: 6;
`;

const SideColumn = styled.div`
  flex: 4;
`;

const SetupBox = styled.form`
  max-height: 280px;
  background: ${themeColor('highlightedSection')};
  border: ${themeBorder('default', 'highlightedSectionBorder')};
`;

const SetupBoxTitle = styled.h2`
  color: ${themeColor('pageTitle')};
`;

const SetupBoxContent = styled.div`
  border-bottom: ${themeBorder('default')};
`;
