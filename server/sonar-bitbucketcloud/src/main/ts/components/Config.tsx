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
import SingleSelect from '@atlaskit/single-select';
import Spinner from '@atlaskit/spinner';
import { getBaseUrl } from '@sqcore/helpers/urls';
import DocModal from './DocModal';
import LoginForm from './LoginForm';
import SonarCloudIcon from './SonarCloudIcon';
import { AppContext } from '../types';
import { bindProject, displayWSError, getMyProjects, putStoredProperty } from '../api';
import { displayMessage } from '../utils';

interface ProjectOption {
  content: string;
  filterValues: string[];
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

  componentWillReceiveProps({ projectKey }: Props) {
    const currentProjectKey = this.state.selectedProject && this.state.selectedProject.value;

    if (currentProjectKey !== projectKey) {
      this.setState((state: State) => ({
        selectedProject: state.projects.find(p => p.value === projectKey)
      }));
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchMyProjects = () => {
    getMyProjects({ ps: 500 }).then(
      ({ projects }) => {
        if (this.mounted) {
          const projectOptions = projects.map(p => ({
            content: p.name,
            filterValues: [p.name, p.key],
            value: p.key
          }));
          this.setState({
            authenticated: true,
            loading: false,
            projects: projectOptions,
            selectedProject: projectOptions.find(p => p.value === this.props.projectKey)
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

  handleFilterChange = (filter: string) => {
    this.setState(({ projects }: State) => ({
      selectedProject: projects.find(p =>
        p.filterValues.some(value => value.toLowerCase() === filter.toLowerCase())
      )
    }));
  };

  handleReload = () => {
    window.location.reload();
  };

  handleSelect = ({ item }: { item: ProjectOption }) => {
    this.setState({ selectedProject: item });
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { disabled, selectedProject } = this.state;

    let updateBinding = false;
    const promises: Promise<any>[] = [];
    if (disabled !== this.props.disabled) {
      promises.push(
        putStoredProperty('disabled', disabled).then(() => {
          this.props.updateDisabled(disabled);
        })
      );
    }
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
    const { projects, selectedProject } = this.state;

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
        <SingleSelect
          defaultSelected={selectedProject}
          hasAutocomplete={true}
          items={[{ items: projects }]}
          label="Project to link to"
          maxHeight={300}
          onFilterChange={this.handleFilterChange}
          onSelected={this.handleSelect}
          placeholder="Select a project"
          shouldFlip={false}
          shouldFocus={true}
        />
        <small>You see only the projects you administer.</small>
      </div>
    );
  };

  render() {
    const { authenticated, disabled, loading, selectedProject } = this.state;

    if (loading) {
      return this.renderContainer(
        <div className="huge-spacer-top">
          <Spinner size="large" />
        </div>
      );
    }

    const hasChanged =
      (selectedProject && selectedProject.value !== this.props.projectKey) ||
      this.props.disabled !== disabled;

    return this.renderContainer(
      <>
        {!authenticated && <LoginForm onReload={this.handleReload} />}
        <form className="settings-form" onSubmit={this.handleSubmit}>
          {authenticated && this.renderProjectsSelect()}
          <div className="ak-field-group display-flex-justify-center">
            <CheckboxStateless
              isChecked={disabled}
              label="Hide repository overview widget"
              name="hide-widget"
              onChange={this.handleDisabledChange}
              value="hide-widget"
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
        <DocModal />
      </>
    );
  }
}
