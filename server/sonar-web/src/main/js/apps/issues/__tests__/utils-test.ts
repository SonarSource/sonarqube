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
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../../types/issues';
import { SecurityStandard } from '../../../types/security';
import {
  parseQuery,
  serializeQuery,
  shouldOpenSonarSourceSecurityFacet,
  shouldOpenStandardsChildFacet,
  shouldOpenStandardsFacet,
} from '../utils';

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
        cleanCodeAttributeCategories: [CleanCodeAttributeCategory.Responsible],
        impactSeverities: [SoftwareImpactSeverity.High],
        impactSoftwareQualities: [SoftwareQuality.Security],
        codeVariants: ['variant1', 'variant2'],
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
      cleanCodeAttributeCategories: CleanCodeAttributeCategory.Responsible,
      impactSeverities: SoftwareImpactSeverity.High,
      impactSoftwareQualities: SoftwareQuality.Security,
      codeVariants: 'variant1,variant2',
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
      scopes: 'a,b',
      inNewCodePeriod: 'true',
      sonarsourceSecurity: 'a,b',
      statuses: 'a,b',
      tags: 'a,b',
      types: 'a,b',
    });
  });

  it('should deserialize correctly', () => {
    expect(
      parseQuery({
        assigned: 'true',
        assignees: 'first,second',
        author: ['author'],
        cleanCodeAttributeCategories: 'CONSISTENT',
        impactSeverities: 'LOW',
        severities: 'CRITICAL,MAJOR',
        impactSoftwareQualities: 'MAINTAINABILITY',
      })
    ).toStrictEqual({
      assigned: true,
      assignees: ['first', 'second'],
      author: ['author'],
      cleanCodeAttributeCategories: [CleanCodeAttributeCategory.Consistent],
      codeVariants: [],
      createdAfter: undefined,
      createdAt: '',
      createdBefore: undefined,
      createdInLast: '',
      cwe: [],
      directories: [],
      files: [],
      impactSeverities: [
        SoftwareImpactSeverity.Low,
        SoftwareImpactSeverity.High,
        SoftwareImpactSeverity.Medium,
      ],
      impactSoftwareQualities: [SoftwareQuality.Maintainability],
      inNewCodePeriod: false,
      issues: [],
      languages: [],
      'owaspAsvs-4.0': [],
      owaspAsvsLevel: '',
      owaspTop10: [],
      'owaspTop10-2021': [],
      'pciDss-3.2': [],
      'pciDss-4.0': [],
      projects: [],
      resolutions: [],
      resolved: true,
      rules: [],
      scopes: [],
      severities: [],
      sonarsourceSecurity: [],
      sort: '',
      statuses: [],
      tags: [],
      types: [],
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
      shouldOpenStandardsChildFacet(
        { cwe: true },
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
      shouldOpenStandardsChildFacet({}, { owaspTop10: ['A1'] }, SecurityStandard.OWASP_TOP10)
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet(
        {},
        { owaspTop10: ['A1'], sonarsourceSecurity: ['sql-injection'] },
        SecurityStandard.SONARSOURCE
      )
    ).toBe(true);
  });

  it('should NOT open standard child facet', () => {
    expect(
      shouldOpenStandardsChildFacet({ standards: true }, {}, SecurityStandard.OWASP_TOP10)
    ).toBe(false);
    expect(shouldOpenStandardsChildFacet({ cwe: true }, {}, SecurityStandard.OWASP_TOP10)).toBe(
      false
    );
    expect(
      shouldOpenStandardsChildFacet({}, { types: ['VULNERABILITY'] }, SecurityStandard.OWASP_TOP10)
    ).toBe(false);
    expect(
      shouldOpenStandardsChildFacet(
        {},
        { owaspTop10: ['A1'], sonarsourceSecurity: ['sql-injection'] },
        SecurityStandard.OWASP_TOP10_2021
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
    expect(shouldOpenSonarSourceSecurityFacet({ standards: true, cwe: true }, {})).toBe(false);
  });
});
