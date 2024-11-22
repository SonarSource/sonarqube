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
import { Mode, ModeResponse } from '../../types/mode';
import { getMode, updateMode } from '../mode';

jest.mock('../mode');

const defaultMode: ModeResponse = {
  mode: Mode.MQR,
  modified: false,
};

export class ModeServiceMock {
  mode: ModeResponse;

  constructor() {
    this.mode = cloneDeep(defaultMode);

    jest.mocked(getMode).mockImplementation(this.handleGetMode);
    jest.mocked(updateMode).mockImplementation(this.handleUpdateMode);
  }

  setMode = (mode: Mode) => {
    this.mode.mode = mode;
  };

  setModified = () => {
    this.mode.modified = true;
  };

  handleGetMode: typeof getMode = () => {
    return this.reply(this.mode);
  };

  handleUpdateMode: typeof updateMode = (mode: Mode) => {
    this.mode.mode = mode;
    this.mode.modified = true;
    return this.reply(this.mode);
  };

  reset = () => {
    this.mode = cloneDeep(defaultMode);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
