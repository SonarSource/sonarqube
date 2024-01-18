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
import { Checkbox, FlagMessage, Spinner, SubTitle } from 'design-system';
import * as React from 'react';
import {
  WithNotificationsProps,
  withNotifications,
} from '../../../components/hoc/withNotifications';
import { hasMessage, translate, translateWithParameters } from '../../../helpers/l10n';
import { NotificationProjectType } from '../../../types/notifications';
import { Component } from '../../../types/types';

interface Props {
  component: Component;
}

export function ProjectNotifications(props: WithNotificationsProps & Props) {
  const { channels, component, loading, notifications, perProjectTypes } = props;

  const handleCheck = (type: NotificationProjectType, channel: string, checked: boolean) => {
    if (checked) {
      props.addNotification({ project: component.key, channel, type });
    } else {
      props.removeNotification({
        project: component.key,
        channel,
        type,
      });
    }
  };

  const getCheckboxId = (type: string, channel: string) => {
    return `project-notification-${component.key}-${type}-${channel}`;
  };

  const getDispatcherLabel = (dispatcher: string) => {
    const globalMessageKey = ['notification.dispatcher', dispatcher];
    const projectMessageKey = [...globalMessageKey, 'project'];
    const shouldUseProjectMessage = hasMessage(...projectMessageKey);
    return shouldUseProjectMessage
      ? translate(...projectMessageKey)
      : translate(...globalMessageKey);
  };

  const isEnabled = (type: string, channel: string) => {
    return !!notifications.find(
      (notification) =>
        notification.type === type &&
        notification.channel === channel &&
        notification.project === component.key,
    );
  };

  const emailChannel = channels[0];

  return (
    <form aria-labelledby="notifications-update-title">
      <SubTitle>{translate('project.info.notifications')}</SubTitle>

      <FlagMessage className="spacer-top" variant="info">
        {translate('notification.dispatcher.information')}
      </FlagMessage>

      <Spinner className="sw-mt-6" loading={loading}>
        <h3 id="notifications-update-title" className="sw-mt-6">
          {translate('project_information.project_notifications.title')}
        </h3>
        <ul className="sw-list-none sw-mt-4 sw-pl-0">
          {perProjectTypes.map((type) => (
            <li className="sw-pl-0 sw-p-2" key={type}>
              <Checkbox
                right
                className="sw-flex sw-justify-between"
                label={translateWithParameters(
                  'notification.dispatcher.descrption_x',
                  getDispatcherLabel(type),
                )}
                checked={isEnabled(type, emailChannel)}
                id={getCheckboxId(type, emailChannel)}
                onCheck={(checked: boolean) => handleCheck(type, emailChannel, checked)}
              >
                {getDispatcherLabel(type)}
              </Checkbox>
            </li>
          ))}
        </ul>
      </Spinner>
    </form>
  );
}

export default withNotifications(ProjectNotifications);
