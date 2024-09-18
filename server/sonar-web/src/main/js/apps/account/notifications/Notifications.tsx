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

import { Heading, Spinner } from '@sonarsource/echoes-react';
import { FlagMessage, GreySeparator } from 'design-system';
import { isEmpty, partition } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import {
  WithNotificationsProps,
  withNotifications,
} from '../../../components/hoc/withNotifications';
import { translate } from '../../../helpers/l10n';
import GlobalNotifications from './GlobalNotifications';
import Projects from './Projects';

export function Notifications({
  addNotification,
  channels,
  globalTypes,
  loading,
  notifications,
  perProjectTypes,
  removeNotification,
}: WithNotificationsProps) {
  const [globalNotifications, projectNotifications] = partition(notifications, (n) =>
    isEmpty(n.project),
  );

  const emailOnly = channels.length === 1 && channels[0] === 'EmailNotificationChannel';

  const header = emailOnly ? undefined : (
    <tr>
      <th className="sw-body-sm-highlight">{translate('events')}</th>

      {channels.map((channel) => (
        <th className="sw-body-sm-highlight sw-text-right" key={channel}>
          {translate('notification.channel', channel)}
        </th>
      ))}
    </tr>
  );

  return (
    <div className="it__account-body">
      <Helmet defer={false} title={translate('my_account.notifications')} />

      <Heading as="h1" hasMarginBottom>
        {translate('my_account.notifications')}
      </Heading>

      <FlagMessage className="sw-my-2" variant="info">
        {translate('notification.dispatcher.information')}
      </FlagMessage>

      <Spinner isLoading={loading}>
        {notifications && (
          <>
            <GreySeparator className="sw-mb-4 sw-mt-6" />

            <GlobalNotifications
              addNotification={addNotification}
              channels={channels}
              header={header}
              notifications={globalNotifications}
              removeNotification={removeNotification}
              types={globalTypes}
            />

            <GreySeparator className="sw-mb-4 sw-mt-6" />

            <Projects
              addNotification={addNotification}
              channels={channels}
              header={header}
              notifications={projectNotifications}
              removeNotification={removeNotification}
              types={perProjectTypes}
            />
          </>
        )}
      </Spinner>
    </div>
  );
}

export default withNotifications(Notifications);
