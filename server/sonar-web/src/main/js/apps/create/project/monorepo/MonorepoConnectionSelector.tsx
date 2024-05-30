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

import { Title } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { GroupBase } from 'react-select';
import { LabelValueSelectOption } from '../../../../helpers/search';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import DopSettingDropdown from '../components/DopSettingDropdown';
import MonorepoNoOrganisations from './MonorepoNoOrganisations';
import { MonorepoOrganisationSelector } from './MonorepoOrganisationSelector';
import { MonorepoRepositorySelector } from './MonorepoRepositorySelector';

interface Props {
  almKey: AlmKeys;
  alreadyBoundProjects: {
    projectId: string;
    projectName: string;
  }[];
  dopSettings: DopSetting[];
  error: boolean;
  isFetchingAlreadyBoundProjects: boolean;
  isLoadingAlreadyBoundProjects: boolean;
  loadingBindings: boolean;
  loadingOrganizations?: boolean;
  loadingRepositories: boolean;
  onSearchRepositories: (query: string) => void;
  onSelectDopSetting: (instance: DopSetting) => void;
  onSelectOrganization?: (organizationKey: string) => void;
  onSelectRepository: (repositoryKey: string) => void;
  organizationOptions?: LabelValueSelectOption[];
  personalAccessTokenComponent?: React.ReactNode;
  repositoryOptions?: LabelValueSelectOption[] | GroupBase<LabelValueSelectOption>[];
  repositorySearchQuery: string;
  selectedDopSetting?: DopSetting;
  selectedOrganization?: LabelValueSelectOption;
  selectedRepository?: LabelValueSelectOption;
  showPersonalAccessToken?: boolean;
  showOrganizations?: boolean;
}

export function MonorepoConnectionSelector({
  almKey,
  alreadyBoundProjects,
  dopSettings,
  error,
  isFetchingAlreadyBoundProjects,
  isLoadingAlreadyBoundProjects,
  loadingOrganizations,
  loadingRepositories,
  onSearchRepositories,
  onSelectDopSetting,
  onSelectOrganization,
  onSelectRepository,
  organizationOptions,
  personalAccessTokenComponent,
  repositoryOptions,
  repositorySearchQuery,
  selectedDopSetting,
  selectedOrganization,
  selectedRepository,
  showPersonalAccessToken,
  showOrganizations,
}: Readonly<Props>) {
  return (
    <div className="sw-flex sw-flex-col sw-gap-6">
      <Title>
        <FormattedMessage
          id={
            showOrganizations
              ? 'onboarding.create_project.monorepo.choose_organization_and_repository'
              : 'onboarding.create_project.monorepo.choose_repository'
          }
        />
      </Title>

      <DopSettingDropdown
        almKey={almKey}
        dopSettings={dopSettings}
        selectedDopSetting={selectedDopSetting}
        onChangeSetting={onSelectDopSetting}
      />

      {showPersonalAccessToken ? (
        personalAccessTokenComponent
      ) : (
        <>
          {showOrganizations && error && selectedDopSetting && !loadingOrganizations && (
            <MonorepoNoOrganisations almKey={almKey} />
          )}

          {showOrganizations && organizationOptions && (
            <div className="sw-flex sw-flex-col">
              <MonorepoOrganisationSelector
                almKey={almKey}
                error={error}
                organizationOptions={organizationOptions}
                loadingOrganizations={loadingOrganizations}
                onSelectOrganization={onSelectOrganization}
                selectedOrganization={selectedOrganization}
              />
            </div>
          )}

          <div className="sw-flex sw-flex-col">
            <MonorepoRepositorySelector
              almKey={almKey}
              alreadyBoundProjects={alreadyBoundProjects}
              error={error}
              isFetchingAlreadyBoundProjects={isFetchingAlreadyBoundProjects}
              isLoadingAlreadyBoundProjects={isLoadingAlreadyBoundProjects}
              loadingRepositories={loadingRepositories}
              onSelectRepository={onSelectRepository}
              onSearchRepositories={onSearchRepositories}
              repositoryOptions={repositoryOptions}
              repositorySearchQuery={repositorySearchQuery}
              selectedOrganization={selectedOrganization}
              selectedRepository={selectedRepository}
              showOrganizations={showOrganizations}
            />
          </div>
        </>
      )}
    </div>
  );
}
