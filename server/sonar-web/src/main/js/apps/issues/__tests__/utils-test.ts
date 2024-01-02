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
import { SecurityStandard } from '../../../types/security';
import {
  serializeQuery,
  shouldOpenSonarSourceSecurityFacet,
  shouldOpenStandardsChildFacet,
  shouldOpenStandardsFacet,
} from '../utils';

jest.mock('../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn(),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe('serialize/deserialize', () => {
  it('should serlialize correctly', () => {
    expect(
      serializeQuery({
        assigned: true,
        assignees: ['a', 'b'],
        author: ['a', 'b'],
        createdAfter: new Date(1000000),
        createdAt: 'a',
        createdBefore: new Date(1000000),
        createdInLast: 'a',
        cwe: ['18', '19'],
        directories: ['a', 'b'],
        files: ['a', 'b'],
        issues: ['a', 'b'],
        languages: ['a', 'b'],
        owaspTop10: ['a', 'b'],
        'owaspTop10-2021': ['a', 'b'],
        'pciDss-3.2': ['a', 'b'],
        'pciDss-4.0': ['a', 'b'],
        'owaspAsvs-4.0': ['2'],
        owaspAsvsLevel: '2',
        projects: ['a', 'b'],
        resolutions: ['a', 'b'],
        resolved: true,
        rules: ['a', 'b'],
        sort: 'rules',
        sansTop25: ['a', 'b'],
        scopes: ['a', 'b'],
        severities: ['a', 'b'],
        inNewCodePeriod: true,
        sonarsourceSecurity: ['a', 'b'],
        statuses: ['a', 'b'],
        tags: ['a', 'b'],
        types: ['a', 'b'],
      })
    ).toStrictEqual({
      assignees: 'a,b',
      author: ['a', 'b'],
      createdAt: 'a',
      createdBefore: '1970-01-01',
      createdAfter: '1970-01-01',
      createdInLast: 'a',
      cwe: '18,19',
      directories: 'a,b',
      files: 'a,b',
      issues: 'a,b',
      languages: 'a,b',
      owaspTop10: 'a,b',
      'owaspTop10-2021': 'a,b',
      'pciDss-3.2': 'a,b',
      'pciDss-4.0': 'a,b',
      'owaspAsvs-4.0': '2',
      owaspAsvsLevel: '2',
      projects: 'a,b',
      resolutions: 'a,b',
      rules: 'a,b',
      s: 'rules',
      sansTop25: 'a,b',
      scopes: 'a,b',
      severities: 'a,b',
      inNewCodePeriod: 'true',
      sonarsourceSecurity: 'a,b',
      statuses: 'a,b',
      tags: 'a,b',
      types: 'a,b',
    });
  });
});

describe('shouldOpenStandardsFacet', () => {
  it('should open standard facet', () => {
    expect(shouldOpenStandardsFacet({ standards: true }, { types: [] })).toBe(true);
    expect(shouldOpenStandardsFacet({ owaspTop10: true }, { types: [] })).toBe(true);
    expect(shouldOpenStandardsFacet({}, { types: ['VULNERABILITY'] })).toBe(true);
    expect(shouldOpenStandardsFacet({ standards: false }, { types: ['VULNERABILITY'] })).toBe(true);
  });

  it('should NOT open standard facet', () => {
    expect(shouldOpenStandardsFacet({ standards: false }, { types: ['BUGS'] })).toBe(false);
    expect(shouldOpenStandardsFacet({}, { types: [] })).toBe(false);
    expect(shouldOpenStandardsFacet({}, {})).toBe(false);
    expect(shouldOpenStandardsFacet({}, { types: ['BUGS'] })).toBe(false);
    expect(shouldOpenStandardsFacet({}, { types: ['BUGS'] })).toBe(false);
  });
});

describe('shouldOpenStandardsChildFacet', () => {
  it('should open standard child facet', () => {
    expect(
      shouldOpenStandardsChildFacet({ owaspTop10: true }, {}, SecurityStandard.OWASP_TOP10)
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet({ sansTop25: true }, {}, SecurityStandard.SANS_TOP25)
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet(
        { sansTop25: true },
        { owaspTop10: ['A1'] },
        SecurityStandard.OWASP_TOP10
      )
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet(
        { owaspTop10: false },
        { owaspTop10: ['A1'] },
        SecurityStandard.OWASP_TOP10
      )
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet(
        {},
        { sansTop25: ['insecure-interactions'] },
        SecurityStandard.SANS_TOP25
      )
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet(
        {},
        { sansTop25: ['insecure-interactions'], sonarsourceSecurity: ['sql-injection'] },
        SecurityStandard.SONARSOURCE
      )
    ).toBe(true);
  });

  it('should NOT open standard child facet', () => {
    expect(
      shouldOpenStandardsChildFacet({ standards: true }, {}, SecurityStandard.OWASP_TOP10)
    ).toBe(false);
    expect(
      shouldOpenStandardsChildFacet({ sansTop25: true }, {}, SecurityStandard.OWASP_TOP10)
    ).toBe(false);
    expect(
      shouldOpenStandardsChildFacet({}, { types: ['VULNERABILITY'] }, SecurityStandard.SANS_TOP25)
    ).toBe(false);
    expect(
      shouldOpenStandardsChildFacet(
        {},
        { sansTop25: ['insecure-interactions'], sonarsourceSecurity: ['sql-injection'] },
        SecurityStandard.OWASP_TOP10
      )
    ).toBe(false);
  });
});

describe('shouldOpenSonarSourceSecurityFacet', () => {
  it('should open sonarsourceSecurity facet', () => {
    expect(shouldOpenSonarSourceSecurityFacet({}, { sonarsourceSecurity: ['xss'] })).toBe(true);
    expect(shouldOpenSonarSourceSecurityFacet({ sonarsourceSecurity: true }, {})).toBe(true);
    expect(shouldOpenSonarSourceSecurityFacet({ standards: true }, {})).toBe(true);
    expect(shouldOpenSonarSourceSecurityFacet({}, { types: ['VULNERABILITY'] })).toBe(true);
    expect(
      shouldOpenSonarSourceSecurityFacet(
        { sonarsourceSecurity: false },
        { sonarsourceSecurity: ['xss'] }
      )
    ).toBe(true);
  });

  it('should NOT open sonarsourceSecurity facet', () => {
    expect(shouldOpenSonarSourceSecurityFacet({ standards: false }, {})).toBe(false);
    expect(shouldOpenSonarSourceSecurityFacet({ owaspTop10: true }, {})).toBe(false);
    expect(shouldOpenSonarSourceSecurityFacet({ standards: true, sansTop25: true }, {})).toBe(
      false
    );
  });
});
