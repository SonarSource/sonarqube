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
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { SecurityStandard } from '../../../types/security';
import { getComponentIssuesUrl, getComponentSecurityHotspotsUrl, queryToSearch } from '../urls';

const SIMPLE_COMPONENT_KEY = 'sonarqube';

describe('queryToSearch', () => {
  it('should return query by default', () => {
    expect(queryToSearch()).toBe('?');
  });

  it('should return query with string values', () => {
    expect(queryToSearch({ key: 'value' })).toBe('?key=value');
  });

  it('should remove empty values', () => {
    expect(queryToSearch({ key: 'value', anotherKey: '' })).toBe('?key=value');
  });

  it('should return query with array values', () => {
    expect(queryToSearch({ key: ['value1', 'value2'] })).toBe('?key=value1&key=value2');
  });
});

describe('#getComponentIssuesUrl', () => {
  it('should work without parameters', () => {
    expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY)).toEqual(
      expect.objectContaining({
        pathname: '/project/issues',
        search: queryToSearch({ id: SIMPLE_COMPONENT_KEY }),
      }),
    );
  });

  it('should work with parameters', () => {
    expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, DEFAULT_ISSUES_QUERY)).toEqual(
      expect.objectContaining({
        pathname: '/project/issues',
        search: queryToSearch({ ...DEFAULT_ISSUES_QUERY, id: SIMPLE_COMPONENT_KEY }),
      }),
    );
  });
});

describe('#getComponentSecurityHotspotsUrl', () => {
  it('should work with no extra parameters', () => {
    expect(getComponentSecurityHotspotsUrl(SIMPLE_COMPONENT_KEY)).toEqual(
      expect.objectContaining({
        pathname: '/security_hotspots',
        search: queryToSearch({ id: SIMPLE_COMPONENT_KEY }),
      }),
    );
  });

  it('should forward some query parameters', () => {
    expect(
      getComponentSecurityHotspotsUrl(SIMPLE_COMPONENT_KEY, undefined, {
        inNewCodePeriod: 'true',
        [SecurityStandard.OWASP_TOP10_2021]: 'a1',
        [SecurityStandard.CWE]: '213',
        [SecurityStandard.OWASP_TOP10]: 'a1',
        [SecurityStandard.SONARSOURCE]: 'command-injection',
        [SecurityStandard.PCI_DSS_3_2]: '4.2',
        [SecurityStandard.PCI_DSS_4_0]: '4.1',
        ignoredParam: '1234',
      }),
    ).toEqual(
      expect.objectContaining({
        pathname: '/security_hotspots',
        search: queryToSearch({
          id: SIMPLE_COMPONENT_KEY,
          inNewCodePeriod: 'true',
          [SecurityStandard.OWASP_TOP10_2021]: 'a1',
          [SecurityStandard.OWASP_TOP10]: 'a1',
          [SecurityStandard.SONARSOURCE]: 'command-injection',
          [SecurityStandard.CWE]: '213',
          [SecurityStandard.PCI_DSS_3_2]: '4.2',
          [SecurityStandard.PCI_DSS_4_0]: '4.1',
        }),
      }),
    );
  });
});
