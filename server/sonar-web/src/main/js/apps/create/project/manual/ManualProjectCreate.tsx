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
import classNames from 'classnames';
import {
  ButtonPrimary,
  ButtonSecondary,
  CloseIcon,
  FlagErrorIcon,
  FlagMessage,
  FlagSuccessIcon,
  FormField,
  InputField,
  InteractiveIcon,
  Link,
  Note,
  TextError,
  Title,
} from 'design-system';
import { debounce, isEmpty } from 'lodash';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { doesComponentExists } from '../../../../api/components';
import { getValue } from '../../../../api/settings';
import { useDocUrl } from '../../../../helpers/docs';
import { translate } from '../../../../helpers/l10n';
import { PROJECT_KEY_INVALID_CHARACTERS, validateProjectKey } from '../../../../helpers/projects';
import { ProjectKeyValidationResult } from '../../../../types/component';
import { GlobalSettingKeys } from '../../../../types/settings';
import { ImportProjectParam } from '../CreateProjectPage';
import { PROJECT_NAME_MAX_LEN } from '../constants';
import { CreateProjectModes } from '../types';

interface Props {
  branchesEnabled: boolean;
  onProjectSetupDone: (importProjects: ImportProjectParam) => void;
  onClose: () => void;
}

interface State {
  projectName: string;
  projectNameError?: boolean;
  projectNameTouched: boolean;
  projectKey: string;
  projectKeyError?: 'DUPLICATE_KEY' | 'WRONG_FORMAT';
  projectKeyTouched: boolean;
  validatingProjectKey: boolean;
  mainBranchName: string;
  mainBranchNameError?: boolean;
  mainBranchNameTouched: boolean;
}

const DEBOUNCE_DELAY = 250;

type ValidState = State & Required<Pick<State, 'projectKey' | 'projectName'>>;

export default function ManualProjectCreate(props: Readonly<Props>) {
  const [project, setProject] = React.useState<State>({
    projectKey: '',
    projectName: '',
    projectKeyTouched: false,
    projectNameTouched: false,
    mainBranchName: 'main',
    mainBranchNameTouched: false,
    validatingProjectKey: false,
  });
  const intl = useIntl();
  const docUrl = useDocUrl();

  const checkFreeKey = React.useCallback(
    debounce((key: string) => {
      setProject((prevProject) => ({ ...prevProject, validatingProjectKey: true }));

      doesComponentExists({ component: key })
        .then((alreadyExist) => {
          setProject((prevProject) => {
            if (key === prevProject.projectKey) {
              return {
                ...prevProject,
                projectKeyError: alreadyExist ? 'DUPLICATE_KEY' : undefined,
                validatingProjectKey: false,
              };
            }
            return prevProject;
          });
        })
        .catch(() => {
          setProject((prevProject) => {
            if (key === prevProject.projectKey) {
              return {
                ...prevProject,
                projectKeyError: undefined,
                validatingProjectKey: false,
              };
            }
            return prevProject;
          });
        });
    }, DEBOUNCE_DELAY),
    [],
  );

  const handleProjectKeyChange = React.useCallback(
    (projectKey: string, fromUI = false) => {
      const projectKeyError = validateKey(projectKey);

      setProject((prevProject) => ({
        ...prevProject,
        projectKey,
        projectKeyError,
        projectKeyTouched: fromUI,
      }));

      if (projectKeyError === undefined) {
        checkFreeKey(projectKey);
      }
    },
    [checkFreeKey],
  );

  React.useEffect(() => {
    async function fetchMainBranchName() {
      const { value: mainBranchName } = await getValue({ key: GlobalSettingKeys.MainBranchName });

      if (mainBranchName !== undefined) {
        setProject((prevProject) => ({
          ...prevProject,
          mainBranchName,
        }));
      }
    }

    fetchMainBranchName();
  }, []);

  React.useEffect(() => {
    if (!project.projectKeyTouched) {
      const sanitizedProjectKey = project.projectName
        .trim()
        .replace(PROJECT_KEY_INVALID_CHARACTERS, '-');

      handleProjectKeyChange(sanitizedProjectKey);
    }
  }, [project.projectName, project.projectKeyTouched, handleProjectKeyChange]);

  const canSubmit = (state: State): state is ValidState => {
    const { projectKey, projectKeyError, projectName, projectNameError, mainBranchName } = state;
    return Boolean(
      projectKeyError === undefined &&
        projectNameError === undefined &&
        !isEmpty(projectKey) &&
        !isEmpty(projectName) &&
        !isEmpty(mainBranchName),
    );
  };

  const handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { projectKey, projectName, mainBranchName } = project;
    if (canSubmit(project)) {
      props.onProjectSetupDone({
        creationMode: CreateProjectModes.Manual,
        projects: [
          {
            project: projectKey,
            name: (projectName || projectKey).trim(),
            mainBranch: mainBranchName,
          },
        ],
      });
    }
  };

  const handleProjectNameChange = (projectName: string, fromUI = false) => {
    setProject({
      ...project,
      projectName,
      projectNameError: validateName(projectName),
      projectNameTouched: fromUI,
    });
  };

  const handleBranchNameChange = (mainBranchName: string, fromUI = false) => {
    setProject({
      ...project,
      mainBranchName,
      mainBranchNameError: validateMainBranchName(mainBranchName),
      mainBranchNameTouched: fromUI,
    });
  };

  const validateKey = (projectKey: string) => {
    const result = validateProjectKey(projectKey);
    if (result !== ProjectKeyValidationResult.Valid) {
      return 'WRONG_FORMAT';
    }
    return undefined;
  };

  const validateName = (projectName: string) => {
    if (isEmpty(projectName)) {
      return true;
    }
    return undefined;
  };

  const validateMainBranchName = (mainBranchName: string) => {
    if (isEmpty(mainBranchName)) {
      return true;
    }
    return undefined;
  };

  const {
    projectKey,
    projectKeyError,
    projectKeyTouched,
    projectName,
    projectNameError,
    projectNameTouched,
    validatingProjectKey,
    mainBranchName,
    mainBranchNameError,
    mainBranchNameTouched,
  } = project;
  const { branchesEnabled } = props;

  const touched = Boolean(projectKeyTouched || projectNameTouched);
  const projectNameIsInvalid = projectNameTouched && projectNameError !== undefined;
  const projectNameIsValid = projectNameTouched && projectNameError === undefined;
  const projectKeyIsInvalid = touched && projectKeyError !== undefined;
  const projectKeyIsValid = touched && !validatingProjectKey && projectKeyError === undefined;
  const mainBranchNameIsValid = mainBranchNameTouched && mainBranchNameError === undefined;
  const mainBranchNameIsInvalid = mainBranchNameTouched && mainBranchNameError !== undefined;

  return (
    <section
      aria-label={translate('onboarding.create_project.manual.title')}
      className="sw-body-sm"
    >
      <div className="sw-flex sw-justify-between">
        <FormattedMessage
          id="onboarding.create_project.manual.step1"
          defaultMessage={translate('onboarding.create_project.manual.step1')}
        />
        <InteractiveIcon
          Icon={CloseIcon}
          aria-label={intl.formatMessage({ id: 'clear' })}
          currentColor
          onClick={props.onClose}
          size="small"
        />
      </div>
      <Title>{translate('onboarding.create_project.manual.title')}</Title>
      {branchesEnabled && (
        <FlagMessage className="sw-my-4" variant="info">
          {translate('onboarding.create_project.pr_decoration.information')}
        </FlagMessage>
      )}
      <div className="sw-max-w-[50%] sw-mt-2">
        <form
          id="create-project-manual"
          className="sw-flex-col sw-body-sm"
          onSubmit={handleFormSubmit}
        >
          <FormField
            htmlFor="project-name"
            label={translate('onboarding.create_project.display_name')}
            required
          >
            <div>
              <InputField
                className={classNames({
                  'js__is-invalid': projectNameIsInvalid,
                })}
                size="large"
                id="project-name"
                maxLength={PROJECT_NAME_MAX_LEN}
                minLength={1}
                onChange={(e) => handleProjectNameChange(e.currentTarget.value, true)}
                type="text"
                value={projectName}
                autoFocus
                isInvalid={projectNameIsInvalid}
                isValid={projectNameIsValid}
                required
              />
              {projectNameIsInvalid && <FlagErrorIcon className="sw-ml-2" />}
              {projectNameIsValid && <FlagSuccessIcon className="sw-ml-2" />}
            </div>
            <Note className="sw-mt-2">
              {translate('onboarding.create_project.display_name.description')}
            </Note>
          </FormField>

          <FormField
            htmlFor="project-key"
            label={translate('onboarding.create_project.project_key')}
            required
          >
            <div>
              <InputField
                className={classNames({
                  'js__is-invalid': projectKeyIsInvalid,
                })}
                size="large"
                id="project-key"
                minLength={1}
                onChange={(e) => handleProjectKeyChange(e.currentTarget.value, true)}
                type="text"
                value={projectKey}
                isInvalid={projectKeyIsInvalid}
                isValid={projectKeyIsValid}
                required
              />
              {projectKeyIsInvalid && <FlagErrorIcon className="sw-ml-2" />}
              {projectKeyIsValid && <FlagSuccessIcon className="sw-ml-2" />}
            </div>
            <Note className="sw-flex-col sw-mt-2">
              {projectKeyError === 'DUPLICATE_KEY' && (
                <TextError
                  text={translate('onboarding.create_project.project_key.duplicate_key')}
                />
              )}
              {!isEmpty(projectKey) && projectKeyError === 'WRONG_FORMAT' && (
                <TextError text={translate('onboarding.create_project.project_key.wrong_format')} />
              )}
              <p>{translate('onboarding.create_project.project_key.description')}</p>
            </Note>
          </FormField>

          <FormField
            htmlFor="main-branch-name"
            label={translate('onboarding.create_project.main_branch_name')}
            required
          >
            <div>
              <InputField
                className={classNames({
                  'js__is-invalid': mainBranchNameIsInvalid,
                })}
                size="large"
                id="main-branch-name"
                minLength={1}
                onChange={(e) => handleBranchNameChange(e.currentTarget.value, true)}
                type="text"
                value={mainBranchName}
                isInvalid={mainBranchNameIsInvalid}
                isValid={mainBranchNameIsValid}
                required
              />
              {mainBranchNameIsInvalid && <FlagErrorIcon className="sw-ml-2" />}
              {mainBranchNameIsValid && <FlagSuccessIcon className="sw-ml-2" />}
            </div>
            <Note className="sw-mt-2">
              <FormattedMessage
                id="onboarding.create_project.main_branch_name.description"
                defaultMessage={translate('onboarding.create_project.main_branch_name.description')}
                values={{
                  learn_more: (
                    <Link to={docUrl('/analyzing-source-code/branches/branch-analysis')}>
                      {translate('learn_more')}
                    </Link>
                  ),
                }}
              />
            </Note>
          </FormField>

          <ButtonSecondary className="sw-mt-4 sw-mr-4" onClick={props.onClose} type="button">
            {intl.formatMessage({ id: 'cancel' })}
          </ButtonSecondary>
          <ButtonPrimary className="sw-mt-4" type="submit" disabled={!canSubmit(project)}>
            {translate('next')}
          </ButtonPrimary>
        </form>
      </div>
    </section>
  );
}
