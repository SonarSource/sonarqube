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
import * as React from 'react';
import BoxedGroupAccordion from '../../../components/controls/BoxedGroupAccordion';
import { translate } from '../../../helpers/l10n';
import {
  Notification,
  NotificationProject,
  NotificationProjectType,
} from '../../../types/notifications';
import NotificationsList from './NotificationsList';

interface Props {
  addNotification: (n: Notification) => void;
  channels: string[];
  collapsed: boolean;
  notifications: Notification[];
  project: NotificationProject;
  removeNotification: (n: Notification) => void;
  types: NotificationProjectType[];
}

export default function ProjectNotifications(props: Props) {
  const { collapsed, project, channels } = props;
  const [isCollapsed, setCollapsed] = React.useState<boolean>(collapsed);

  const getCheckboxId = (type: string, channel: string) => {
    return `project-notification-${props.project.project}-${type}-${channel}`;
  };

  const handleAddNotification = ({ channel, type }: { channel: string; type: string }) => {
    props.addNotification({ ...props.project, channel, type });
  };

  const handleRemoveNotification = ({ channel, type }: { channel: string; type: string }) => {
    props.removeNotification({
      ...props.project,
      channel,
      type,
    });
  };

  const toggleExpanded = () => setCollapsed(!isCollapsed);

  return (
    <BoxedGroupAccordion
      onClick={toggleExpanded}
      open={!isCollapsed}
      title={<h4 className="display-inline-block">{project.projectName}</h4>}
    >
      <table className="data zebra notifications-table" key={project.project}>
        <thead>
          <tr>
            <th aria-label={translate('project')} />
            {channels.map((channel) => (
              <th className="text-center" key={channel}>
                <h4>{translate('notification.channel', channel)}</h4>
              </th>
            ))}
          </tr>
        </thead>

        <NotificationsList
          channels={props.channels}
          checkboxId={getCheckboxId}
          notifications={props.notifications}
          onAdd={handleAddNotification}
          onRemove={handleRemoveNotification}
          project={true}
          types={props.types}
        />
      </table>
    </BoxedGroupAccordion>
  );
}
