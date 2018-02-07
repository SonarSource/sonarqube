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
import NotificationsList from './NotificationsList';
import { addNotification, removeNotification } from './actions';
import { translate } from '../../../helpers/l10n';
import {
  getGlobalNotifications,
  getNotificationChannels,
  getNotificationGlobalTypes
} from '../../../store/rootReducer';
/*:: import type {
  Notification,
  NotificationsState,
  ChannelsState,
  TypesState
} from '../../../store/notifications/duck'; */

/*::
type Props = {
  notifications: NotificationsState,
  channels: ChannelsState,
  types: TypesState,
  addNotification: (n: Notification) => void,
  removeNotification: (n: Notification) => void
};
*/

function GlobalNotifications(props /*: Props */) {
  return (
    <section className="boxed-group">
      <h2>{translate('my_profile.overall_notifications.title')}</h2>

      <div className="boxed-group-inner">
        <table className="form">
          <thead>
            <tr>
              <th />
              {props.channels.map(channel => (
                <th key={channel} className="text-center">
                  <h4>{translate('notification.channel', channel)}</h4>
                </th>
              ))}
            </tr>
          </thead>

          <NotificationsList
            notifications={props.notifications}
            channels={props.channels}
            types={props.types}
            checkboxId={(d, c) => `global-notification-${d}-${c}`}
            onAdd={props.addNotification}
            onRemove={props.removeNotification}
          />
        </table>
      </div>
    </section>
  );
}

const mapStateToProps = state => ({
  notifications: getGlobalNotifications(state),
  channels: getNotificationChannels(state),
  types: getNotificationGlobalTypes(state)
});

const mapDispatchToProps = { addNotification, removeNotification };

export default connect(mapStateToProps, mapDispatchToProps)(GlobalNotifications);

export const UnconnectedGlobalNotifications = GlobalNotifications;
