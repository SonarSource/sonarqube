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
import { groupBy, sortBy, uniqBy } from 'lodash';
import * as React from 'react';
import { Button } from '../../../components/controls/buttons';
import SearchBox from '../../../components/controls/SearchBox';
import { translate } from '../../../helpers/l10n';
import {
  Notification,
  NotificationProject,
  NotificationProjectType,
} from '../../../types/notifications';
import ProjectModal from './ProjectModal';
import ProjectNotifications from './ProjectNotifications';

export interface Props {
  addNotification: (n: Notification) => void;
  channels: string[];
  notifications: Notification[];
  removeNotification: (n: Notification) => void;
  types: NotificationProjectType[];
}

const THRESHOLD_COLLAPSED = 3;

interface State {
  addedProjects: NotificationProject[];
  search: string;
  showModal: boolean;
}

function isNotificationProject(project: {
  project?: string;
  projectName?: string;
}): project is NotificationProject {
  return project.project !== undefined && project.projectName !== undefined;
}

export default class Projects extends React.PureComponent<Props, State> {
  state: State = {
    addedProjects: [],
    search: '',
    showModal: false,
  };

  filterSearch = (project: NotificationProject, search: string) => {
    return project.projectName && project.projectName.toLowerCase().includes(search);
  };

  handleAddProject = (project: NotificationProject) => {
    this.setState((state) => {
      return {
        addedProjects: [...state.addedProjects, project],
      };
    });
  };

  handleSearch = (search = '') => {
    this.setState({ search: search.toLowerCase() });
  };

  handleSubmit = (selectedProject: NotificationProject) => {
    if (selectedProject) {
      this.handleAddProject(selectedProject);
    }

    this.closeModal();
  };

  closeModal = () => {
    this.setState({ showModal: false });
  };

  openModal = () => {
    this.setState({ showModal: true });
  };

  removeNotification = (removed: Notification, allProjects: NotificationProject[]) => {
    const projectToRemove = allProjects.find((p) => p.project === removed.project);
    if (projectToRemove) {
      this.handleAddProject(projectToRemove);
    }

    this.props.removeNotification(removed);
  };

  render() {
    const { notifications } = this.props;
    const { addedProjects, search } = this.state;

    const projects = uniqBy(notifications, ({ project }) => project).filter(
      isNotificationProject
    ) as NotificationProject[];
    const notificationsByProject = groupBy(notifications, (n) => n.project);
    const allProjects = uniqBy([...addedProjects, ...projects], (project) => project.project);
    const filteredProjects = sortBy(allProjects, 'projectName').filter((p) =>
      this.filterSearch(p, search)
    );
    const shouldBeCollapsed = Object.keys(notificationsByProject).length > THRESHOLD_COLLAPSED;

    return (
      <section className="boxed-group" data-test="account__project-notifications">
        <div className="boxed-group-inner">
          <div className="page-actions">
            <Button onClick={this.openModal}>
              <span data-test="account__add-project-notification">
                {translate('my_profile.per_project_notifications.add')}
              </span>
            </Button>
          </div>

          <h2>{translate('my_profile.per_project_notifications.title')}</h2>
        </div>

        {this.state.showModal && (
          <ProjectModal
            addedProjects={allProjects}
            closeModal={this.closeModal}
            onSubmit={this.handleSubmit}
          />
        )}

        <div className="boxed-group-inner">
          {allProjects.length === 0 && (
            <div className="note">{translate('my_account.no_project_notifications')}</div>
          )}

          {allProjects.length > 0 && (
            <div className="big-spacer-bottom">
              <SearchBox
                onChange={this.handleSearch}
                placeholder={translate('search.search_for_projects')}
              />
            </div>
          )}

          {filteredProjects.map((project) => {
            const collapsed = addedProjects.find((p) => p.project === project.project)
              ? false
              : shouldBeCollapsed;
            return (
              <ProjectNotifications
                addNotification={this.props.addNotification}
                channels={this.props.channels}
                collapsed={collapsed}
                key={project.project}
                notifications={notificationsByProject[project.project] || []}
                project={project}
                removeNotification={(n) => this.removeNotification(n, allProjects)}
                types={this.props.types}
              />
            );
          })}
        </div>
      </section>
    );
  }
}
