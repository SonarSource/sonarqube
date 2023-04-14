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
import { mockNewCodePeriod, mockNewCodePeriodBranch } from '../../helpers/mocks/new-code-period';
import { NewCodePeriod, NewCodePeriodBranch, NewCodePeriodSettingType } from '../../types/types';
import {
  getNewCodePeriod,
  listBranchesNewCodePeriod,
  resetNewCodePeriod,
  setNewCodePeriod,
} from '../newCodePeriod';

jest.mock('../newCodePeriod');
export default class NewCodePeriodsServiceMock {
  #defaultNewCodePeriod = mockNewCodePeriod({ inherited: true });
  #defaultListBranchesNewCode = [
    mockNewCodePeriodBranch({ inherited: true, branchKey: 'main' }),
    mockNewCodePeriodBranch({
      branchKey: 'feature',
      type: NewCodePeriodSettingType.NUMBER_OF_DAYS,
      value: '1',
    }),
  ];

  #newCodePeriod: NewCodePeriod;
  #listBranchesNewCode: NewCodePeriodBranch[];

  constructor() {
    this.#newCodePeriod = cloneDeep(this.#defaultNewCodePeriod);
    this.#listBranchesNewCode = cloneDeep(this.#defaultListBranchesNewCode);
    jest.mocked(getNewCodePeriod).mockImplementation(this.handleGetNewCodePeriod);
    jest.mocked(setNewCodePeriod).mockImplementation(this.handleSetNewCodePeriod);
    jest.mocked(resetNewCodePeriod).mockImplementation(this.handleResetNewCodePeriod);
    jest.mocked(listBranchesNewCodePeriod).mockImplementation(this.handleListBranchesNewCodePeriod);
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
    const { type, value, branch } = data;
    if (branch) {
      const branchNewCode = this.#listBranchesNewCode.find(
        (bNew) => bNew.branchKey === branch
      ) as NewCodePeriodBranch;
      branchNewCode.type = type;
      branchNewCode.value = value;
    } else {
      this.#newCodePeriod = mockNewCodePeriod({ type, value });
    }

    return this.reply(undefined);
  };

  handleResetNewCodePeriod = (data: { project?: string; branch?: string }) => {
    const { branch } = data;
    if (branch) {
      const index = this.#listBranchesNewCode.findIndex((bNew) => bNew.branchKey === branch);
      if (index >= 0) {
        Object.assign(this.#listBranchesNewCode[index], cloneDeep(this.#defaultNewCodePeriod));
      }
    } else {
      this.#newCodePeriod = cloneDeep(this.#defaultNewCodePeriod);
    }

    return this.reply(undefined);
  };

  handleListBranchesNewCodePeriod = () => {
    return this.reply({ newCodePeriods: this.#listBranchesNewCode });
  };

  reset = () => {
    this.#newCodePeriod = cloneDeep(this.#defaultNewCodePeriod);
    this.#listBranchesNewCode = cloneDeep(this.#defaultListBranchesNewCode);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
