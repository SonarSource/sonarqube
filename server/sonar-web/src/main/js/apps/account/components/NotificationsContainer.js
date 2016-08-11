/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import Helmet from 'react-helmet';
import Notifications from './Notifications';
import { translate } from '../../../helpers/l10n';

export default class NotificationsContainer extends React.Component {
  state = {
    globalNotifications: window.sonarqube.notifications.global,
    projectNotifications: window.sonarqube.notifications.project
  };

  componentWillMount () {
    this.handleAddProject = this.handleAddProject.bind(this);
    this.handleRemoveProject = this.handleRemoveProject.bind(this);
  }

  handleAddProject (project) {
    const { projectNotifications } = this.state;
    const found = projectNotifications
        .find(notification => notification.project.internalId === project.internalId);

    if (!found) {
      const newProjectNotification = {
        project,
        notifications: window.sonarqube.notifications.projectDispatchers.map(dispatcher => {
          const channels = window.sonarqube.notifications.channels.map(channel => {
            return { id: channel, checked: false };
          });
          return { dispatcher, channels };
        })
      };

      this.setState({
        projectNotifications: [...projectNotifications, newProjectNotification]
      });
    }
  }

  handleRemoveProject (project) {
    const projectNotifications = this.state.projectNotifications
        .filter(notification => notification.project.internalId !== project.internalId);
    this.setState({ projectNotifications });
  }

  render () {
    const title = translate('my_account.page') + ' - ' +
        translate('my_account.notifications');

    return (
        <div>
          <Helmet
              title={title}
              titleTemplate="SonarQube - %s"/>

          <Notifications
              globalNotifications={this.state.globalNotifications}
              projectNotifications={this.state.projectNotifications}
              onAddProject={this.handleAddProject}
              onRemoveProject={this.handleRemoveProject}/>
        </div>
    );
  }
}
