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
// @flow
import { combineReducers } from 'redux';
import { uniqBy, uniqWith } from 'lodash';

/*::
export type Notification = {
  channel: string,
  type: string,
  project?: string,
  projectName?: string,
  organization?: string
};
*/

/*::
export type NotificationsState = Array<Notification>;
*/
/*::
export type ChannelsState = Array<string>;
*/
/*::
export type TypesState = Array<string>;
*/

/*::
type AddNotificationAction = {
  type: 'ADD_NOTIFICATION',
  notification: Notification
};
*/

/*::
type RemoveNotificationAction = {
  type: 'REMOVE_NOTIFICATION',
  notification: Notification
};
*/

/*::
type ReceiveNotificationsAction = {
  type: 'RECEIVE_NOTIFICATIONS',
  notifications: NotificationsState,
  channels: ChannelsState,
  globalTypes: TypesState,
  perProjectTypes: TypesState
};
*/

/*::
type Action = AddNotificationAction | RemoveNotificationAction | ReceiveNotificationsAction;
*/

export function addNotification(notification /*: Notification */) /*: AddNotificationAction */ {
  return {
    type: 'ADD_NOTIFICATION',
    notification
  };
}

export function removeNotification(
  notification /*: Notification */
) /*: RemoveNotificationAction */ {
  return {
    type: 'REMOVE_NOTIFICATION',
    notification
  };
}

export function receiveNotifications(
  notifications /*: NotificationsState */,
  channels /*: ChannelsState */,
  globalTypes /*: TypesState */,
  perProjectTypes /*: TypesState */
) /*: ReceiveNotificationsAction */ {
  return {
    type: 'RECEIVE_NOTIFICATIONS',
    notifications,
    channels,
    globalTypes,
    perProjectTypes
  };
}

function onAddNotification(state /*: NotificationsState */, notification /*: Notification */) {
  function isNotificationsEqual(a /*: Notification */, b /*: Notification */) {
    return a.channel === b.channel && a.type === b.type && a.project === b.project;
  }

  return uniqWith([...state, notification], isNotificationsEqual);
}

function onRemoveNotification(state /*: NotificationsState */, notification /*: Notification */) {
  return state.filter(
    n =>
      n.channel !== notification.channel ||
      n.type !== notification.type ||
      n.project !== notification.project
  );
}

function onReceiveNotifications(
  state /*: NotificationsState */,
  notifications /*: NotificationsState */
) {
  return [...notifications];
}

function notifications(state /*: NotificationsState */ = [], action /*: Action */) {
  switch (action.type) {
    case 'ADD_NOTIFICATION':
      return onAddNotification(state, action.notification);
    case 'REMOVE_NOTIFICATION':
      return onRemoveNotification(state, action.notification);
    case 'RECEIVE_NOTIFICATIONS':
      return onReceiveNotifications(state, action.notifications);
    default:
      return state;
  }
}

function channels(state /*: ChannelsState */ = [], action /*: Action */) {
  if (action.type === 'RECEIVE_NOTIFICATIONS') {
    return action.channels;
  } else {
    return state;
  }
}

function globalTypes(state /*: TypesState */ = [], action /*: Action */) {
  if (action.type === 'RECEIVE_NOTIFICATIONS') {
    return action.globalTypes;
  } else {
    return state;
  }
}

function perProjectTypes(state /*: TypesState */ = [], action /*: Action */) {
  if (action.type === 'RECEIVE_NOTIFICATIONS') {
    return action.perProjectTypes;
  } else {
    return state;
  }
}

/*::
type State = {
  notifications: NotificationsState,
  channels: ChannelsState,
  globalTypes: TypesState,
  perProjectTypes: TypesState
};
*/

export default combineReducers({ notifications, channels, globalTypes, perProjectTypes });

export function getGlobal(state /*: State */) /*: NotificationsState */ {
  return state.notifications.filter(n => !n.project);
}

export function getProjects(state /*: State */) /*: Array<{ key: string, name: string }> */ {
  // $FlowFixMe
  const allProjects = state.notifications.filter(n => n.project != null).map(n => ({
    key: n.project,
    name: n.projectName,
    organization: n.organization
  }));

  return uniqBy(allProjects, project => project.key);
}

export function getForProject(state /*: State */, project /*: string */) /*: NotificationsState */ {
  return state.notifications.filter(n => n.project === project);
}

export function getChannels(state /*: State */) /*: ChannelsState */ {
  return state.channels;
}

export function getGlobalTypes(state /*: State */) /*: TypesState */ {
  return state.globalTypes;
}

export function getPerProjectTypes(state /*: State */) /*: TypesState */ {
  return state.perProjectTypes;
}
