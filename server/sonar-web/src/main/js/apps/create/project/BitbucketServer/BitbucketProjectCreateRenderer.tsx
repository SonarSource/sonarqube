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
import { LightPrimary, PageContentFontWrapper, Spinner, Title } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import {
  BitbucketProject,
  BitbucketProjectRepositories,
  BitbucketRepository,
} from '../../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import AlmSettingsInstanceDropdown from '../components/AlmSettingsInstanceDropdown';
import WrongBindingCountAlert from '../components/WrongBindingCountAlert';
import BitbucketImportRepositoryForm from './BitbucketImportRepositoryForm';
import BitbucketServerPersonalAccessTokenForm from './BitbucketServerPersonalAccessTokenForm';

export interface BitbucketProjectCreateRendererProps {
  selectedAlmInstance?: AlmSettingsInstance;
  almInstances: AlmSettingsInstance[];
  canAdmin?: boolean;
  loading: boolean;
  onImportRepository: (repository: BitbucketRepository) => void;
  onSearch: (query: string) => void;
  onPersonalAccessTokenCreated: () => void;
  onSelectedAlmInstanceChange: (instance: AlmSettingsInstance) => void;
  projects?: BitbucketProject[];
  projectRepositories?: BitbucketProjectRepositories;
  resetPat: boolean;
  searching: boolean;
  searchResults?: BitbucketRepository[];
  showPersonalAccessTokenForm?: boolean;
}

export default function BitbucketProjectCreateRenderer(props: BitbucketProjectCreateRendererProps) {
  const {
    almInstances,
    selectedAlmInstance,
    canAdmin,
    loading,
    projects,
    projectRepositories,

    searching,
    searchResults,
    showPersonalAccessTokenForm,
    resetPat,
  } = props;

  return (
    <PageContentFontWrapper>
      <header className="sw-mb-10">
        <Title className="sw-mb-4">{translate('onboarding.create_project.bitbucket.title')}</Title>
        <LightPrimary className="sw-body-sm">
          {translate('onboarding.create_project.bitbucket.subtitle')}
        </LightPrimary>
      </header>

      <AlmSettingsInstanceDropdown
        almKey={AlmKeys.BitbucketServer}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onChangeConfig={props.onSelectedAlmInstanceChange}
      />

      <Spinner loading={loading}>
        {!loading && !selectedAlmInstance && (
          <WrongBindingCountAlert alm={AlmKeys.BitbucketServer} canAdmin={!!canAdmin} />
        )}

        {!loading &&
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
