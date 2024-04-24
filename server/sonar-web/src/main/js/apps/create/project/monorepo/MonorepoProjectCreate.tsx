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
import { Spinner } from '@sonarsource/echoes-react';
import { BlueGreySeparator, ButtonPrimary, ButtonSecondary } from 'design-system';
import React, { useEffect, useRef } from 'react';
import { FormattedMessage } from 'react-intl';
import { GroupBase } from 'react-select';
import { getComponents } from '../../../../api/project-management';
import { useLocation, useRouter } from '../../../../components/hoc/withRouter';
import { throwGlobalError } from '../../../../helpers/error';
import { LabelValueSelectOption } from '../../../../helpers/search';
import { useProjectBindingsQuery } from '../../../../queries/dop-translation';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';
import { ImportProjectParam } from '../CreateProjectPage';
import { ProjectData } from '../components/ProjectValidation';
import { CreateProjectModes } from '../types';
import { getSanitizedProjectKey } from '../utils';
import { MonorepoConnectionSelector } from './MonorepoConnectionSelector';
import { MonorepoProjectHeader } from './MonorepoProjectHeader';
import { MonorepoProjectsList } from './MonorepoProjectsList';

interface MonorepoProjectCreateProps {
  dopSettings: DopSetting[];
  error: boolean;
  loadingBindings: boolean;
  loadingOrganizations: boolean;
  loadingRepositories: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
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
  showOrganizations?: boolean;
  showPersonalAccessToken?: boolean;
}

type ProjectItem = Required<ProjectData<number>>;

export default function MonorepoProjectCreate(props: Readonly<MonorepoProjectCreateProps>) {
  const {
    loadingBindings,
    onProjectSetupDone,
    selectedDopSetting,
    selectedOrganization,
    selectedRepository,
    showOrganizations = false,
  } = props;

  const projectCounter = useRef(0);

  const [projects, setProjects] = React.useState<ProjectItem[]>([]);
  const [alreadyBoundProjects, setAlreadyBoundProjects] = React.useState<
    Array<{ projectId: string; projectName: string }>
  >([]);

  const location = useLocation();
  const { push } = useRouter();

  const projectKeys = React.useMemo(() => projects.map(({ key }) => key), [projects]);
  const {
    data: alreadyBoundProjectBindings,
    isFetching: isFetchingAlreadyBoundProjects,
    isLoading: isLoadingAlreadyBoundProjects,
  } = useProjectBindingsQuery(
    {
      dopSettingId: selectedDopSetting?.id,
      repository: selectedRepository?.value,
    },
    selectedRepository !== undefined,
  );

  const almKey = location.query.mode as AlmKeys;

  const isOptionSelectionInvalid =
    (showOrganizations && selectedOrganization === undefined) || selectedRepository === undefined;
  const isSetupInvalid =
    selectedDopSetting === undefined ||
    isOptionSelectionInvalid ||
    projects.length === 0 ||
    projects.some(({ hasError, key, name }) => hasError || key === '' || name === '');

  const onAddProject = React.useCallback(() => {
    if (isOptionSelectionInvalid) {
      return;
    }

    const id = projectCounter.current;
    projectCounter.current += 1;
    const projectKeySuffix = id === 0 ? '' : `-${id}`;
    const projectKey = getSanitizedProjectKey(
      showOrganizations && selectedOrganization
        ? `${selectedOrganization.label}_${selectedRepository.label}_add-your-reference${projectKeySuffix}`
        : `${selectedRepository.label}_add-your-reference${projectKeySuffix}`,
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
  }, [
    isOptionSelectionInvalid,
    projects,
    selectedOrganization,
    selectedRepository,
    showOrganizations,
  ]);

  const onChangeProject = React.useCallback(
    (project: ProjectItem) => {
      const newProjects = projects.filter(({ id }) => id !== project.id);
      newProjects.push({
        ...project,
      });
      newProjects.sort((a, b) => a.id - b.id);

      setProjects(newProjects);
    },
    [projects],
  );

  const onRemoveProject = React.useCallback(
    (id: number) => {
      const newProjects = projects.filter(({ id: projectId }) => projectId !== id);

      setProjects(newProjects);
    },
    [projects],
  );

  const cancelMonorepoSetup = () => {
    push({
      pathname: location.pathname,
      query: { mode: almKey },
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
      onAddProject();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRepository]);

  useEffect(() => {
    if (alreadyBoundProjectBindings === undefined) {
      return;
    }

    if (alreadyBoundProjectBindings.projectBindings.length === 0) {
      setAlreadyBoundProjects([]);
      return;
    }

    getComponents({
      projects: alreadyBoundProjectBindings.projectBindings.reduce(
        (projectsSearchParam, { projectKey }) => `${projectsSearchParam},${projectKey}`,
        '',
      ),
    })
      .then(({ components }) => {
        setAlreadyBoundProjects(
          components.map(({ key, name }) => ({ projectId: key, projectName: name })),
        );
      })
      .catch(throwGlobalError);
  }, [alreadyBoundProjectBindings]);

  if (loadingBindings) {
    return <Spinner />;
  }

  return (
    <div>
      <MonorepoProjectHeader />

      <BlueGreySeparator className="sw-my-5" />

      <MonorepoConnectionSelector
        almKey={almKey}
        alreadyBoundProjects={alreadyBoundProjects}
        isFetchingAlreadyBoundProjects={isFetchingAlreadyBoundProjects}
        isLoadingAlreadyBoundProjects={isLoadingAlreadyBoundProjects}
        {...props}
      />

      {selectedRepository !== undefined && (
        <>
          <BlueGreySeparator className="sw-my-5" />

          <MonorepoProjectsList
            projectKeys={projectKeys}
            onAddProject={onAddProject}
            onChangeProject={onChangeProject}
            onRemoveProject={onRemoveProject}
            projects={projects}
          />
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
