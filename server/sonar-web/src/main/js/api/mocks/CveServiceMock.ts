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
import { mockCve } from '../../helpers/testMocks';
import { Cve } from '../../types/cves';
import { getCve } from '../cves';

jest.mock('../../api/cves');

export const DEFAULT_CVE_LIST = [
  mockCve({ id: 'CVE-2021-12345' }),
  mockCve({ id: 'CVE-2021-12346' }),
];

export default class CveServiceMock {
  private cveList: Cve[];

  constructor() {
    this.cveList = cloneDeep(DEFAULT_CVE_LIST);
    jest.mocked(getCve).mockImplementation(this.handleGetCve);
  }

  setCveList(cveList: Cve[]) {
    this.cveList = cveList;
  }

  handleGetCve = (cveId: string) => {
    const cve = this.cveList.find((cve) => cve.id === cveId);
    if (!cve) {
      return Promise.reject(new Error('Cve not found'));
    }
    return this.reply(cve);
  };

  reset = () => {
    this.cveList = cloneDeep(DEFAULT_CVE_LIST);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
