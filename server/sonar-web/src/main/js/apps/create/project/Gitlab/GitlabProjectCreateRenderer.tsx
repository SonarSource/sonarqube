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
import { LightPrimary, Title } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import { translate } from '../../../../helpers/l10n';
import { queryToSearch } from '../../../../helpers/urls';
import { GitlabProject } from '../../../../types/alm-integration';
import { AlmInstanceBase, AlmKeys, AlmSettingsInstance } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { Paging } from '../../../../types/types';
import AlmSettingsInstanceDropdown from '../components/AlmSettingsInstanceDropdown';
import WrongBindingCountAlert from '../components/WrongBindingCountAlert';
import { CreateProjectModes } from '../types';
import GitlabPersonalAccessTokenForm from './GItlabPersonalAccessTokenForm';
import GitlabProjectSelectionForm from './GitlabProjectSelectionForm';

export interface GitlabProjectCreateRendererProps {
  canAdmin?: boolean;
  loading: boolean;
  loadingMore: boolean;
  onImport: (gitlabProjectId: string) => void;
  onLoadMore: () => void;
  onPersonalAccessTokenCreated: () => void;
  onSearch: (searchQuery: string) => void;
  projects?: GitlabProject[];
  projectsPaging: Paging;
  resetPat: boolean;
  searching: boolean;
  searchQuery: string;
  almInstances?: AlmSettingsInstance[];
  selectedAlmInstance?: AlmSettingsInstance;
  showPersonalAccessTokenForm?: boolean;
  onSelectedAlmInstanceChange: (instance: AlmInstanceBase) => void;
}

export default function GitlabProjectCreateRenderer(
  props: Readonly<GitlabProjectCreateRendererProps>,
) {
  const isMonorepoSupported = React.useContext(AvailableFeaturesContext).includes(
    Feature.MonoRepositoryPullRequestDecoration,
  );

  const {
    canAdmin,
    loading,
    loadingMore,
    projects,
    projectsPaging,
    resetPat,
    searching,
    searchQuery,
    selectedAlmInstance,
    almInstances,
    showPersonalAccessTokenForm,
  } = props;

  return (
    <>
      <header className="sw-mb-10">
        <Title className="sw-mb-4">{translate('onboarding.create_project.gitlab.title')}</Title>
        <LightPrimary className="sw-body-sm">
          {isMonorepoSupported ? (
            <FormattedMessage
              id="onboarding.create_project.gitlab.subtitle.with_monorepo"
              values={{
                monorepoSetupLink: (
                  <Link
                    to={{
                      pathname: '/projects/create',
                      search: queryToSearch({
                        mode: CreateProjectModes.GitLab,
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
            <FormattedMessage id="onboarding.create_project.gitlab.subtitle" />
          )}
        </LightPrimary>
      </header>

      <AlmSettingsInstanceDropdown
        almKey={AlmKeys.GitLab}
        almInstances={almInstances}
        selectedAlmInstance={selectedAlmInstance}
        onChangeConfig={props.onSelectedAlmInstanceChange}
      />

      <Spinner isLoading={loading} />

      {!loading && !selectedAlmInstance && (
        <WrongBindingCountAlert alm={AlmKeys.GitLab} canAdmin={!!canAdmin} />
      )}

      {!loading &&
        selectedAlmInstance &&
        (showPersonalAccessTokenForm ? (
          <GitlabPersonalAccessTokenForm
            almSetting={selectedAlmInstance}
            resetPat={resetPat}
            onPersonalAccessTokenCreated={props.onPersonalAccessTokenCreated}
          />
        ) : (
          <GitlabProjectSelectionForm
            loadingMore={loadingMore}
            onImport={props.onImport}
            onLoadMore={props.onLoadMore}
            onSearch={props.onSearch}
            projects={projects}
            projectsPaging={projectsPaging}
            searching={searching}
            searchQuery={searchQuery}
          />
        ))}
    </>
  );
}
