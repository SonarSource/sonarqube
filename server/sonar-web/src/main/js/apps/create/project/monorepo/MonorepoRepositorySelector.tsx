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

import { LinkHighlight, LinkStandalone } from '@sonarsource/echoes-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { GroupBase } from 'react-select';
import { DarkLabel, FlagMessage, InputSelect } from '~design-system';
import { LabelValueSelectOption } from '../../../../helpers/search';
import { getProjectUrl } from '../../../../helpers/urls';
import { AlmKeys } from '../../../../types/alm-settings';

interface Props {
  almKey: AlmKeys;
  alreadyBoundProjects: {
    projectId: string;
    projectName: string;
  }[];
  error: boolean;
  isFetchingAlreadyBoundProjects: boolean;
  isLoadingAlreadyBoundProjects: boolean;
  loadingRepositories: boolean;
  onSearchRepositories: (query: string) => void;
  onSelectRepository: (repositoryKey: string) => void;
  repositoryOptions?: LabelValueSelectOption[] | GroupBase<LabelValueSelectOption>[];
  repositorySearchQuery: string;
  selectedOrganization?: LabelValueSelectOption;
  selectedRepository?: LabelValueSelectOption;
  showOrganizations?: boolean;
}

export function MonorepoRepositorySelector({
  almKey,
  alreadyBoundProjects,
  error,
  isFetchingAlreadyBoundProjects,
  isLoadingAlreadyBoundProjects,
  loadingRepositories,
  onSearchRepositories,
  onSelectRepository,
  repositorySearchQuery,
  repositoryOptions,
  selectedOrganization,
  selectedRepository,
  showOrganizations,
}: Readonly<Props>) {
  const { formatMessage } = useIntl();

  const repositorySelectorEnabled =
    !error &&
    !loadingRepositories &&
    ((showOrganizations && !!selectedOrganization) || !showOrganizations);
  const showWarningMessage =
    error ||
    (repositorySelectorEnabled &&
      repositoryOptions &&
      repositoryOptions.length === 0 &&
      repositorySearchQuery === '');

  return (
    <>
      <DarkLabel htmlFor={`${almKey}-monorepo-choose-repository`} className="sw-mb-2">
        <FormattedMessage id="onboarding.create_project.monorepo.choose_repository" />
      </DarkLabel>
      {showWarningMessage ? (
        <FormattedMessage
          id="onboarding.create_project.monorepo.no_projects"
          defaultMessage={formatMessage({ id: 'onboarding.create_project.monorepo.no_projects' })}
          values={{
            almKey: formatMessage({ id: `alm.${almKey}` }),
          }}
        />
      ) : (
        <>
          <InputSelect
            inputId={`${almKey}-monorepo-choose-repository`}
            inputValue={repositorySearchQuery}
            isLoading={loadingRepositories}
            isSearchable
            noOptionsMessage={() => formatMessage({ id: 'no_results' })}
            onChange={({ value }: LabelValueSelectOption) => {
              onSelectRepository(value);
            }}
            onInputChange={onSearchRepositories}
            options={repositoryOptions}
            placeholder={formatMessage({
              id: `onboarding.create_project.monorepo.choose_repository.placeholder`,
            })}
            size="full"
            value={selectedRepository}
          />
          {selectedRepository &&
            !isLoadingAlreadyBoundProjects &&
            !isFetchingAlreadyBoundProjects && (
              <FlagMessage className="sw-mt-2" variant="info">
                {alreadyBoundProjects.length === 0 ? (
                  <FormattedMessage id="onboarding.create_project.monorepo.choose_repository.no_already_bound_projects" />
                ) : (
                  <div>
                    <FormattedMessage id="onboarding.create_project.monorepo.choose_repository.existing_already_bound_projects" />
                    <ul className="sw-mt-4">
                      {alreadyBoundProjects.map(({ projectId, projectName }) => (
                        <li key={projectId}>
                          <LinkStandalone
                            to={getProjectUrl(projectId)}
                            highlight={LinkHighlight.Subdued}
                          >
                            {projectName}
                          </LinkStandalone>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </FlagMessage>
            )}
        </>
      )}
    </>
  );
}
