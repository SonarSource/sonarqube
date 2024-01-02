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
import { TokenType } from '../../types/token';
import { HttpStatus } from '../request';
import {
  buildPortRange,
  openHotspot,
  portIsValid,
  probeSonarLintServers,
  sendUserToken,
} from '../sonarlint';

describe('buildPortRange', () => {
  it('should build a port range of size <size> starting at port <port>', () => {
    expect(buildPortRange(10000, 5)).toStrictEqual([10000, 10001, 10002, 10003, 10004]);
  });
});

describe('probeSonarLintServers', () => {
  const sonarLintResponse = { ideName: 'BlueJ IDE', description: 'Hello World' };

  window.fetch = jest.fn((input: RequestInfo) => {
    const calledPort = new URL(input.toString()).port;
    if (calledPort === '64120') {
      const resp = new Response();
      resp.json = () => Promise.resolve(sonarLintResponse);
      return Promise.resolve(resp);
    } else {
      return Promise.reject('oops');
    }
  });

  it('should probe all ports in range', async () => {
    const results = await probeSonarLintServers();
    expect(results).toStrictEqual([{ port: 64120, ...sonarLintResponse }]);
  });
});

describe('openHotspot', () => {
  it('should send request to IDE on the right port', async () => {
    const resp = new Response();
    window.fetch = jest.fn((input: RequestInfo) => {
      const calledUrl = new URL(input.toString());
      try {
        expect(calledUrl.searchParams.get('server')).toStrictEqual('http://localhost');
        expect(calledUrl.searchParams.get('project')).toStrictEqual('my-project:key');
        expect(calledUrl.searchParams.get('hotspot')).toStrictEqual('my-hotspot-key');
      } catch (error) {
        return Promise.reject(error);
      }
      return Promise.resolve(resp);
    });

    const result = await openHotspot(42000, 'my-project:key', 'my-hotspot-key');
    expect(result).toBe(resp);
  });
});

describe('portIsValid', () => {
  it.each([
    [64119, false],
    [64120, true],
    [64125, true],
    [64130, true],
    [64131, false],
  ])('should validate port %s is within the expected range', (port, expectation) => {
    expect(portIsValid(port)).toBe(expectation);
  });
});

describe('sendUserToken', () => {
  it('should send the token the right port', async () => {
    const token = {
      login: 'Takeshi',
      name: 'sonarlint-vscode-1',
      createdAt: '12-12-2018',
      expirationDate: '17-02-2019',
      token: '78gfh78d6gf8h',
      type: TokenType.User,
    };

    const resp = new Response();
    window.fetch = jest.fn((_url, { body }: RequestInit) => {
      try {
        const data = JSON.parse(body?.toString() ?? '{}');

        expect(data).toEqual(token);
      } catch (error) {
        return Promise.reject(error);
      }
      return Promise.resolve(resp);
    });

    const result = await sendUserToken(64122, { ...token, isExpired: false });
    expect(result).toBeUndefined();
  });

  it('should handle errors', async () => {
    const token = {
      login: 'Takeshi',
      name: 'sonarlint-vscode-1',
      createdAt: '12-12-2018',
      expirationDate: '17-02-2019',
      token: '78gfh78d6gf8h',
      type: TokenType.User,
    };

    const resp = new Response('Meh', { status: HttpStatus.BadRequest, statusText: 'I no likez' });
    window.fetch = jest.fn(() => {
      return Promise.resolve(resp);
    });

    await expect(async () => {
      await sendUserToken(64122, { ...token, isExpired: false });
    }).rejects.toThrow('400 I no likez. Meh');
  });
});
