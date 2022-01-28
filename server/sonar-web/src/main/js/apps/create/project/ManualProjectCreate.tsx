/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { debounce } from 'lodash';
import * as React from 'react';
import { createProject, doesComponentExists } from '../../../api/components';
import ProjectKeyInput from '../../../components/common/ProjectKeyInput';
import { SubmitButton } from '../../../components/controls/buttons';
import ValidationInput from '../../../components/controls/ValidationInput';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { PROJECT_KEY_INVALID_CHARACTERS, validateProjectKey } from '../../../helpers/projects';
import { ProjectKeyValidationResult } from '../../../types/component';
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
  projectNameTouched?: boolean;
  projectKey: string;
  projectKeyError?: string;
  projectKeyTouched?: boolean;
  validatingProjectKey: boolean;
  submitting: boolean;
}

type ValidState = State & Required<Pick<State, 'projectKey' | 'projectName'>>;

export default class ManualProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      projectKey: '',
      projectName: '',
      submitting: false,
      validatingProjectKey: false
    };
    this.checkFreeKey = debounce(this.checkFreeKey, 250);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkFreeKey = (key: string) => {
    this.setState({ validatingProjectKey: true });

    return doesComponentExists({ component: key })
      .then(alreadyExist => {
        if (this.mounted && key === this.state.projectKey) {
          this.setState({
            projectKeyError: alreadyExist
              ? translate('onboarding.create_project.project_key.taken')
              : undefined,
            validatingProjectKey: false
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
    const { projectKey, projectKeyError, projectName, projectNameError } = state;
    return Boolean(
      projectKeyError === undefined &&
        projectNameError === undefined &&
        projectKey.length > 0 &&
        projectName.length > 0
    );
  }

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { state } = this;
    if (this.canSubmit(state)) {
      this.setState({ submitting: true });
      createProject({
        project: state.projectKey,
        name: (state.projectName || state.projectKey).trim()
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
      projectKeyTouched: fromUI
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
        projectNameTouched: fromUI
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

  validateKey = (projectKey: string) => {
    const result = validateProjectKey(projectKey);
    return result === ProjectKeyValidationResult.Valid
      ? undefined
      : translate('onboarding.create_project.project_key.error', result);
  };

  validateName = (projectName: string) => {
    if (projectName.length === 0) {
      return translate('onboarding.create_project.display_name.error.empty');
    } else if (projectName.length > PROJECT_NAME_MAX_LEN) {
      return translate('onboarding.create_project.display_name.error.too_long');
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
      submitting
    } = this.state;
    const { branchesEnabled } = this.props;

    const touched = !!(projectKeyTouched || projectNameTouched);
    const projectNameIsInvalid = touched && projectNameError !== undefined;
    const projectNameIsValid = touched && projectNameError === undefined;

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
                id="project-name"
                isInvalid={projectNameIsInvalid}
                isValid={projectNameIsValid}
                label={translate('onboarding.create_project.display_name')}
                required={true}>
                <input
                  className={classNames('input-super-large', {
                    'is-invalid': projectNameIsInvalid,
                    'is-valid': projectNameIsValid
                  })}
                  id="project-name"
                  maxLength={PROJECT_NAME_MAX_LEN}
                  minLength={1}
                  onChange={e => this.handleProjectNameChange(e.currentTarget.value, true)}
                  type="text"
                  value={projectName}
                  autoFocus={true}
                />
              </ValidationInput>
              <ProjectKeyInput
                error={projectKeyError}
                label={translate('onboarding.create_project.project_key')}
                onProjectKeyChange={e => this.handleProjectKeyChange(e.currentTarget.value, true)}
                projectKey={projectKey}
                touched={touched}
                validating={validatingProjectKey}
              />

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
