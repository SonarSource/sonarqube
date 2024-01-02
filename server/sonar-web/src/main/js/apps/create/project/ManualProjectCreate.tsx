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
import { debounce, isEmpty } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { createProject, doesComponentExists } from '../../../api/components';
import { getValue } from '../../../api/settings';
import DocLink from '../../../components/common/DocLink';
import ProjectKeyInput from '../../../components/common/ProjectKeyInput';
import { SubmitButton } from '../../../components/controls/buttons';
import ValidationInput from '../../../components/controls/ValidationInput';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { PROJECT_KEY_INVALID_CHARACTERS, validateProjectKey } from '../../../helpers/projects';
import { ProjectKeyValidationResult } from '../../../types/component';
import { GlobalSettingKeys } from '../../../types/settings';
import { PROJECT_NAME_MAX_LEN } from './constants';
import CreateProjectPageHeader from './CreateProjectPageHeader';
import './ManualProjectCreate.css';

interface Props {
  branchesEnabled: boolean;
  onProjectCreate: (projectKey: string) => void;
}

interface State {
  projectName: string;
  projectNameError?: string;
  projectNameTouched: boolean;
  projectKey: string;
  projectKeyError?: string;
  projectKeyTouched: boolean;
  validatingProjectKey: boolean;
  mainBranchName: string;
  mainBranchNameError?: string;
  mainBranchNameTouched: boolean;
  submitting: boolean;
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
      submitting: false,
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
            projectKeyError: alreadyExist
              ? translate('onboarding.create_project.project_key.taken')
              : undefined,
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
        !isEmpty(mainBranchName)
    );
  }

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { projectKey, projectName, mainBranchName } = this.state;
    if (this.canSubmit(this.state)) {
      this.setState({ submitting: true });
      createProject({
        project: projectKey,
        name: (projectName || projectKey).trim(),
        mainBranch: mainBranchName,
      }).then(
        ({ project }) => this.props.onProjectCreate(project.key),
        () => {
          if (this.mounted) {
            this.setState({ submitting: false });
          }
        }
      );
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
      }
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
    return result === ProjectKeyValidationResult.Valid
      ? undefined
      : translate('onboarding.create_project.project_key.error', result);
  };

  validateName = (projectName: string) => {
    if (isEmpty(projectName)) {
      return translate('onboarding.create_project.display_name.error.empty');
    }
    return undefined;
  };

  validateMainBranchName = (mainBranchName: string) => {
    if (isEmpty(mainBranchName)) {
      return translate('onboarding.create_project.main_branch_name.error.empty');
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
      submitting,
    } = this.state;
    const { branchesEnabled } = this.props;

    const touched = Boolean(projectKeyTouched || projectNameTouched);
    const projectNameIsInvalid = projectNameTouched && projectNameError !== undefined;
    const projectNameIsValid = projectNameTouched && projectNameError === undefined;
    const mainBranchNameIsValid = mainBranchNameTouched && mainBranchNameError === undefined;
    const mainBranchNameIsInvalid = mainBranchNameTouched && mainBranchNameError !== undefined;

    return (
      <>
        <CreateProjectPageHeader title={translate('onboarding.create_project.setup_manually')} />

        <div className="create-project-manual">
          <div className="flex-1 huge-spacer-right">
            <form className="manual-project-create" onSubmit={this.handleFormSubmit}>
              <MandatoryFieldsExplanation className="big-spacer-bottom" />

              <ValidationInput
                className="form-field"
                description={translate('onboarding.create_project.display_name.description')}
                error={projectNameError}
                labelHtmlFor="project-name"
                isInvalid={projectNameIsInvalid}
                isValid={projectNameIsValid}
                label={translate('onboarding.create_project.display_name')}
                required={true}
              >
                <input
                  className={classNames('input-super-large', {
                    'is-invalid': projectNameIsInvalid,
                    'is-valid': projectNameIsValid,
                  })}
                  id="project-name"
                  maxLength={PROJECT_NAME_MAX_LEN}
                  minLength={1}
                  onChange={(e) => this.handleProjectNameChange(e.currentTarget.value, true)}
                  type="text"
                  value={projectName}
                  autoFocus={true}
                />
              </ValidationInput>
              <ProjectKeyInput
                error={projectKeyError}
                label={translate('onboarding.create_project.project_key')}
                onProjectKeyChange={(e) => this.handleProjectKeyChange(e.currentTarget.value, true)}
                projectKey={projectKey}
                touched={touched}
                validating={validatingProjectKey}
              />

              <ValidationInput
                className="form-field"
                description={
                  <FormattedMessage
                    id="onboarding.create_project.main_branch_name.description"
                    defaultMessage={translate(
                      'onboarding.create_project.main_branch_name.description'
                    )}
                    values={{
                      learn_more: (
                        <DocLink to="/branches/overview">{translate('learn_more')}</DocLink>
                      ),
                    }}
                  />
                }
                error={mainBranchNameError}
                labelHtmlFor="main-branch-name"
                isInvalid={mainBranchNameIsInvalid}
                isValid={mainBranchNameIsValid}
                label={translate('onboarding.create_project.main_branch_name')}
                required={true}
              >
                <input
                  id="main-branch-name"
                  className={classNames('input-super-large', {
                    'is-invalid': mainBranchNameIsInvalid,
                    'is-valid': mainBranchNameIsValid,
                  })}
                  minLength={1}
                  onChange={(e) => this.handleBranchNameChange(e.currentTarget.value, true)}
                  type="text"
                  value={mainBranchName}
                />
              </ValidationInput>

              <SubmitButton disabled={!this.canSubmit(this.state) || submitting}>
                {translate('set_up')}
              </SubmitButton>
              <DeferredSpinner className="spacer-left" loading={submitting} />
            </form>

            {branchesEnabled && (
              <Alert variant="info" display="inline" className="big-spacer-top">
                {translate('onboarding.create_project.pr_decoration.information')}
              </Alert>
            )}
          </div>
        </div>
      </>
    );
  }
}
