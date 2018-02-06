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
import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import NotificationsList from './NotificationsList';
import { addNotification, removeNotification } from './actions';
import Organization from '../../../components/shared/Organization';
import { translate } from '../../../helpers/l10n';
import {
  getProjectNotifications,
  getNotificationChannels,
  getNotificationPerProjectTypes
} from '../../../store/rootReducer';
/*:: import type {
  Notification,
  NotificationsState,
  ChannelsState,
  TypesState
} from '../../../store/notifications/duck'; */
import { getProjectUrl } from '../../../helpers/urls';

class ProjectNotifications extends React.PureComponent {
  /*:: props: {
    project: {
      key: string,
      name: string,
      organization: string
    },
    notifications: NotificationsState,
    channels: ChannelsState,
    types: TypesState,
    addNotification: (n: Notification) => void,
    removeNotification: (n: Notification) => void
  };
*/

  handleAddNotification({ channel, type }) {
    this.props.addNotification({
      channel,
      type,
      project: this.props.project.key,
      projectName: this.props.project.name,
      organization: this.props.project.organization
    });
  }

  handleRemoveNotification({ channel, type }) {
    this.props.removeNotification({
      channel,
      type,
      project: this.props.project.key
    });
  }

  render() {
    const { project, channels } = this.props;

    return (
      <table key={project.key} className="form big-spacer-bottom">
        <thead>
          <tr>
            <th>
              <span className="text-normal">
                <Organization organizationKey={project.organization} />
              </span>
              <h4 className="display-inline-block">
                <Link to={getProjectUrl(project.key)}>{project.name}</Link>
              </h4>
            </th>
            {channels.map(channel => (
              <th key={channel} className="text-center">
                <h4>{translate('notification.channel', channel)}</h4>
              </th>
            ))}
          </tr>
        </thead>
        <NotificationsList
          notifications={this.props.notifications}
          channels={this.props.channels}
          types={this.props.types}
          checkboxId={(d, c) => `project-notification-${project.key}-${d}-${c}`}
          onAdd={n => this.handleAddNotification(n)}
          onRemove={n => this.handleRemoveNotification(n)}
          project={true}
        />
      </table>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  notifications: getProjectNotifications(state, ownProps.project.key),
  channels: getNotificationChannels(state),
  types: getNotificationPerProjectTypes(state)
});

const mapDispatchToProps = { addNotification, removeNotification };

export default connect(mapStateToProps, mapDispatchToProps)(ProjectNotifications);

export const UnconnectedProjectNotifications = ProjectNotifications;
