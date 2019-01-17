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
import Helmet from 'react-helmet';
import { groupBy, partition, uniq, uniqBy, uniqWith } from 'lodash';
import GlobalNotifications from './GlobalNotifications';
import Projects from './Projects';
import { NotificationProject } from './types';
import * as api from '../../../api/notifications';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { Alert } from '../../../components/ui/Alert';
import { withAppState } from '../../../components/hoc/withAppState';

export interface Props {
  appState: Pick<T.AppState, 'organizationsEnabled'>;
  fetchOrganizations: (organizations: string[]) => void;
}

interface State {
  channels: string[];
  globalTypes: string[];
  loading: boolean;
  notifications: T.Notification[];
  perProjectTypes: string[];
}

export class Notifications extends React.PureComponent<Props, State> {
  mounted = false;
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
    api.getNotifications().then(
      response => {
        if (this.mounted) {
          if (this.props.appState.organizationsEnabled) {
            const organizations = uniq(response.notifications
              .filter(n => n.organization)
              .map(n => n.organization) as string[]);
            this.props.fetchOrganizations(organizations);
          }

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
    this.setState(state => ({
      notifications: uniqWith([...state.notifications, added], areNotificationsEqual)
    }));
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

  render() {
    const [globalNotifications, projectNotifications] = partition(
      this.state.notifications,
      n => !n.project
    );
    const projects = uniqBy(
      projectNotifications.map(n => ({
        key: n.project,
        name: n.projectName,
        organization: n.organization
      })) as NotificationProject[],
      project => project.key
    );
    const notificationsByProject = groupBy(projectNotifications, n => n.project);

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
                notificationsByProject={notificationsByProject}
                projects={projects}
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

export default withAppState(Notifications);

function areNotificationsEqual(a: T.Notification, b: T.Notification) {
  return a.channel === b.channel && a.type === b.type && a.project === b.project;
}
