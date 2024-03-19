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
import {
  AddNewIcon,
  BlueGreySeparator,
  ButtonPrimary,
  ButtonSecondary,
  DarkLabel,
  FlagMessage,
  InputSelect,
  SubTitle,
  Title,
} from 'design-system';
import React, { useEffect, useRef } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useLocation, useRouter } from '../../../../components/hoc/withRouter';
import { translate } from '../../../../helpers/l10n';
import { LabelValueSelectOption } from '../../../../helpers/search';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { ImportProjectParam } from '../CreateProjectPage';
import DopSettingDropdown from '../components/DopSettingDropdown';
import { ProjectData, ProjectValidationCard } from '../components/ProjectValidation';
import { CreateProjectModes } from '../types';
import { getSanitizedProjectKey } from '../utils';
import { MonorepoProjectHeader } from './MonorepoProjectHeader';

interface MonorepoProjectCreateProps {
  canAdmin: boolean;
  dopSettings: DopSetting[];
  error: boolean;
  loadingBindings: boolean;
  loadingOrganizations: boolean;
  loadingRepositories: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
  onSearchRepositories: (query: string) => void;
  onSelectDopSetting: (instance: DopSetting) => void;
  onSelectOrganization: (organizationKey: string) => void;
  onSelectRepository: (repositoryIdentifier: string) => void;
  organizationOptions?: LabelValueSelectOption[];
  repositoryOptions?: LabelValueSelectOption[];
  repositorySearchQuery: string;
  selectedDopSetting?: DopSetting;
  selectedOrganization?: LabelValueSelectOption;
  selectedRepository?: LabelValueSelectOption;
}

type ProjectItem = Required<ProjectData<number>>;

export default function MonorepoProjectCreate(props: Readonly<MonorepoProjectCreateProps>) {
  const {
    dopSettings,
    canAdmin,
    error,
    loadingBindings,
    loadingOrganizations,
    loadingRepositories,
    onProjectSetupDone,
    onSearchRepositories,
    onSelectDopSetting,
    onSelectOrganization,
    onSelectRepository,
    organizationOptions,
    repositoryOptions,
    repositorySearchQuery,
    selectedDopSetting,
    selectedOrganization,
    selectedRepository,
  } = props;

  const projectCounter = useRef(0);

  const [projects, setProjects] = React.useState<ProjectItem[]>([]);

  const location = useLocation();
  const { push } = useRouter();
  const { formatMessage } = useIntl();

  const projectKeys = React.useMemo(() => projects.map(({ key }) => key), [projects]);

  const almKey = location.query.mode as AlmKeys;

  const isSetupInvalid =
    selectedDopSetting === undefined ||
    selectedOrganization === undefined ||
    selectedRepository === undefined ||
    projects.length === 0 ||
    projects.some(({ hasError, key, name }) => hasError || key === '' || name === '');

  const addProject = () => {
    if (selectedOrganization === undefined || selectedRepository === undefined) {
      return;
    }

    const id = projectCounter.current;
    projectCounter.current += 1;

    const projectKeySuffix = id === 0 ? '' : `-${id}`;
    const projectKey = getSanitizedProjectKey(
      `${selectedOrganization.label}_${selectedRepository.label}_add-your-reference${projectKeySuffix}`,
    );

    const newProjects = [
      ...projects,
      {
        hasError: false,
        id,
        key: projectKey,
        name: projectKey,
        touched: false,
      },
    ];

    setProjects(newProjects);
  };

  const onProjectChange = (project: ProjectItem) => {
    const newProjects = projects.filter(({ id }) => id !== project.id);
    newProjects.push({
      ...project,
    });
    newProjects.sort((a, b) => a.id - b.id);

    setProjects(newProjects);
  };

  const onProjectRemove = (id: number) => {
    const newProjects = projects.filter(({ id: projectId }) => projectId !== id);

    setProjects(newProjects);
  };

  const cancelMonorepoSetup = () => {
    push({
      pathname: location.pathname,
      query: { mode: AlmKeys.GitHub },
    });
  };

  const submitProjects = () => {
    if (isSetupInvalid) {
      return;
    }

    const monorepoSetup: ImportProjectParam = {
      creationMode: almKey as unknown as CreateProjectModes,
      devOpsPlatformSettingId: selectedDopSetting.id,
      monorepo: true,
      projects: projects.map(({ key: projectKey, name: projectName }) => ({
        projectKey,
        projectName,
      })),
      repositoryIdentifier: selectedRepository.value,
    };

    onProjectSetupDone(monorepoSetup);
  };

  useEffect(() => {
    if (selectedRepository !== undefined && projects.length === 0) {
      addProject();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRepository]);

  if (loadingBindings) {
    return <Spinner />;
  }

  return (
    <div>
      <MonorepoProjectHeader />

      <BlueGreySeparator className="sw-my-5" />

      <div className="sw-flex sw-flex-col sw-gap-6">
        <Title>
          <FormattedMessage
            id={`onboarding.create_project.monorepo.choose_organization_and_repository.${almKey}`}
          />
        </Title>

        <DopSettingDropdown
          almKey={almKey}
          dopSettings={dopSettings}
          selectedDopSetting={selectedDopSetting}
          onChangeSetting={onSelectDopSetting}
        />

        {error && selectedDopSetting && !loadingOrganizations && (
          <FlagMessage variant="warning">
            <span>
              {canAdmin ? (
                <FormattedMessage
                  id="onboarding.create_project.github.warning.message_admin"
                  defaultMessage={translate(
                    'onboarding.create_project.github.warning.message_admin',
                  )}
                  values={{
                    link: (
                      <Link to="/admin/settings?category=almintegration">
                        {translate('onboarding.create_project.github.warning.message_admin.link')}
                      </Link>
                    ),
                  }}
                />
              ) : (
                translate('onboarding.create_project.github.warning.message')
              )}
            </span>
          </FlagMessage>
        )}

        <div className="sw-flex sw-flex-col">
          <Spinner isLoading={loadingOrganizations && !error}>
            {!error && (
              <>
                <DarkLabel htmlFor="monorepo-choose-organization" className="sw-mb-2">
                  <FormattedMessage
                    id={`onboarding.create_project.monorepo.choose_organization.${almKey}`}
                  />
                </DarkLabel>
                {(organizationOptions?.length ?? 0) > 0 ? (
                  <InputSelect
                    size="full"
                    isSearchable
                    inputId="monorepo-choose-organization"
                    options={organizationOptions}
                    onChange={({ value }: LabelValueSelectOption) => {
                      onSelectOrganization(value);
                    }}
                    placeholder={formatMessage({
                      id: `onboarding.create_project.monorepo.choose_organization.${almKey}.placeholder`,
                    })}
                    value={selectedOrganization}
                  />
                ) : (
                  !loadingOrganizations && (
                    <FlagMessage variant="error" className="sw-mb-2">
                      <span>
                        {canAdmin ? (
                          <FormattedMessage
                            id="onboarding.create_project.github.no_orgs_admin"
                            defaultMessage={translate(
                              'onboarding.create_project.github.no_orgs_admin',
                            )}
                            values={{
                              link: (
                                <Link to="/admin/settings?category=almintegration">
                                  {translate(
                                    'onboarding.create_project.github.warning.message_admin.link',
                                  )}
                                </Link>
                              ),
                            }}
                          />
                        ) : (
                          translate('onboarding.create_project.github.no_orgs')
                        )}
                      </span>
                    </FlagMessage>
                  )
                )}
              </>
            )}
          </Spinner>
        </div>

        <div className="sw-flex sw-flex-col">
          {selectedOrganization && (
            <DarkLabel className="sw-mb-2" htmlFor="monorepo-choose-repository">
              <FormattedMessage
                id={`onboarding.create_project.monorepo.choose_repository.${almKey}`}
              />
            </DarkLabel>
          )}
          {selectedOrganization && (
            <InputSelect
              inputId="monorepo-choose-repository"
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
                id: `onboarding.create_project.monorepo.choose_repository.${almKey}.placeholder`,
              })}
              size="full"
              value={selectedRepository}
            />
          )}
        </div>
      </div>

      {selectedRepository !== undefined && (
        <>
          <BlueGreySeparator className="sw-my-5" />

          <div>
            <SubTitle>
              <FormattedMessage id="onboarding.create_project.monorepo.project_title" />
            </SubTitle>
            <div>
              {projects.map(({ id, key, name }) => (
                <ProjectValidationCard
                  className="sw-mt-4"
                  initialKey={key}
                  initialName={name}
                  key={id}
                  monorepoSetupProjectKeys={projectKeys}
                  onChange={onProjectChange}
                  onRemove={() => {
                    onProjectRemove(id);
                  }}
                  projectId={id}
                />
              ))}
            </div>

            <div className="sw-flex sw-justify-end sw-mt-4">
              <ButtonSecondary onClick={addProject}>
                <AddNewIcon className="sw-mr-2" />
                <FormattedMessage id="onboarding.create_project.monorepo.add_project" />
              </ButtonSecondary>
            </div>
          </div>
        </>
      )}

      <div className="sw-my-5">
        <ButtonSecondary onClick={cancelMonorepoSetup}>
          <FormattedMessage id="cancel" />
        </ButtonSecondary>
        <ButtonPrimary className="sw-ml-3" disabled={isSetupInvalid} onClick={submitProjects}>
          <FormattedMessage id="next" />
        </ButtonPrimary>
      </div>
    </div>
  );
}
