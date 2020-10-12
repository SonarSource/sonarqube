/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { InjectedRouter } from 'react-router';
import { getApplicationDetails } from '../../api/application';
import { Application, ApplicationProject } from '../../types/application';
import ApplicationDetails from './ApplicationDetails';
import { ApplicationBranch } from './utils';

interface Props {
  applicationKey: string;
  canRecompute?: boolean;
  onDelete: (key: string) => void;
  onEdit: (key: string, name: string) => void;
  pathname: string;
  router: Pick<InjectedRouter, 'replace'>;
  single?: boolean;
}

interface State {
  application?: Application;
  loading: boolean;
}

export default class ApplicationView extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchDetails();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.applicationKey !== this.props.applicationKey) {
      this.fetchDetails();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchDetails = async () => {
    try {
      const application = await getApplicationDetails(this.props.applicationKey);
      if (this.mounted) {
        this.setState({ application, loading: false });
      }
    } catch {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    }
  };

  handleDelete = (key: string) => {
    if (this.mounted) {
      this.props.onDelete(key);
      this.props.router.replace(this.props.pathname);
    }
  };

  handleEdit = (key: string, name: string, description: string) => {
    if (this.mounted) {
      this.props.onEdit(key, name);
      this.setState(state => {
        if (state.application) {
          return {
            application: {
              ...state.application,
              name,
              description
            }
          };
        } else {
          return null;
        }
      });
    }
  };

  handleAddProject = (project: ApplicationProject) => {
    this.setState(state => {
      if (state.application) {
        return {
          application: {
            ...state.application,
            projects: [...state.application.projects, project]
          }
        };
      } else {
        return null;
      }
    });
  };

  handleRemoveProject = (projectKey: string) => {
    this.setState(state => {
      if (state.application) {
        return {
          application: {
            ...state.application,
            projects: state.application.projects.filter(p => p.key !== projectKey)
          }
        };
      } else {
        return null;
      }
    });
  };

  handleUpdateBranches = (branches: ApplicationBranch[]) => {
    this.setState(state => {
      if (state.application) {
        return { application: { ...state.application, branches } };
      } else {
        return null;
      }
    });
  };

  render() {
    if (this.state.loading) {
      return <i className="spinner spacer" />;
    }

    const { application } = this.state;
    if (!application) {
      // when application is not found
      return null;
    }

    return (
      <ApplicationDetails
        application={application}
        canRecompute={this.props.canRecompute}
        onAddProject={this.handleAddProject}
        onDelete={this.handleDelete}
        onEdit={this.handleEdit}
        onRemoveProject={this.handleRemoveProject}
        onUpdateBranches={this.handleUpdateBranches}
        pathname={this.props.pathname}
        single={this.props.single}
      />
    );
  }
}
