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
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { AzureProject, AzureRepository } from '../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import AzurePersonalAccessTokenForm from './AzurePersonalAccessTokenForm';
import AzureProjectsList from './AzureProjectsList';
import CreateProjectPageHeader from './CreateProjectPageHeader';
import WrongBindingCountAlert from './WrongBindingCountAlert';

export interface AzureProjectCreateRendererProps {
  canAdmin?: boolean;
  loading: boolean;
  loadingRepositories: T.Dict<boolean>;
  onOpenProject: (key: string) => void;
  onPersonalAccessTokenCreate: (token: string) => void;
  onSearch: (query: string) => void;
  projects?: AzureProject[];
  repositories: T.Dict<AzureRepository[]>;
  searching?: boolean;
  searchResults?: T.Dict<AzureRepository[]>;
  settings?: AlmSettingsInstance;
  showPersonalAccessTokenForm?: boolean;
  submittingToken?: boolean;
  tokenValidationFailed: boolean;
}

export default function AzureProjectCreateRenderer(props: AzureProjectCreateRendererProps) {
  const {
    canAdmin,
    loading,
    loadingRepositories,
    projects,
    repositories,
    searching,
    searchResults,
    showPersonalAccessTokenForm,
    settings,
    submittingToken,
    tokenValidationFailed
  } = props;

  return (
    <>
      <CreateProjectPageHeader
        title={
          <span className="text-middle">
            <img
              alt="" // Should be ignored by screen readers
              className="spacer-right"
              height="24"
              src={`${getBaseUrl()}/images/alm/azure.svg`}
            />
            {translate('onboarding.create_project.azure.title')}
          </span>
        }
      />

      {loading && <i className="spinner" />}

      {!loading && !settings && (
        <WrongBindingCountAlert alm={AlmKeys.Azure} canAdmin={!!canAdmin} />
      )}

      {!loading &&
        settings &&
        (showPersonalAccessTokenForm ? (
          <div className="display-flex-justify-center">
            <AzurePersonalAccessTokenForm
              almSetting={settings}
              onPersonalAccessTokenCreate={props.onPersonalAccessTokenCreate}
              submitting={submittingToken}
              validationFailed={tokenValidationFailed}
            />
          </div>
        ) : (
          <>
            <div className="huge-spacer-bottom">
              <SearchBox
                onChange={props.onSearch}
                placeholder={translate('onboarding.create_project.search_repositories_by_name')}
              />
            </div>
            <DeferredSpinner loading={Boolean(searching)}>
              <AzureProjectsList
                loadingRepositories={loadingRepositories}
                onOpenProject={props.onOpenProject}
                projects={projects}
                repositories={repositories}
                searchResults={searchResults}
              />
            </DeferredSpinner>
          </>
        ))}
    </>
  );
}
