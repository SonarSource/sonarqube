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
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { getGlobalSettingsUrl } from '../../../helpers/urls';
import { AzureProject, AzureRepository } from '../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { Dict } from '../../../types/types';
import { ALM_INTEGRATION_CATEGORY } from '../../settings/constants';
import AlmSettingsInstanceDropdown from './AlmSettingsInstanceDropdown';
import AzurePersonalAccessTokenForm from './AzurePersonalAccessTokenForm';
import AzureProjectsList from './AzureProjectsList';
import CreateProjectPageHeader from './CreateProjectPageHeader';
import WrongBindingCountAlert from './WrongBindingCountAlert';

export interface AzureProjectCreateRendererProps {
  canAdmin?: boolean;
  importing: boolean;
  loading: boolean;
  loadingRepositories: Dict<boolean>;
  onImportRepository: () => void;
  onOpenProject: (key: string) => void;
  onPersonalAccessTokenCreate: (token: string) => void;
  onSearch: (query: string) => void;
  onSelectRepository: (repository: AzureRepository) => void;
  projects?: AzureProject[];
  repositories: Dict<AzureRepository[]>;
  searching?: boolean;
  searchResults?: AzureRepository[];
  searchQuery?: string;
  selectedRepository?: AzureRepository;
  almInstances?: AlmSettingsInstance[];
  selectedAlmInstance?: AlmSettingsInstance;
  showPersonalAccessTokenForm?: boolean;
  submittingToken?: boolean;
  tokenValidationFailed: boolean;
  onSelectedAlmInstanceChange: (instance: AlmSettingsInstance) => void;
  firstConnection?: boolean;
}

export default function AzureProjectCreateRenderer(props: AzureProjectCreateRendererProps) {
  const {
    canAdmin,
    importing,
    loading,
    loadingRepositories,
    projects,
    repositories,
    searching,
    searchResults,
    searchQuery,
    selectedRepository,
    almInstances,
    showPersonalAccessTokenForm,
    submittingToken,
    tokenValidationFailed,
    selectedAlmInstance,
    firstConnection,
  } = props;

  const showCountError = !loading && (!almInstances || almInstances?.length === 0);
  const settingIsValid = selectedAlmInstance && selectedAlmInstance.url;
  const showUrlError = !loading && selectedAlmInstance && !selectedAlmInstance.url;

  return (
    <>
      <CreateProjectPageHeader
        additionalActions={
          !showPersonalAccessTokenForm &&
          settingIsValid && (
            <div className="display-flex-center pull-right">
              <DeferredSpinner className="spacer-right" loading={importing} />
              <Button
                className="button-large button-primary"
                disabled={!selectedRepository || importing}
                onClick={props.onImportRepository}
              >
                {translate('onboarding.create_project.import_selected_repo')}
              </Button>
            </div>
          )
        }
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

      <AlmSettingsInstanceDropdown
        almKey={AlmKeys.Azure}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onChangeConfig={props.onSelectedAlmInstanceChange}
      />

      {loading && <i className="spinner" />}

      {showUrlError && (
        <Alert variant="error">
          {canAdmin ? (
            <FormattedMessage
              defaultMessage={translate('onboarding.create_project.azure.no_url.admin')}
              id="onboarding.create_project.azure.no_url.admin"
              values={{
                alm: translate('onboarding.alm', AlmKeys.Azure),
                url: (
                  <Link to={getGlobalSettingsUrl(ALM_INTEGRATION_CATEGORY)}>
                    {translate('settings.page')}
                  </Link>
                ),
              }}
            />
          ) : (
            translate('onboarding.create_project.azure.no_url')
          )}
        </Alert>
      )}

      {showCountError && <WrongBindingCountAlert alm={AlmKeys.Azure} canAdmin={!!canAdmin} />}

      {!loading &&
        selectedAlmInstance &&
        selectedAlmInstance.url &&
        (showPersonalAccessTokenForm ? (
          <div>
            <AzurePersonalAccessTokenForm
              almSetting={selectedAlmInstance}
              onPersonalAccessTokenCreate={props.onPersonalAccessTokenCreate}
              submitting={submittingToken}
              validationFailed={tokenValidationFailed}
              firstConnection={firstConnection}
            />
          </div>
        ) : (
          <>
            <div className="huge-spacer-bottom">
              <SearchBox
                onChange={props.onSearch}
                placeholder={translate('onboarding.create_project.search_projects_repositories')}
              />
            </div>
            <DeferredSpinner loading={Boolean(searching)}>
              <AzureProjectsList
                importing={importing}
                loadingRepositories={loadingRepositories}
                onOpenProject={props.onOpenProject}
                onSelectRepository={props.onSelectRepository}
                projects={projects}
                repositories={repositories}
                searchResults={searchResults}
                searchQuery={searchQuery}
                selectedRepository={selectedRepository}
              />
            </DeferredSpinner>
          </>
        ))}
    </>
  );
}
