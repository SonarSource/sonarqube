/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Link } from 'react-router';
import NotificationsList from './NotificationsList';
import { NotificationProject } from './types';
import Organization from '../../../components/shared/Organization';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';

interface Props {
  addNotification: (n: T.Notification) => void;
  channels: string[];
  notifications: T.Notification[];
  project: NotificationProject;
  removeNotification: (n: T.Notification) => void;
  types: string[];
}

export default class ProjectNotifications extends React.PureComponent<Props> {
  getCheckboxId = (type: string, channel: string) => {
    return `project-notification-${this.props.project.key}-${type}-${channel}`;
  };

  handleAddNotification = ({ channel, type }: { channel: string; type: string }) => {
    this.props.addNotification({
      channel,
      type,
      project: this.props.project.key,
      projectName: this.props.project.name,
      organization: this.props.project.organization
    });
  };

  handleRemoveNotification = ({ channel, type }: { channel: string; type: string }) => {
    this.props.removeNotification({
      channel,
      type,
      project: this.props.project.key
    });
  };

  render() {
    const { project, channels } = this.props;

    return (
      <table className="form big-spacer-bottom" key={project.key}>
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
              <th className="text-center" key={channel}>
                <h4>{translate('notification.channel', channel)}</h4>
              </th>
            ))}
          </tr>
        </thead>
        <NotificationsList
          channels={this.props.channels}
          checkboxId={this.getCheckboxId}
          notifications={this.props.notifications}
          onAdd={this.handleAddNotification}
          onRemove={this.handleRemoveNotification}
          project={true}
          types={this.props.types}
        />
      </table>
    );
  }
}
