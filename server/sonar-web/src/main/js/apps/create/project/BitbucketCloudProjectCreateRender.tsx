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
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { BitbucketCloudRepository } from '../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import AlmSettingsInstanceDropdown from './AlmSettingsInstanceDropdown';
import BitbucketCloudSearchForm from './BitbucketCloudSearchForm';
import CreateProjectPageHeader from './CreateProjectPageHeader';
import PersonalAccessTokenForm from './PersonalAccessTokenForm';
import WrongBindingCountAlert from './WrongBindingCountAlert';

export interface BitbucketCloudProjectCreateRendererProps {
  importingSlug?: string;
  isLastPage: boolean;
  canAdmin?: boolean;
  loading: boolean;
  loadingMore: boolean;
  onImport: (repositorySlug: string) => void;
  onLoadMore: () => void;
  onPersonalAccessTokenCreated: () => void;
  onSearch: (searchQuery: string) => void;
  onSelectedAlmInstanceChange: (instance: AlmSettingsInstance) => void;
  repositories?: BitbucketCloudRepository[];
  resetPat: boolean;
  searching: boolean;
  searchQuery: string;
  showPersonalAccessTokenForm: boolean;
  almInstances: AlmSettingsInstance[];
  selectedAlmInstance?: AlmSettingsInstance;
}

export default function BitbucketCloudProjectCreateRenderer(
  props: BitbucketCloudProjectCreateRendererProps
) {
  const {
    almInstances,
    importingSlug,
    isLastPage,
    selectedAlmInstance,
    canAdmin,
    loading,
    loadingMore,
    repositories,
    resetPat,
    searching,
    searchQuery,
    showPersonalAccessTokenForm,
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
              src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
            />
            {translate('onboarding.create_project.bitbucketcloud.title')}
          </span>
        }
      />

      <AlmSettingsInstanceDropdown
        almKey={AlmKeys.BitbucketCloud}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onChangeConfig={props.onSelectedAlmInstanceChange}
      />

      {loading && <i className="spinner" />}

      {!loading && !selectedAlmInstance && (
        <WrongBindingCountAlert alm={AlmKeys.BitbucketCloud} canAdmin={!!canAdmin} />
      )}

      {!loading &&
        selectedAlmInstance &&
        (showPersonalAccessTokenForm ? (
          <PersonalAccessTokenForm
            almSetting={selectedAlmInstance}
            resetPat={resetPat}
            onPersonalAccessTokenCreated={props.onPersonalAccessTokenCreated}
          />
        ) : (
          <BitbucketCloudSearchForm
            importingSlug={importingSlug}
            isLastPage={isLastPage}
            loadingMore={loadingMore}
            searchQuery={searchQuery}
            searching={searching}
            onImport={props.onImport}
            onSearch={props.onSearch}
            onLoadMore={props.onLoadMore}
            repositories={repositories}
          />
        ))}
    </>
  );
}
