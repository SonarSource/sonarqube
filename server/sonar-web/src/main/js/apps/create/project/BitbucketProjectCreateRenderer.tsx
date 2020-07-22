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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import {
  BitbucketProject,
  BitbucketProjectRepositories,
  BitbucketRepository
} from '../../../types/alm-integration';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import BitbucketImportRepositoryForm from './BitbucketImportRepositoryForm';
import CreateProjectPageHeader from './CreateProjectPageHeader';
import PersonalAccessTokenForm from './PersonalAccessTokenForm';
import WrongBindingCountAlert from './WrongBindingCountAlert';

export interface BitbucketProjectCreateRendererProps {
  bitbucketSetting?: AlmSettingsInstance;
  canAdmin?: boolean;
  importing: boolean;
  loading: boolean;
  onImportRepository: () => void;
  onSearch: (query: string) => void;
  onSelectRepository: (repo: BitbucketRepository) => void;
  onPersonalAccessTokenCreate: (token: string) => void;
  onProjectCreate: (projectKeys: string[]) => void;
  projects?: BitbucketProject[];
  projectRepositories?: BitbucketProjectRepositories;
  searching: boolean;
  searchResults?: BitbucketRepository[];
  selectedRepository?: BitbucketRepository;
  showPersonalAccessTokenForm?: boolean;
  submittingToken?: boolean;
  tokenValidationFailed: boolean;
}

export default function BitbucketProjectCreateRenderer(props: BitbucketProjectCreateRendererProps) {
  const {
    bitbucketSetting,
    canAdmin,
    importing,
    loading,
    projects,
    projectRepositories,
    selectedRepository,
    searching,
    searchResults,
    showPersonalAccessTokenForm,
    submittingToken,
    tokenValidationFailed
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
                onClick={props.onImportRepository}>
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

      {loading && <i className="spinner" />}

      {!loading && !bitbucketSetting && (
        <WrongBindingCountAlert alm={AlmKeys.Bitbucket} canAdmin={!!canAdmin} />
      )}

      {!loading &&
        bitbucketSetting &&
        (showPersonalAccessTokenForm ? (
          <PersonalAccessTokenForm
            almSetting={bitbucketSetting}
            onPersonalAccessTokenCreate={props.onPersonalAccessTokenCreate}
            submitting={submittingToken}
            validationFailed={tokenValidationFailed}
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
