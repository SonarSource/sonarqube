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
import NotificationsList from '../../../../../../apps/account/notifications/NotificationsList';
import {
  withNotifications,
  WithNotificationsProps,
} from '../../../../../../components/hoc/withNotifications';
import { Alert } from '../../../../../../components/ui/Alert';
import DeferredSpinner from '../../../../../../components/ui/DeferredSpinner';
import { translate } from '../../../../../../helpers/l10n';
import { Component } from '../../../../../../types/types';

interface Props {
  component: Component;
}

export function ProjectNotifications(props: WithNotificationsProps & Props) {
  const { channels, component, loading, notifications, perProjectTypes } = props;
  const heading = React.useRef<HTMLHeadingElement>(null);

  React.useEffect(() => {
    if (heading.current) {
      // a11y: provide focus to the heading when the info drawer page is opened.
      heading.current.focus();
    }
  }, [heading]);

  const handleAddNotification = ({ channel, type }: { channel: string; type: string }) => {
    props.addNotification({ project: component.key, channel, type });
  };

  const handleRemoveNotification = ({ channel, type }: { channel: string; type: string }) => {
    props.removeNotification({
      project: component.key,
      channel,
      type,
    });
  };

  const getCheckboxId = (type: string, channel: string) => {
    return `project-notification-${component.key}-${type}-${channel}`;
  };

  const projectNotifications = notifications.filter(
    (n) => n.project && n.project === component.key
  );

  return (
    <>
      <h3 tabIndex={-1} ref={heading}>
        {translate('project.info.notifications')}
      </h3>

      <Alert className="spacer-top" variant="info" aria-live="off">
        {translate('notification.dispatcher.information')}
      </Alert>

      <DeferredSpinner loading={loading}>
        <table className="data zebra notifications-table">
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
            channels={channels}
            checkboxId={getCheckboxId}
            notifications={projectNotifications}
            onAdd={handleAddNotification}
            onRemove={handleRemoveNotification}
            project={true}
            types={perProjectTypes}
          />
        </table>
      </DeferredSpinner>
    </>
  );
}

export default withNotifications(ProjectNotifications);
