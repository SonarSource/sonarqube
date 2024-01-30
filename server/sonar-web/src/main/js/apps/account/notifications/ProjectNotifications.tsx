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
import classNames from 'classnames';
import { Link, Table } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';
import {
  Notification,
  NotificationProject,
  NotificationProjectType,
} from '../../../types/notifications';
import NotificationsList from './NotificationsList';

interface Props {
  addNotification: (n: Notification) => void;
  channels: string[];
  header?: React.JSX.Element;
  notifications: Notification[];
  project: NotificationProject;
  removeNotification: (n: Notification) => void;
  types: NotificationProjectType[];
}

export default function ProjectNotifications({
  addNotification,
  channels,
  header,
  notifications,
  project,
  removeNotification,
  types,
}: Readonly<Props>) {
  const getCheckboxId = (type: string, channel: string) => {
    return `project-notification-${project.project}-${type}-${channel}`;
  };

  const handleAddNotification = ({ channel, type }: { channel: string; type: string }) => {
    addNotification({ ...project, channel, type });
  };

  const handleRemoveNotification = ({ channel, type }: { channel: string; type: string }) => {
    removeNotification({
      ...project,
      channel,
      type,
    });
  };

  return (
    <div className="sw-my-6">
      <div className="sw-mb-4">
        <Link to={getProjectUrl(project.project)}>{project.projectName}</Link>
      </div>
      {!header && (
        <div className="sw-body-sm-highlight sw-mb-2">{translate('notifications.send_email')}</div>
      )}

      <Table
        className={classNames('sw-w-full', { 'sw-mt-4': header })}
        columnCount={2}
        header={header ?? null}
      >
        <NotificationsList
          channels={channels}
          checkboxId={getCheckboxId}
          notifications={notifications}
          onAdd={handleAddNotification}
          onRemove={handleRemoveNotification}
          project
          types={types}
        />
      </Table>
    </div>
  );
}
