/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Button from '@atlaskit/button';
import { CheckboxStateless } from '@atlaskit/checkbox';
import Select, { createFilter } from '@atlaskit/select';
import Spinner from '@atlaskit/spinner';
import { getBaseUrl } from '@sqcore/helpers/urls';
import HelpLink from './HelpLink';
import LoginForm from './LoginForm';
import SonarCloudIcon from './SonarCloudIcon';
import { AppContext } from '../types';
import { bindProject, displayWSError, getMyProjects, putStoredProperty } from '../api';
import { displayMessage } from '../utils';

interface ProjectOption {
  label: string;
  value: string;
}

interface Props {
  context: AppContext;
  disabled: boolean;
  projectKey?: string;
  updateDisabled: (disabled: boolean) => void;
  updateProjectKey: (projectKey: string) => void;
}

interface State {
  authenticated: boolean;
  saving: boolean;
  bindSuccess?: boolean;
  disabled: boolean;
  loading: boolean;
  projects: ProjectOption[];
  selectedProject?: ProjectOption;
}

export default class Config extends React.PureComponent<Props, State> {
  filterOption = createFilter({
    ignoreCase: true,
    ignoreAccents: true,
    stringify: (option: ProjectOption) => `${option.label} ${option.value}`,
    trim: true,
    matchFrom: 'any'
  });

  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      authenticated: false,
      saving: false,
      disabled: props.disabled,
      loading: true,
      projects: []
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchMyProjects();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  get selectedProject() {
    return (
      this.state.selectedProject || this.state.projects.find(p => p.value === this.props.projectKey)
    );
  }

  fetchMyProjects = () => {
    getMyProjects({ ps: 500 }).then(
      ({ projects }) => {
        if (this.mounted) {
          const projectOptions = projects.map(p => ({ label: p.name, value: p.key }));
          this.setState({
            authenticated: true,
            loading: false,
            projects: projectOptions
          });
        }
      },
      ({ response }) => {
        if (this.mounted && response && response.status === 401) {
          this.setState({ authenticated: false, loading: false });
        } else {
          displayWSError({ response });
        }
      }
    );
  };

  handleDisabledChange = () => {
    this.setState(state => ({ disabled: !state.disabled }));
  };

  handleReload = () => {
    window.location.reload();
  };

  handleChange = (selectedProject?: ProjectOption) => {
    if (selectedProject && !Array.isArray(selectedProject)) {
      this.setState({ selectedProject });
    }
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { disabled } = this.state;

    let updateBinding = false;
    const promises: Promise<any>[] = [];
    if (disabled !== this.props.disabled) {
      promises.push(
        putStoredProperty('disabled', disabled).then(() => {
          this.props.updateDisabled(disabled);
        })
      );
    }

    const { selectedProject } = this;
    if (selectedProject && selectedProject.value !== this.props.projectKey) {
      updateBinding = true;
      promises.push(
        bindProject({ ...this.props.context, projectKey: selectedProject.value }).then(() => {
          this.props.updateProjectKey(selectedProject.value);
        })
      );
    }
    if (promises.length > 0) {
      (document.activeElement as HTMLElement).blur();
      this.setState({ saving: true });
      Promise.all(promises).then(
        () => {
          if (this.mounted) {
            displayMessage(
              'success',
              'SonarCloud',
              updateBinding
                ? 'SonarCloud project successfully linked, check the new Code Quality widget on your repository overview.'
                : 'Widget visibility successfully changed.'
            );
            this.setState({ saving: false });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ saving: false });
          }
        }
      );
    }
  };

  renderContainer = (children: React.ReactNode) => {
    return (
      <>
        <h2>SonarCloud Settings</h2>
        <div className="settings-content">
          <div className="settings-logo">
            <SonarCloudIcon size={128} />
          </div>
          <p className="settings-description">
            To display the quality of your repository, you have to link it with a project analyzed
            on SonarCloud.
          </p>
          {children}
        </div>
      </>
    );
  };

  renderProjectsSelect = () => {
    const { projects } = this.state;

    if (!projects || projects.length <= 0) {
      return (
        <>
          <p>
            You don&apos;t have any project on SonarCloud yet:{' '}
            <a href={getBaseUrl() + '/onboarding'} rel="noopener noreferrer" target="_blank">
              Analyse a project
            </a>
          </p>
        </>
      );
    }

    return (
      <div className="settings-projects">
        <label htmlFor="projects-select">Project to link to</label>
        <Select
          autoFocus={true}
          className="settings-projects-select"
          filterOption={this.filterOption}
          id="projects-select"
          isClearable={false}
          isSearchable={true}
          maxMenuHeight={300}
          onChange={this.handleChange}
          options={projects}
          placeholder="Select a project"
          value={this.selectedProject}
        />
        <small>You see only the projects you administer.</small>
      </div>
    );
  };

  render() {
    const { authenticated, disabled, loading } = this.state;

    if (loading) {
      return this.renderContainer(
        <div className="huge-spacer-top">
          <Spinner size="large" />
        </div>
      );
    }

    const { selectedProject } = this;
    const hasChanged =
      (selectedProject && selectedProject.value !== this.props.projectKey) ||
      this.props.disabled !== disabled;

    return this.renderContainer(
      <>
        {!authenticated && <LoginForm onReload={this.handleReload} />}
        <form className="settings-form" onSubmit={this.handleSubmit}>
          {authenticated && this.renderProjectsSelect()}
          <div className="display-flex-justify-center">
            <CheckboxStateless
              isChecked={!disabled}
              label="Show repository overview widget"
              name="show-widget"
              onChange={this.handleDisabledChange}
              value="show-widget"
            />
          </div>
          <div className="ak-field-group">
            <Button
              appearance="primary"
              isDisabled={this.state.saving || !hasChanged}
              type="submit">
              Save
            </Button>
          </div>
        </form>
        <HelpLink />
      </>
    );
  }
}
