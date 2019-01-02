/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as u from '../utils';
import { ClusterSysInfo, SysInfo, SystemUpgrade } from '../../../api/system';

describe('parseQuery', () => {
  it('should correctly parse the expand array', () => {
    expect(u.parseQuery({})).toEqual({ expandedCards: [] });
    expect(u.parseQuery({ expand: 'foo,bar' })).toEqual({ expandedCards: ['foo', 'bar'] });
  });
});

describe('serializeQuery', () => {
  it('should correctly serialize the expand array', () => {
    expect(u.serializeQuery({ expandedCards: [] })).toEqual({});
    expect(u.serializeQuery({ expandedCards: ['foo', 'bar'] })).toEqual({ expand: 'foo,bar' });
  });
});

describe('groupSections', () => {
  it('should correctly group the root field into a main section', () => {
    expect(u.groupSections({ foo: 'Foo', bar: 3, baz: { a: 'a' } })).toEqual({
      mainSection: { foo: 'Foo', bar: 3 },
      sections: { baz: { a: 'a' } }
    });
  });
});

describe('getSystemLogsLevel', () => {
  it('should correctly return log level for standalone mode', () => {
    expect(u.getSystemLogsLevel({ System: { 'Logs Level': 'FOO' } } as SysInfo)).toBe('FOO');
    expect(u.getSystemLogsLevel({} as SysInfo)).toBe('INFO');
    expect(u.getSystemLogsLevel()).toBe('INFO');
  });

  it('should return the worst log level for cluster mode', () => {
    expect(
      u.getSystemLogsLevel({
        System: { 'High Availability': true },
        'Application Nodes': [
          {
            'Compute Engine Logging': { 'Logs Level': 'DEBUG' },
            'Web Logging': { 'Logs Level': 'INFO' }
          },
          {
            'Compute Engine Logging': { 'Logs Level': 'INFO' },
            'Web Logging': { 'Logs Level': 'INFO' }
          }
        ]
      } as ClusterSysInfo)
    ).toBe('DEBUG');
  });

  it('should not fail if the log informations are not there yet', () => {
    expect(
      u.getSystemLogsLevel({
        System: { 'High Availability': true },
        'Application Nodes': [{ Name: 'App 1' }, { Name: 'App 2' }]
      } as ClusterSysInfo)
    ).toBe('INFO');
    expect(
      u.getSystemLogsLevel({
        System: { 'High Availability': true },
        'Application Nodes': [{ 'Compute Engine Logging': {} }, { Name: 'App 2' }]
      } as any)
    ).toBe('INFO');
    expect(u.getSystemLogsLevel({ System: {} } as SysInfo)).toBe('INFO');
  });
});

describe('sortUpgrades', () => {
  it('should sort correctly versions', () => {
    expect(
      u.sortUpgrades([
        { version: '5.4.2' },
        { version: '5.10' },
        { version: '5.1' },
        { version: '5.4' }
      ] as SystemUpgrade[])
    ).toEqual([{ version: '5.10' }, { version: '5.4.2' }, { version: '5.4' }, { version: '5.1' }]);
    expect(
      u.sortUpgrades([
        { version: '5.10' },
        { version: '5.1.2' },
        { version: '6.0' },
        { version: '6.9' }
      ] as SystemUpgrade[])
    ).toEqual([{ version: '6.9' }, { version: '6.0' }, { version: '5.10' }, { version: '5.1.2' }]);
  });
});

describe('groupUpgrades', () => {
  it('should group correctly', () => {
    expect(
      u.groupUpgrades([
        { version: '5.10' },
        { version: '5.4.2' },
        { version: '5.4' },
        { version: '5.1' }
      ] as SystemUpgrade[])
    ).toEqual([
      [{ version: '5.10' }, { version: '5.4.2' }, { version: '5.4' }, { version: '5.1' }]
    ]);
    expect(
      u.groupUpgrades([
        { version: '6.9' },
        { version: '6.7' },
        { version: '6.0' },
        { version: '5.10' },
        { version: '5.4.2' }
      ] as SystemUpgrade[])
    ).toEqual([
      [{ version: '6.9' }, { version: '6.7' }, { version: '6.0' }],
      [{ version: '5.10' }, { version: '5.4.2' }]
    ]);
  });
});
