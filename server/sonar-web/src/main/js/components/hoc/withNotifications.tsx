/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { uniqWith } from 'lodash';
import * as React from 'react';
import { addNotification, getNotifications, removeNotification } from '../../api/notifications';
import { getWrappedDisplayName } from './utils';

interface State {
  channels: string[];
  globalTypes: string[];
  loading: boolean;
  notifications: T.Notification[];
  perProjectTypes: string[];
}

export interface WithNotificationsProps {
  addNotification: (added: T.Notification) => void;
  channels: string[];
  globalTypes: string[];
  loading: boolean;
  notifications: T.Notification[];
  perProjectTypes: string[];
  removeNotification: (removed: T.Notification) => void;
}

export function withNotifications<P>(
  WrappedComponent: React.ComponentType<P & WithNotificationsProps>
) {
  class Wrapper extends React.Component<P, State> {
    mounted = false;
    static displayName = getWrappedDisplayName(WrappedComponent, 'withNotifications');

    state: State = {
      channels: [],
      globalTypes: [],
      loading: true,
      notifications: [],
      perProjectTypes: []
    };

    componentDidMount() {
      this.mounted = true;
      this.fetchNotifications();
    }

    componentWillUnmount() {
      this.mounted = false;
    }

    fetchNotifications = () => {
      getNotifications().then(
        response => {
          if (this.mounted) {
            this.setState({
              channels: response.channels,
              globalTypes: response.globalTypes,
              loading: false,
              notifications: response.notifications,
              perProjectTypes: response.perProjectTypes
            });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        }
      );
    };

    addNotificationToState = (added: T.Notification) => {
      this.setState(state => {
        const notifications = uniqWith([...state.notifications, added], this.areNotificationsEqual);
        return { notifications };
      });
    };

    removeNotificationFromState = (removed: T.Notification) => {
      this.setState(state => {
        const notifications = state.notifications.filter(
          notification => !this.areNotificationsEqual(notification, removed)
        );
        return { notifications };
      });
    };

    addNotification = (added: T.Notification) => {
      // optimistic update
      this.addNotificationToState(added);

      // recreate `data` to omit `projectName` and `organization` from `Notification`
      const data = { channel: added.channel, project: added.project, type: added.type };
      addNotification(data).catch(() => {
        this.removeNotificationFromState(added);
      });
    };

    removeNotification = (removed: T.Notification) => {
      // optimistic update
      this.removeNotificationFromState(removed);

      // recreate `data` to omit `projectName` and `organization` from `Notification`
      const data = { channel: removed.channel, project: removed.project, type: removed.type };
      removeNotification(data).catch(() => {
        this.addNotificationToState(removed);
      });
    };

    areNotificationsEqual = (a: T.Notification, b: T.Notification) => {
      return a.channel === b.channel && a.type === b.type && a.project === b.project;
    };

    render() {
      const { channels, globalTypes, loading, notifications, perProjectTypes } = this.state;
      return (
        <WrappedComponent
          {...this.props}
          addNotification={this.addNotification}
          channels={channels}
          globalTypes={globalTypes}
          loading={loading}
          notifications={notifications}
          perProjectTypes={perProjectTypes}
          removeNotification={this.removeNotification}
        />
      );
    }
  }

  return Wrapper;
}
