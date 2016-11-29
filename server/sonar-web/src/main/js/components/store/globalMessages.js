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
import uniqueId from 'lodash/uniqueId';
import { actions } from '../../app/store/appState/duck';

export const ERROR = 'ERROR';
export const SUCCESS = 'SUCCESS';

/* Actions */
const ADD_GLOBAL_MESSAGE = 'ADD_GLOBAL_MESSAGE';

const addGlobalMessage = (message, level) => ({
  type: ADD_GLOBAL_MESSAGE,
  message,
  level
});

export const addGlobalErrorMessage = message =>
    addGlobalMessage(message, ERROR);

export const addGlobalSuccessMessage = message =>
    addGlobalMessage(message, SUCCESS);

const CLOSE_ALL_GLOBAL_MESSAGES = 'CLOSE_ALL_GLOBAL_MESSAGES';

export const closeAllGlobalMessages = id => ({
  type: CLOSE_ALL_GLOBAL_MESSAGES,
  id
});

/* Reducer */
const globalMessages = (state = [], action = {}) => {
  if (action.type === ADD_GLOBAL_MESSAGE) {
    return [{
      id: uniqueId('global-message-'),
      message: action.message,
      level: action.level
    }];
  }

  if (action.type === actions.REQUIRE_AUTHENTICATION) {
    // FIXME l10n
    return [{
      id: uniqueId('global-message-'),
      message: 'Authentication required to see this page.',
      level: ERROR
    }];
  }

  if (action.type === actions.REQUIRE_AUTHORIZATION) {
    // FIXME l10n
    return [{
      id: uniqueId('global-message-'),
      message: 'You are not authorized to access this page. Please log in with more privileges and try again.',
      level: ERROR
    }];
  }

  if (action.type === CLOSE_ALL_GLOBAL_MESSAGES) {
    return [];
  }

  return state;
};

export default globalMessages;

/* Selectors */
export const getGlobalMessages = state => state;
