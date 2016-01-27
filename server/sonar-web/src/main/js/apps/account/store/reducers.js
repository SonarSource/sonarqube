/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { ADD_PROJECT_NOTIFICATIONS, REMOVE_PROJECT_NOTIFICATIONS } from './actions';

function addProjectNotifications (state, project) {
  const found = state.find(notification => {
    return notification.project.internalId === project.internalId;
  });

  if (found) {
    return state;
  }

  const newProjectNotification = {
    project,
    notifications: window.sonarqube.notifications.projectDispatchers.map(dispatcher => {
      const channels = window.sonarqube.notifications.channels.map(channel => {
        return { id: channel, checked: false };
      });
      return { dispatcher, channels };
    })
  };

  return [...state, newProjectNotification];
}

function removeProjectNotifications (state, project) {
  return state.filter(notification => {
    return notification.project.internalId !== project.internalId;
  });
}

export const initialState = {
  user: window.sonarqube.user,
  projectNotifications: window.sonarqube.notifications.project
};

export default function (state = initialState, action) {
  switch (action.type) {
    case ADD_PROJECT_NOTIFICATIONS:
      return {
        ...state,
        projectNotifications: addProjectNotifications(state.projectNotifications, action.project)
      };
    case REMOVE_PROJECT_NOTIFICATIONS:
      return {
        ...state,
        projectNotifications: removeProjectNotifications(state.projectNotifications, action.project)
      };
    default:
      return state;
  }
}
