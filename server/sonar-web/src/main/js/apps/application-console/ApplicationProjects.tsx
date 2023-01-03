/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { find, without } from 'lodash';
import * as React from 'react';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams
} from 'sonar-ui-common/components/controls/SelectList';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import {
  addProjectToApplication,
  getApplicationProjects,
  removeProjectFromApplication
} from '../../api/application';
import { Application, ApplicationProject } from '../../types/application';

interface Props {
  onAddProject?: (project: ApplicationProject) => void;
  onRemoveProject?: (projectKey: string) => void;
  application: Application;
}

interface State {
  disabledProjects: string[];
  lastSearchParams: SelectListSearchParams & { applicationKey: string };
  needToReload: boolean;
  projects: Array<ApplicationProject>;
  projectsTotalCount?: number;
  selectedProjects: string[];
}

export default class ApplicationProjects extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      disabledProjects: [],
      lastSearchParams: {
        applicationKey: props.application.key,
        query: '',
        filter: SelectListFilter.Selected
      },
      needToReload: false,
      projects: [],
      selectedProjects: []
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.application.key !== this.props.application.key) {
      this.setState(
        prevState => {
          return {
            lastSearchParams: {
              ...prevState.lastSearchParams,
              applicationKey: this.props.application.key
            }
          };
        },
        () => this.fetchProjects(this.state.lastSearchParams)
      );
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadApplicationProjects = (searchParams: SelectListSearchParams) =>
    getApplicationProjects({
      application: this.state.lastSearchParams.applicationKey,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined,
      selected: searchParams.filter
    });

  fetchProjects = (searchParams: SelectListSearchParams) =>
    this.loadApplicationProjects(searchParams).then(data => {
      if (this.mounted) {
        this.setState(prevState => {
          const more = searchParams.page != null && searchParams.page > 1;

          const { projects, selectedProjects, disabledProjects } = this.dealWithProjects(
            data,
            more,
            prevState
          );

          return {
            disabledProjects,
            lastSearchParams: { ...prevState.lastSearchParams, ...searchParams },
            needToReload: false,
            projects,
            projectsTotalCount: data.paging.total,
            selectedProjects
          };
        });
      }
    });

  dealWithProjects = (
    data: { projects: Array<ApplicationProject>; paging: T.Paging },
    more: boolean,
    prevState: Readonly<State>
  ) => {
    const projects = more ? [...prevState.projects, ...data.projects] : data.projects;

    const newSelectedProjects = data.projects
      .filter(project => project.selected)
      .map(project => project.key);
    const selectedProjects = more
      ? [...prevState.selectedProjects, ...newSelectedProjects]
      : newSelectedProjects;

    const disabledProjects = more ? [...prevState.disabledProjects] : [];

    return {
      disabledProjects,
      projects,
      selectedProjects
    };
  };

  handleSelect = (projectKey: string) => {
    return addProjectToApplication(this.props.application.key, projectKey).then(() => {
      if (this.mounted) {
        this.setState(state => {
          const project = state.projects.find(p => p.key === projectKey);
          if (project && this.props.onAddProject) {
            this.props.onAddProject(project);
          }
          return {
            needToReload: true,
            selectedProjects: [...state.selectedProjects, projectKey]
          };
        });
      }
    });
  };

  handleUnselect = (projectKey: string) => {
    return removeProjectFromApplication(this.props.application.key, projectKey).then(() => {
      if (this.mounted) {
        this.setState(state => {
          if (this.props.onRemoveProject) {
            this.props.onRemoveProject(projectKey);
          }
          return {
            needToReload: true,
            selectedProjects: without(state.selectedProjects, projectKey)
          };
        });
      }
    });
  };

  renderElement = (projectKey: string) => {
    const project = find(this.state.projects, { key: projectKey });
    if (project === undefined) {
      return '';
    }

    return (
      <div className="views-project-item display-flex-center">
        <QualifierIcon className="spacer-right" qualifier="TRK" />
        <div>
          <div title={project.name}>{project.name}</div>
          <div className="note">{project.key}</div>
        </div>
      </div>
    );
  };

  render() {
    const { projects, selectedProjects } = this.state;

    return (
      <SelectList
        disabledElements={this.state.disabledProjects}
        elements={projects.map(project => project.key)}
        elementsTotalCount={this.state.projectsTotalCount}
        needToReload={
          this.state.needToReload &&
          this.state.lastSearchParams &&
          this.state.lastSearchParams.filter !== SelectListFilter.All
        }
        onSearch={this.fetchProjects}
        onSelect={this.handleSelect}
        onUnselect={this.handleUnselect}
        renderElement={this.renderElement}
        selectedElements={selectedProjects}
        withPaging={true}
      />
    );
  }
}
