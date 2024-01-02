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
import { cloneDeep } from 'lodash';
import {
  checkMessageDismissed,
  MessageDismissParams,
  MessageTypes,
  setMessageDismissed,
} from '../messages';

jest.mock('../messages');

interface Dismissed {
  dismissed: boolean;
}

interface ProjectDismissed {
  [projectKey: string]: Dismissed;
}

export default class MessagesServiceMock {
  #messageResponse: {
    [key in MessageTypes]?: ProjectDismissed | Dismissed;
  };

  constructor() {
    this.#messageResponse = {};
    jest.mocked(checkMessageDismissed).mockImplementation(this.handleCheckMessageDismissed);
    jest.mocked(setMessageDismissed).mockImplementation(this.handleSetMessageDismissed);
  }

  handleCheckMessageDismissed = (data: MessageDismissParams) => {
    const result = this.getMessageDismissed(data);
    return this.reply(result as Dismissed);
  };

  handleSetMessageDismissed = (data: MessageDismissParams) => {
    this.setMessageDismissed(data);
    return Promise.resolve();
  };

  setMessageDismissed = ({ projectKey, messageType }: MessageDismissParams) => {
    if (projectKey) {
      this.#messageResponse[messageType] ||= {
        ...this.#messageResponse[messageType],
        [projectKey]: {
          dismissed: true,
        },
      };
    } else {
      this.#messageResponse[messageType] = {
        ...this.#messageResponse[messageType],
        dismissed: true,
      };
    }
  };

  getMessageDismissed = ({ projectKey, messageType }: MessageDismissParams) => {
    const dismissed = projectKey
      ? (this.#messageResponse[messageType] as ProjectDismissed)?.[projectKey]
      : this.#messageResponse[messageType];
    return dismissed || { dismissed: false };
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }

  reset = () => {
    this.#messageResponse = {};
  };
}
