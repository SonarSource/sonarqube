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
  FlagErrorIcon,
  FlagMessage,
  FlagSuccessIcon,
  FormField,
  InputField,
  Link,
  Note,
  Title,
} from 'design-system';
import { debounce, isEmpty } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
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
}

interface State {
  projectName: string;
  projectNameError?: boolean;
  projectNameTouched: boolean;
  projectKey: string;
  projectKeyError?: boolean;
  projectKeyTouched: boolean;
  validatingProjectKey: boolean;
  mainBranchName: string;
  mainBranchNameError?: boolean;
  mainBranchNameTouched: boolean;
}

const DEBOUNCE_DELAY = 250;

type ValidState = State & Required<Pick<State, 'projectKey' | 'projectName'>>;

export default class ManualProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      projectKey: '',
      projectName: '',
      projectKeyTouched: false,
      projectNameTouched: false,
      mainBranchName: 'main',
      mainBranchNameTouched: false,
      validatingProjectKey: false,
    };
    this.checkFreeKey = debounce(this.checkFreeKey, DEBOUNCE_DELAY);
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchMainBranchName();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchMainBranchName = async () => {
    const mainBranchName = await getValue({ key: GlobalSettingKeys.MainBranchName });

    if (this.mounted && mainBranchName.value !== undefined) {
      this.setState({ mainBranchName: mainBranchName.value });
    }
  };

  checkFreeKey = (key: string) => {
    this.setState({ validatingProjectKey: true });

    doesComponentExists({ component: key })
      .then((alreadyExist) => {
        if (this.mounted && key === this.state.projectKey) {
          this.setState({
            projectKeyError: alreadyExist ? true : undefined,
            validatingProjectKey: false,
          });
        }
      })
      .catch(() => {
        if (this.mounted && key === this.state.projectKey) {
          this.setState({ projectKeyError: undefined, validatingProjectKey: false });
        }
      });
  };

  canSubmit(state: State): state is ValidState {
    const { projectKey, projectKeyError, projectName, projectNameError, mainBranchName } = state;
    return Boolean(
      projectKeyError === undefined &&
        projectNameError === undefined &&
        !isEmpty(projectKey) &&
        !isEmpty(projectName) &&
        !isEmpty(mainBranchName),
    );
  }

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { projectKey, projectName, mainBranchName } = this.state;
    if (this.canSubmit(this.state)) {
      this.props.onProjectSetupDone({
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

  handleProjectKeyChange = (projectKey: string, fromUI = false) => {
    const projectKeyError = this.validateKey(projectKey);

    this.setState({
      projectKey,
      projectKeyError,
      projectKeyTouched: fromUI,
    });

    if (projectKeyError === undefined) {
      this.checkFreeKey(projectKey);
    }
  };

  handleProjectNameChange = (projectName: string, fromUI = false) => {
    this.setState(
      {
        projectName,
        projectNameError: this.validateName(projectName),
        projectNameTouched: fromUI,
      },
      () => {
        if (!this.state.projectKeyTouched) {
          const sanitizedProjectKey = this.state.projectName
            .trim()
            .replace(PROJECT_KEY_INVALID_CHARACTERS, '-');
          this.handleProjectKeyChange(sanitizedProjectKey);
        }
      },
    );
  };

  handleBranchNameChange = (mainBranchName: string, fromUI = false) => {
    this.setState({
      mainBranchName,
      mainBranchNameError: this.validateMainBranchName(mainBranchName),
      mainBranchNameTouched: fromUI,
    });
  };

  validateKey = (projectKey: string) => {
    const result = validateProjectKey(projectKey);
    return result === ProjectKeyValidationResult.Valid ? undefined : true;
  };

  validateName = (projectName: string) => {
    if (isEmpty(projectName)) {
      return true;
    }
    return undefined;
  };

  validateMainBranchName = (mainBranchName: string) => {
    if (isEmpty(mainBranchName)) {
      return true;
    }
    return undefined;
  };

  render() {
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
    } = this.state;
    const { branchesEnabled } = this.props;

    const touched = Boolean(projectKeyTouched || projectNameTouched);
    const projectNameIsInvalid = projectNameTouched && projectNameError !== undefined;
    const projectNameIsValid = projectNameTouched && projectNameError === undefined;
    const projectKeyIsInvalid = touched && projectKeyError !== undefined;
    const projectKeyIsValid = touched && !validatingProjectKey && projectKeyError === undefined;
    const mainBranchNameIsValid = mainBranchNameTouched && mainBranchNameError === undefined;
    const mainBranchNameIsInvalid = mainBranchNameTouched && mainBranchNameError !== undefined;

    return (
      <div className="sw-max-w-[50%]">
        <Title>{translate('onboarding.create_project.manual.title')}</Title>
        {branchesEnabled && (
          <FlagMessage className="sw-my-4" variant="info">
            {translate('onboarding.create_project.pr_decoration.information')}
          </FlagMessage>
        )}
        <form
          id="create-project-manual"
          className="sw-flex-col sw-body-sm"
          onSubmit={this.handleFormSubmit}
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
                onChange={(e) => this.handleProjectNameChange(e.currentTarget.value, true)}
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
                onChange={(e) => this.handleProjectKeyChange(e.currentTarget.value, true)}
                type="text"
                value={projectKey}
                isInvalid={projectKeyIsInvalid}
                isValid={projectKeyIsValid}
                required
              />
              {projectKeyIsInvalid && <FlagErrorIcon className="sw-ml-2" />}
              {projectKeyIsValid && <FlagSuccessIcon className="sw-ml-2" />}
            </div>
            <Note className="sw-mt-2">
              {translate('onboarding.create_project.project_key.description')}
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
                onChange={(e) => this.handleBranchNameChange(e.currentTarget.value, true)}
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
              <FormattedMessageWithDocLink />
            </Note>
          </FormField>

          <ButtonPrimary
            type="submit"
            className="sw-mt-4 sw-mb-4"
            disabled={!this.canSubmit(this.state)}
          >
            {translate('next')}
          </ButtonPrimary>
        </form>
      </div>
    );
  }
}

function FormattedMessageWithDocLink() {
  const docUrl = useDocUrl();

  return (
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
  );
}
