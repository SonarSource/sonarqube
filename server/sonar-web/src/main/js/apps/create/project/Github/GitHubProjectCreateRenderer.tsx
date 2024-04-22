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

import { Link, Spinner } from '@sonarsource/echoes-react';
import { DarkLabel, FlagMessage, InputSelect, LightPrimary, Title } from 'design-system';
import React, { useContext, useEffect, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import { translate } from '../../../../helpers/l10n';
import { LabelValueSelectOption } from '../../../../helpers/search';
import { queryToSearch } from '../../../../helpers/urls';
import { GithubOrganization, GithubRepository } from '../../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { Paging } from '../../../../types/types';
import AlmSettingsInstanceDropdown from '../components/AlmSettingsInstanceDropdown';
import RepositoryList from '../components/RepositoryList';
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

function orgToOption({ key, name }: GithubOrganization) {
  return { value: key, label: name };
}

export default function GitHubProjectCreateRenderer(
  props: Readonly<GitHubProjectCreateRendererProps>,
) {
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

  useEffect(() => {
    const selectedKeys = Array.from(selected).filter((key) =>
      repositories?.find((r) => r.key === key),
    );
    setSelected(new Set(selectedKeys));
    // We want to update only when `repositories` changes.
    // If we subscribe to `selected` changes we will enter an infinite loop.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [repositories]);

  if (loadingBindings) {
    return <Spinner />;
  }

  const handleCheck = (key: string) => {
    setSelected((prev) => new Set(prev.delete(key) ? prev : prev.add(key)));
  };

  const handleCheckAll = () => {
    setSelected(
      new Set(repositories?.filter((r) => r.sqProjectKey === undefined).map((r) => r.key) ?? []),
    );
  };

  const handleImport = () => {
    props.onImportRepository(Array.from(selected));
  };

  const handleUncheckAll = () => {
    setSelected(new Set());
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

      <Spinner isLoading={loadingOrganizations && !error}>
        {!error && (
          <div className="sw-flex sw-flex-col">
            <DarkLabel htmlFor="github-choose-organization" className="sw-mb-2">
              {translate('onboarding.create_project.github.choose_organization')}
            </DarkLabel>
            {organizations.length > 0 ? (
              <InputSelect
                className="sw-w-7/12 sw-mb-9"
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
        {selectedOrganization && (
          <RepositoryList
            {...props}
            almKey={AlmKeys.GitHub}
            checkAll={handleCheckAll}
            onCheck={handleCheck}
            onImport={handleImport}
            selected={selected}
            uncheckAll={handleUncheckAll}
          />
        )}
      </Spinner>
    </>
  );
}
