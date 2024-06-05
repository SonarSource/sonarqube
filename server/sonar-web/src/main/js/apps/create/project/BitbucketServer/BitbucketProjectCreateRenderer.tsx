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
import { LightPrimary, PageContentFontWrapper, Title } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import { translate } from '../../../../helpers/l10n';
import { BitbucketProject, BitbucketRepository } from '../../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { Dict } from '../../../../types/types';
import AlmSettingsInstanceDropdown from '../components/AlmSettingsInstanceDropdown';
import WrongBindingCountAlert from '../components/WrongBindingCountAlert';
import { CreateProjectModes } from '../types';
import BitbucketImportRepositoryForm from './BitbucketImportRepositoryForm';
import BitbucketServerPersonalAccessTokenForm from './BitbucketServerPersonalAccessTokenForm';

export interface BitbucketProjectCreateRendererProps {
  almInstances: AlmSettingsInstance[];
  isLoading: boolean;
  onImportRepository: (repository: BitbucketRepository) => void;
  onPersonalAccessTokenCreated: () => void;
  onSearch: (query: string) => void;
  onSelectedAlmInstanceChange: (instance: AlmSettingsInstance) => void;
  projectRepositories?: Dict<BitbucketRepository[]>;
  projects?: BitbucketProject[];
  resetPat: boolean;
  searchResults?: BitbucketRepository[];
  searching: boolean;
  selectedAlmInstance?: AlmSettingsInstance;
  showPersonalAccessTokenForm?: boolean;
}

export default function BitbucketProjectCreateRenderer(
  props: Readonly<BitbucketProjectCreateRendererProps>,
) {
  const {
    almInstances,
    isLoading,
    projects,
    projectRepositories,
    resetPat,
    searching,
    searchResults,
    selectedAlmInstance,
    showPersonalAccessTokenForm,
  } = props;

  const isMonorepoSupported = React.useContext(AvailableFeaturesContext).includes(
    Feature.MonoRepositoryPullRequestDecoration,
  );

  return (
    <PageContentFontWrapper>
      <header className="sw-mb-10">
        <Title className="sw-mb-4">{translate('onboarding.create_project.bitbucket.title')}</Title>
        <LightPrimary className="sw-body-sm">
          {isMonorepoSupported ? (
            <FormattedMessage
              id="onboarding.create_project.bitbucket.subtitle.with_monorepo"
              values={{
                monorepoSetupLink: (
                  <Link
                    to={{
                      pathname: '/projects/create',
                      search: queryToSearchString({
                        mode: CreateProjectModes.BitbucketServer,
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
            <FormattedMessage id="onboarding.create_project.bitbucket.subtitle" />
          )}
        </LightPrimary>
      </header>

      <AlmSettingsInstanceDropdown
        almKey={AlmKeys.BitbucketServer}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onChangeConfig={props.onSelectedAlmInstanceChange}
      />

      <Spinner isLoading={isLoading}>
        {!isLoading && almInstances && almInstances.length === 0 && !selectedAlmInstance && (
          <WrongBindingCountAlert alm={AlmKeys.BitbucketServer} />
        )}

        {!isLoading &&
          selectedAlmInstance &&
          (showPersonalAccessTokenForm ? (
            <BitbucketServerPersonalAccessTokenForm
              almSetting={selectedAlmInstance}
              onPersonalAccessTokenCreated={props.onPersonalAccessTokenCreated}
              resetPat={resetPat}
            />
          ) : (
            <BitbucketImportRepositoryForm
              onSearch={props.onSearch}
              projectRepositories={projectRepositories}
              projects={projects}
              searchResults={searchResults}
              searching={searching}
              onImportRepository={props.onImportRepository}
            />
          ))}
      </Spinner>
    </PageContentFontWrapper>
  );
}
