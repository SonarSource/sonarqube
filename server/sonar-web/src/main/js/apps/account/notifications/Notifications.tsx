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
import { partition, uniqWith } from 'lodash';
import * as React from 'react';
import Helmet from 'react-helmet';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import * as api from '../../../api/notifications';
import GlobalNotifications from './GlobalNotifications';
import Projects from './Projects';

interface State {
  channels: string[];
  globalTypes: string[];
  initialProjectNotificationsCount: number;
  loading: boolean;
  notifications: T.Notification[];
  perProjectTypes: string[];
}

export default class Notifications extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {
    channels: [],
    globalTypes: [],
    initialProjectNotificationsCount: 0,
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
    api.getNotifications().then(
      response => {
        if (this.mounted) {
          const { notifications } = response;
          const { projectNotifications } = this.getNotificationUpdates(notifications);

          this.setState({
            channels: response.channels,
            globalTypes: response.globalTypes,
            initialProjectNotificationsCount: projectNotifications.length,
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
      const notifications = uniqWith([...state.notifications, added], areNotificationsEqual);
      return { notifications };
    });
  };

  removeNotificationFromState = (removed: T.Notification) => {
    this.setState(state => ({
      notifications: state.notifications.filter(
        notification => !areNotificationsEqual(notification, removed)
      )
    }));
  };

  addNotification = (added: T.Notification) => {
    // optimistic update
    this.addNotificationToState(added);

    // recreate `data` to omit `projectName` and `organization` from `Notification`
    const data = { channel: added.channel, project: added.project, type: added.type };
    api.addNotification(data).catch(() => {
      this.removeNotificationFromState(added);
    });
  };

  removeNotification = (removed: T.Notification) => {
    // optimistic update
    this.removeNotificationFromState(removed);

    // recreate `data` to omit `projectName` and `organization` from `Notification`
    const data = { channel: removed.channel, project: removed.project, type: removed.type };
    api.removeNotification(data).catch(() => {
      this.addNotificationToState(removed);
    });
  };

  getNotificationUpdates = (notifications: T.Notification[]) => {
    const [globalNotifications, projectNotifications] = partition(notifications, n => !n.project);

    return {
      globalNotifications,
      projectNotifications
    };
  };

  render() {
    const { initialProjectNotificationsCount, notifications } = this.state;
    const { globalNotifications, projectNotifications } = this.getNotificationUpdates(
      notifications
    );

    return (
      <div className="account-body account-container">
        <Helmet title={translate('my_account.notifications')} />
        <Alert variant="info">{translate('notification.dispatcher.information')}</Alert>
        <DeferredSpinner loading={this.state.loading}>
          {this.state.notifications && (
            <>
              <GlobalNotifications
                addNotification={this.addNotification}
                channels={this.state.channels}
                notifications={globalNotifications}
                removeNotification={this.removeNotification}
                types={this.state.globalTypes}
              />
              <Projects
                addNotification={this.addNotification}
                channels={this.state.channels}
                initialProjectNotificationsCount={initialProjectNotificationsCount}
                notifications={projectNotifications}
                removeNotification={this.removeNotification}
                types={this.state.perProjectTypes}
              />
            </>
          )}
        </DeferredSpinner>
      </div>
    );
  }
}

function areNotificationsEqual(a: T.Notification, b: T.Notification) {
  return a.channel === b.channel && a.type === b.type && a.project === b.project;
}
