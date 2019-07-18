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
/* eslint-disable sonarjs/no-duplicate-string */
import { mockClusterSysInfo, mockStandaloneSysInfo } from '../../../helpers/testMocks';
import * as u from '../utils';

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
  it('should correctly return the worst log level for standalone mode', () => {
    expect(u.getSystemLogsLevel(mockStandaloneSysInfo())).toBe('DEBUG');
  });

  it('should return the worst log level for cluster mode', () => {
    expect(u.getSystemLogsLevel(mockClusterSysInfo())).toBe('DEBUG');
  });

  it('should not fail if the log informations are not there yet', () => {
    expect(
      u.getSystemLogsLevel(
        mockClusterSysInfo({
          'Application Nodes': [{ Name: 'App 1' }, { Name: 'App 2' }]
        })
      )
    ).toBe('INFO');
    expect(
      u.getSystemLogsLevel(
        mockClusterSysInfo({
          'Application Nodes': [{ 'Compute Engine Logging': {} }, { Name: 'App 2' }]
        })
      )
    ).toBe('INFO');
    expect(u.getSystemLogsLevel({} as T.SysInfoStandalone)).toBe('INFO');
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
      ] as T.SystemUpgrade[])
    ).toEqual([{ version: '5.10' }, { version: '5.4.2' }, { version: '5.4' }, { version: '5.1' }]);
    expect(
      u.sortUpgrades([
        { version: '5.10' },
        { version: '5.1.2' },
        { version: '6.0' },
        { version: '6.9' }
      ] as T.SystemUpgrade[])
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
      ] as T.SystemUpgrade[])
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
      ] as T.SystemUpgrade[])
    ).toEqual([
      [{ version: '6.9' }, { version: '6.7' }, { version: '6.0' }],
      [{ version: '5.10' }, { version: '5.4.2' }]
    ]);
  });
});

describe('isCluster', () => {
  it('should return the correct information', () => {
    expect(u.isCluster(mockClusterSysInfo())).toBe(true);
    expect(u.isCluster(mockStandaloneSysInfo())).toBe(false);
  });
});

describe('isLogInfoBlock', () => {
  it('should return the correct information', () => {
    expect(u.isLogInfoBlock(mockStandaloneSysInfo().System)).toBe(false);
    expect(u.isLogInfoBlock(mockStandaloneSysInfo()['Web Logging'])).toBe(true);
  });
});

describe('hasLoggingInfo', () => {
  it('should return the correct information', () => {
    expect(u.hasLoggingInfo(mockStandaloneSysInfo())).toBe(true);
    expect(u.hasLoggingInfo(mockClusterSysInfo()['Application Nodes'][0])).toBe(true);
    expect(u.hasLoggingInfo(mockClusterSysInfo())).toBe(false);
  });
});

describe('getStandaloneSecondarySections', () => {
  it('should return the correct information', () => {
    expect(Object.keys(u.getStandaloneSecondarySections(mockStandaloneSysInfo()))).toEqual(
      expect.arrayContaining(['Compute Engine', 'Search Engine', 'Web'])
    );
    expect(Object.keys(u.getStandaloneSecondarySections(mockClusterSysInfo()))).toEqual(
      expect.arrayContaining(['Compute Engine', 'Search Engine', 'Web'])
    );
  });
});

describe('getStandaloneMainSections', () => {
  it('should return the correct information', () => {
    expect(Object.keys(u.getStandaloneMainSections(mockStandaloneSysInfo()))).toEqual(
      expect.arrayContaining([
        'Server ID',
        'High Availability',
        'Health',
        'Health Causes',
        'Database'
      ])
    );
  });
});

describe('getClusterMainCardSection', () => {
  it('should return the correct information', () => {
    expect(Object.keys(u.getClusterMainCardSection(mockClusterSysInfo()))).toEqual(
      expect.arrayContaining([
        'Server ID',
        'High Availability',
        'Lines of Code',
        'Health',
        'Health Causes',
        'Database',
        'Compute Engine Tasks',
        'Search State',
        'Search Indexes'
      ])
    );
  });
});

describe('getSearchNodes', () => {
  it('should return the correct information', () => {
    expect(
      u.getSearchNodes(
        mockClusterSysInfo({
          'Search Nodes': [{ Name: 'searchnode1' }]
        })
      )
    ).toEqual([{ Name: 'searchnode1' }]);
  });
});

describe('getAppNodes', () => {
  it('should return the correct information', () => {
    expect(
      u.getAppNodes(
        mockClusterSysInfo({
          'Application Nodes': [{ Name: 'appnode1' }]
        })
      )
    ).toEqual([{ Name: 'appnode1' }]);
  });
});

describe('getNodeName', () => {
  it('should return the correct information', () => {
    expect(u.getNodeName({ Name: 'Foo' })).toEqual('Foo');
  });
});

describe('getHealthCauses', () => {
  it('should return the correct information', () => {
    expect(u.getHealthCauses({ 'Health Causes': ['Foo'] } as T.SysInfoBase)).toEqual(['Foo']);
  });
});

describe('getHealth', () => {
  it('should return the correct information', () => {
    expect(u.getHealth({ Health: 'GREEN' } as T.SysInfoBase)).toEqual('GREEN');
  });
});

describe('getLogsLevel', () => {
  it('should return the correct information, if available', () => {
    expect(u.getLogsLevel({ 'Compute Engine Logging': { 'Logs Level': 'TRACE' } })).toEqual(
      'TRACE'
    );
  });

  it('should return the worst level', () => {
    expect(
      u.getLogsLevel({
        'Web Logging': { 'Logs Level': 'DEBUG' },
        'Compute Engine Logging': { 'Logs Level': 'TRACE' }
      })
    ).toEqual('TRACE');
  });

  it('should return the default level if no information is provided', () => {
    expect(u.getLogsLevel()).toEqual('INFO');
  });
});

describe('getServerId', () => {
  it('should return the correct information, if available', () => {
    expect(u.getServerId(mockStandaloneSysInfo({ System: { 'Server ID': 'foo-bar' } }))).toEqual(
      'foo-bar'
    );
  });

  it('should return undefined if no information is available', () => {
    expect(u.getServerId(mockStandaloneSysInfo({ System: {} }))).toBeUndefined();
  });
});

describe('getVersion', () => {
  it('should return the correct information, if available', () => {
    expect(u.getVersion(mockStandaloneSysInfo({ System: { Version: '1.0' } }))).toEqual('1.0');
  });

  it('should return undefined if no information is available', () => {
    expect(u.getVersion(mockStandaloneSysInfo({ System: {} }))).toBeUndefined();
  });
});

describe('getClusterVersion', () => {
  it('should return the correct information, if available', () => {
    expect(
      u.getClusterVersion(
        mockClusterSysInfo({
          'Application Nodes': [{ System: { Version: '1.0' } }]
        })
      )
    ).toEqual('1.0');
  });

  it('should return undefined if no information is available', () => {
    expect(
      u.getClusterVersion(mockClusterSysInfo({ 'Application Nodes': [{ System: {} }] }))
    ).toBeUndefined();
    expect(
      u.getClusterVersion(
        mockClusterSysInfo({
          'Application Nodes': [],
          System: { Version: '1.0' }
        })
      )
    ).toBeUndefined();
  });
});
