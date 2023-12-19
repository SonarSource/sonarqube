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
import { cloneDeep, last } from 'lodash';
import { mockUserToken } from '../../helpers/mocks/token';
import { NewUserToken, TokenType, UserToken } from '../../types/token';
import { generateToken, getTokens, revokeToken } from '../user-tokens';

jest.mock('../../api/user-tokens');

const defaultTokens = [
  mockUserToken({
    name: 'local-scanner',
    createdAt: '2022-03-07T09:02:59+0000',
    lastConnectionDate: '2022-04-07T09:51:48+0000',
  }),
  mockUserToken({
    name: 'test',
    createdAt: '2020-01-23T19:25:19+0000',
  }),
];

export default class UserTokensMock {
  tokens: Array<Partial<NewUserToken> & UserToken>;
  failGeneration = false;

  constructor() {
    this.tokens = cloneDeep(defaultTokens);

    jest.mocked(getTokens).mockImplementation(this.handleGetTokens);
    jest.mocked(generateToken).mockImplementation(this.handleGenerateToken);
    jest.mocked(revokeToken).mockImplementation(this.handleRevokeToken);
  }

  handleGetTokens = () => {
    return Promise.resolve(cloneDeep(this.tokens));
  };

  handleGenerateToken = ({
    name,
    login,
    type,
    projectKey,
    expirationDate,
  }: {
    name: string;
    login: string;
    type: TokenType;
    projectKey: string;
    expirationDate?: string;
  }) => {
    if (this.failGeneration) {
      this.failGeneration = false;
      return Promise.reject('x_x');
    }

    if (this.tokens.some((t) => t.name === name)) {
      return Promise.reject('This name is already used');
    }

    const token = {
      name,
      login,
      type,
      projectKey,
      isExpired: false,
      token: `generatedtoken${this.tokens.length}`,
      createdAt: '2022-04-04T04:04:04+0000',
      expirationDate,
    };

    this.tokens.push(token);

    return Promise.resolve(token);
  };

  handleRevokeToken = ({ name }: { name: string; login?: string }) => {
    this.tokens = this.tokens.filter((t) => t.name !== name);

    return Promise.resolve();
  };

  failNextTokenGeneration = () => {
    this.failGeneration = true;
  };

  getTokens = () => {
    return cloneDeep(this.tokens);
  };

  getLastToken = () => {
    return last(this.getTokens());
  };

  reset = () => {
    this.tokens = cloneDeep(defaultTokens);
  };
}
