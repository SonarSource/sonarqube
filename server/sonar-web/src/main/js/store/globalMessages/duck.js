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
import { uniqueId } from 'lodash';

/*::
type Level = 'ERROR' | 'SUCCESS';
*/

/*::
type Message = {
  id: string,
  message: string,
  level: Level
};
*/

/*::
export type State = Array<Message>;
*/

/*::
type Action = Object;
*/

export const ERROR = 'ERROR';
export const SUCCESS = 'SUCCESS';

/* Actions */
const ADD_GLOBAL_MESSAGE = 'ADD_GLOBAL_MESSAGE';
const CLOSE_GLOBAL_MESSAGE = 'CLOSE_GLOBAL_MESSAGE';
const CLOSE_ALL_GLOBAL_MESSAGES = 'CLOSE_ALL_GLOBAL_MESSAGES';

function addGlobalMessageActionCreator(
  id /*: string */,
  message /*: string */,
  level /*: Level */
) {
  return {
    type: ADD_GLOBAL_MESSAGE,
    message,
    level,
    id
  };
}

export function closeGlobalMessage(id /*: string */) {
  return {
    type: CLOSE_GLOBAL_MESSAGE,
    id
  };
}

export function closeAllGlobalMessages(id /*: string */) {
  return {
    type: CLOSE_ALL_GLOBAL_MESSAGES,
    id
  };
}

function addGlobalMessage(message /*: string */, level /*: Level */) {
  return function(dispatch /*: Function */) {
    const id = uniqueId('global-message-');
    dispatch(addGlobalMessageActionCreator(id, message, level));
    setTimeout(() => dispatch(closeGlobalMessage(id)), 5000);
  };
}

export function addGlobalErrorMessage(message /*: string */) {
  return addGlobalMessage(message, ERROR);
}

export function addGlobalSuccessMessage(message /*: string */) {
  return addGlobalMessage(message, SUCCESS);
}

/* Reducer */
export default function(state /*: State */ = [], action /*: Action */ = {}) {
  switch (action.type) {
    case ADD_GLOBAL_MESSAGE:
      return [
        {
          id: action.id,
          message: action.message,
          level: action.level
        }
      ];

    case 'REQUIRE_AUTHORIZATION':
      // FIXME l10n
      return [
        {
          id: uniqueId('global-message-'),
          message:
            'You are not authorized to access this page. ' +
            'Please log in with more privileges and try again.',
          level: ERROR
        }
      ];

    case CLOSE_GLOBAL_MESSAGE:
      return state.filter(message => message.id !== action.id);

    case CLOSE_ALL_GLOBAL_MESSAGES:
      return [];
    default:
      return state;
  }
}

/* Selectors */
export function getGlobalMessages(state /*: State */) {
  return state;
}
