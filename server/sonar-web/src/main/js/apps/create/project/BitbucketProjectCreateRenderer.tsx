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
import { Button } from '../../../components/controls/buttons';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import {
  BitbucketProject,
  BitbucketProjectRepositories,
  BitbucketRepository,
} from '../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import AlmSettingsInstanceDropdown from './AlmSettingsInstanceDropdown';
import BitbucketImportRepositoryForm from './BitbucketImportRepositoryForm';
import CreateProjectPageHeader from './CreateProjectPageHeader';
import PersonalAccessTokenForm from './PersonalAccessTokenForm';
import WrongBindingCountAlert from './WrongBindingCountAlert';

export interface BitbucketProjectCreateRendererProps {
  selectedAlmInstance?: AlmSettingsInstance;
  almInstances: AlmSettingsInstance[];
  canAdmin?: boolean;
  importing: boolean;
  loading: boolean;
  onImportRepository: () => void;
  onSearch: (query: string) => void;
  onSelectRepository: (repo: BitbucketRepository) => void;
  onPersonalAccessTokenCreated: () => void;
  onSelectedAlmInstanceChange: (instance: AlmSettingsInstance) => void;
  projects?: BitbucketProject[];
  projectRepositories?: BitbucketProjectRepositories;
  resetPat: boolean;
  searching: boolean;
  searchResults?: BitbucketRepository[];
  selectedRepository?: BitbucketRepository;
  showPersonalAccessTokenForm?: boolean;
}

export default function BitbucketProjectCreateRenderer(props: BitbucketProjectCreateRendererProps) {
  const {
    almInstances,
    selectedAlmInstance,
    canAdmin,
    importing,
    loading,
    projects,
    projectRepositories,
    selectedRepository,
    searching,
    searchResults,
    showPersonalAccessTokenForm,
    resetPat,
  } = props;

  return (
    <>
      <CreateProjectPageHeader
        additionalActions={
          !showPersonalAccessTokenForm && (
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
              src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
            />
            {translate('onboarding.create_project.from_bbs')}
          </span>
        }
      />

      <AlmSettingsInstanceDropdown
        almKey={AlmKeys.BitbucketServer}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onChangeConfig={props.onSelectedAlmInstanceChange}
      />

      {loading && <i className="spinner" />}

      {!loading && !selectedAlmInstance && (
        <WrongBindingCountAlert alm={AlmKeys.BitbucketServer} canAdmin={!!canAdmin} />
      )}

      {!loading &&
        selectedAlmInstance &&
        (showPersonalAccessTokenForm ? (
          <PersonalAccessTokenForm
            almSetting={selectedAlmInstance}
            onPersonalAccessTokenCreated={props.onPersonalAccessTokenCreated}
            resetPat={resetPat}
          />
        ) : (
          <BitbucketImportRepositoryForm
            disableRepositories={importing}
            onSearch={props.onSearch}
            onSelectRepository={props.onSelectRepository}
            projectRepositories={projectRepositories}
            projects={projects}
            searchResults={searchResults}
            searching={searching}
            selectedRepository={selectedRepository}
          />
        ))}
    </>
  );
}
