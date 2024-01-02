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
import { uniqueId } from 'lodash';
import { Message, MessageLevel } from '../types/globalMessages';
import { parseError } from './request';

const listeners: Array<(message: Message) => void> = [];

export function registerListener(callback: (message: Message) => void) {
  listeners.push(callback);
}

export function unregisterListener(callback: (message: Message) => void) {
  const index = listeners.indexOf(callback);

  if (index > -1) {
    listeners.splice(index, 1);
  }
}

function addMessage(text: string, level: MessageLevel) {
  listeners.forEach((listener) =>
    listener({
      id: uniqueId('global-message-'),
      level,
      text,
    })
  );
}

export function addGlobalErrorMessage(text: string) {
  addMessage(text, MessageLevel.Error);
}

export function addGlobalErrorMessageFromAPI(param: Response | string) {
  if (param instanceof Response) {
    return parseError(param).then(addGlobalErrorMessage, () => {
      /* ignore parsing errors */
    });
  }
  if (typeof param === 'string') {
    return Promise.resolve(param).then(addGlobalErrorMessage);
  }

  return Promise.resolve();
}

export function addGlobalSuccessMessage(text: string) {
  addMessage(text, MessageLevel.Success);
}
