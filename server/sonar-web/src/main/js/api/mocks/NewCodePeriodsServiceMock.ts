/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { mockNewCodePeriod } from '../../helpers/mocks/new-code-period';
import { NewCodePeriod, NewCodePeriodSettingType } from '../../types/types';
import { getNewCodePeriod, setNewCodePeriod } from '../newCodePeriod';

jest.mock('../newCodePeriod');

const defaultNewCodePeriod = mockNewCodePeriod();

export default class NewCodePeriodsServiceMock {
  #newCodePeriod: NewCodePeriod;

  constructor() {
    this.#newCodePeriod = cloneDeep(defaultNewCodePeriod);
    jest.mocked(getNewCodePeriod).mockImplementation(this.handleGetNewCodePeriod);
    jest.mocked(setNewCodePeriod).mockImplementation(this.handleSetNewCodePeriod);
  }

  handleGetNewCodePeriod = () => {
    return this.reply(this.#newCodePeriod);
  };

  handleSetNewCodePeriod = (data: {
    project?: string;
    branch?: string;
    type: NewCodePeriodSettingType;
    value?: string;
  }) => {
    const { type, value } = data;
    this.#newCodePeriod = mockNewCodePeriod({ type, value });
    return this.reply(undefined);
  };

  reset = () => {
    this.#newCodePeriod = cloneDeep(defaultNewCodePeriod);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
