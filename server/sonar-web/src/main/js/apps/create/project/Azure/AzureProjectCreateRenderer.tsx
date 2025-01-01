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

import { Link, Spinner } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import {
  FlagMessage,
  InputSearch,
  LightPrimary,
  PageContentFontWrapper,
  Title,
} from '~design-system';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { useAppState } from '../../../../app/components/app-state/withAppStateContext';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import { translate } from '../../../../helpers/l10n';
import { getGlobalSettingsUrl } from '../../../../helpers/urls';
import { AzureProject, AzureRepository } from '../../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { Dict } from '../../../../types/types';
import { ALM_INTEGRATION_CATEGORY } from '../../../settings/constants';
import AlmSettingsInstanceDropdown from '../components/AlmSettingsInstanceDropdown';
import WrongBindingCountAlert from '../components/WrongBindingCountAlert';
import { CreateProjectModes } from '../types';
import AzurePersonalAccessTokenForm from './AzurePersonalAccessTokenForm';
import AzureProjectsList from './AzureProjectsList';

export interface AzureProjectCreateRendererProps {
  almInstances?: AlmSettingsInstance[];
  loading: boolean;
  loadingRepositories: Dict<boolean>;
  onImportRepository: (resository: AzureRepository) => void;
  onOpenProject: (key: string) => void;
  onPersonalAccessTokenCreate: () => void;
  onSearch: (query: string) => void;
  onSelectedAlmInstanceChange: (instance: AlmSettingsInstance) => void;
  projects?: AzureProject[];
  repositories?: Dict<AzureRepository[]>;
  resetPat: boolean;
  searchQuery?: string;
  searchResults?: AzureRepository[];
  searching?: boolean;
  selectedAlmInstance?: AlmSettingsInstance;
  showPersonalAccessTokenForm?: boolean;
}

export default function AzureProjectCreateRenderer(
  props: Readonly<AzureProjectCreateRendererProps>,
) {
  const {
    loading,
    loadingRepositories,
    projects,
    repositories,
    searching,
    searchResults,
    searchQuery,
    almInstances,
    showPersonalAccessTokenForm,
    resetPat,
    selectedAlmInstance,
  } = props;

  const isMonorepoSupported = React.useContext(AvailableFeaturesContext).includes(
    Feature.MonoRepositoryPullRequestDecoration,
  );

  const { canAdmin } = useAppState();

  const showCountError = !loading && (!almInstances || almInstances.length === 0);
  const showUrlError =
    !loading && selectedAlmInstance !== undefined && selectedAlmInstance.url === undefined;

  return (
    <PageContentFontWrapper>
      <header className="sw-mb-10">
        <Title className="sw-mb-4">{translate('onboarding.create_project.azure.title')}</Title>
        <LightPrimary className="sw-typo-default">
          {isMonorepoSupported ? (
            <FormattedMessage
              id="onboarding.create_project.azure.subtitle.with_monorepo"
              values={{
                monorepoSetupLink: (
                  <Link
                    to={{
                      pathname: '/projects/create',
                      search: queryToSearchString({
                        mode: CreateProjectModes.AzureDevOps,
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
            <FormattedMessage id="onboarding.create_project.azure.subtitle" />
          )}
        </LightPrimary>
      </header>

      <AlmSettingsInstanceDropdown
        almKey={AlmKeys.Azure}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onChangeConfig={props.onSelectedAlmInstanceChange}
      />

      <Spinner isLoading={loading} />

      {showUrlError && (
        <FlagMessage variant="error" className="sw-mb-2">
          <span>
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
          </span>
        </FlagMessage>
      )}

      {showCountError && <WrongBindingCountAlert alm={AlmKeys.Azure} />}

      {!loading &&
        selectedAlmInstance?.url &&
        (showPersonalAccessTokenForm ? (
          <div>
            <AzurePersonalAccessTokenForm
              almSetting={selectedAlmInstance}
              onPersonalAccessTokenCreate={props.onPersonalAccessTokenCreate}
              resetPat={resetPat}
            />
          </div>
        ) : (
          <>
            <div className="sw-mb-10 sw-w-abs-400">
              <InputSearch
                onChange={props.onSearch}
                placeholder={translate('onboarding.create_project.search_projects_repositories')}
                size="full"
              />
            </div>
            <Spinner isLoading={Boolean(searching)}>
              <AzureProjectsList
                loadingRepositories={loadingRepositories}
                onOpenProject={props.onOpenProject}
                onImportRepository={props.onImportRepository}
                projects={projects}
                repositories={repositories}
                searchResults={searchResults}
                searchQuery={searchQuery}
              />
            </Spinner>
          </>
        ))}
    </PageContentFontWrapper>
  );
}
