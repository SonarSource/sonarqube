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

import { Heading } from '@sonarsource/echoes-react';
import { Table } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Notification, NotificationGlobalType } from '../../../types/notifications';
import NotificationsList from './NotificationsList';

interface Props {
  addNotification: (n: Notification) => void;
  channels: string[];
  header?: React.JSX.Element;
  notifications: Notification[];
  removeNotification: (n: Notification) => void;
  types: NotificationGlobalType[];
}

export default function GlobalNotifications(props: Readonly<Props>) {
  return (
    <>
      <Heading as="h2" hasMarginBottom>
        {translate('my_profile.overall_notifications.title')}
      </Heading>

      {!props.header && (
        <div className="sw-typo-semibold sw-mb-2">{translate('notifications.send_email')}</div>
      )}

      <Table className="sw-w-full" columnCount={2} header={props.header ?? null}>
        <NotificationsList
          channels={props.channels}
          checkboxId={getCheckboxId}
          notifications={props.notifications}
          onAdd={props.addNotification}
          onRemove={props.removeNotification}
          types={props.types}
        />
      </Table>
    </>
  );
}

function getCheckboxId(type: string, channel: string) {
  return `global-notification-${type}-${channel}`;
}
