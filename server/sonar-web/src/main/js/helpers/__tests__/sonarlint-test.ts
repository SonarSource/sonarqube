/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { buildPortRange, openHotspot, probeSonarLintServers } from '../sonarlint';

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
