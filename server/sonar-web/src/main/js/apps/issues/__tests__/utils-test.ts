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
import { scrollToElement } from 'sonar-ui-common/helpers/scrolling';
import {
  scrollToIssue,
  shouldOpenSeverityFacet,
  shouldOpenSonarSourceSecurityFacet,
  shouldOpenStandardsChildFacet,
  shouldOpenStandardsFacet
} from '../utils';

jest.mock('sonar-ui-common/helpers/scrolling', () => ({
  scrollToElement: jest.fn()
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe('scrollToIssue', () => {
  it('should scroll to the issue', () => {
    document.querySelector = jest.fn(() => ({}));

    scrollToIssue('issue1', false);
    expect(scrollToElement).toHaveBeenCalled();
  });
  it("should ignore issue if it doesn't exist", () => {
    document.querySelector = jest.fn(() => null);

    scrollToIssue('issue1', false);
    expect(scrollToElement).not.toHaveBeenCalled();
  });
  it('should scroll smoothly by default', () => {
    document.querySelector = jest.fn(() => ({}));

    scrollToIssue('issue1');
    expect(scrollToElement).toHaveBeenCalledWith(
      {},
      {
        bottomOffset: 100,
        smooth: true,
        topOffset: 250
      }
    );
  });
});

describe('shouldOpenSeverityFacet', () => {
  it('should open severity facet', () => {
    expect(shouldOpenSeverityFacet({ severities: true }, { types: [] })).toBe(true);
    expect(shouldOpenSeverityFacet({}, { types: [] })).toBe(true);
    expect(shouldOpenSeverityFacet({}, { types: ['VULNERABILITY'] })).toBe(true);
    expect(shouldOpenSeverityFacet({ severities: false }, { types: ['VULNERABILITY'] })).toBe(true);
    expect(shouldOpenSeverityFacet({ severities: false }, { types: [] })).toBe(true);
    expect(shouldOpenSeverityFacet({}, { types: ['BUGS', 'SECURITY_HOTSPOT'] })).toBe(true);
    expect(shouldOpenSeverityFacet({ severities: true }, { types: ['SECURITY_HOTSPOT'] })).toBe(
      true
    );
  });

  it('should NOT open severity facet', () => {
    expect(shouldOpenSeverityFacet({}, { types: ['SECURITY_HOTSPOT'] })).toBe(false);
  });
});

describe('shouldOpenStandardsFacet', () => {
  it('should open standard facet', () => {
    expect(shouldOpenStandardsFacet({ standards: true }, { types: [] })).toBe(true);
    expect(shouldOpenStandardsFacet({ owaspTop10: true }, { types: [] })).toBe(true);
    expect(shouldOpenStandardsFacet({}, { types: ['VULNERABILITY'] })).toBe(true);
    expect(shouldOpenStandardsFacet({}, { types: ['SECURITY_HOTSPOT'] })).toBe(true);
    expect(shouldOpenStandardsFacet({}, { types: ['VULNERABILITY', 'SECURITY_HOTSPOT'] })).toBe(
      true
    );
    expect(shouldOpenStandardsFacet({}, { types: ['BUGS', 'SECURITY_HOTSPOT'] })).toBe(true);
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
    expect(shouldOpenStandardsChildFacet({ owaspTop10: true }, {}, 'owaspTop10')).toBe(true);
    expect(shouldOpenStandardsChildFacet({ sansTop25: true }, {}, 'sansTop25')).toBe(true);
    expect(
      shouldOpenStandardsChildFacet({ sansTop25: true }, { owaspTop10: ['A1'] }, 'owaspTop10')
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet({ owaspTop10: false }, { owaspTop10: ['A1'] }, 'owaspTop10')
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet({}, { sansTop25: ['insecure-interactions'] }, 'sansTop25')
    ).toBe(true);
    expect(
      shouldOpenStandardsChildFacet(
        {},
        { sansTop25: ['insecure-interactions'], sonarsourceSecurity: ['sql-injection'] },
        'sonarsourceSecurity'
      )
    ).toBe(true);
  });

  it('should NOT open standard child facet', () => {
    expect(shouldOpenStandardsChildFacet({ standards: true }, {}, 'owaspTop10')).toBe(false);
    expect(shouldOpenStandardsChildFacet({ sansTop25: true }, {}, 'owaspTop10')).toBe(false);
    expect(shouldOpenStandardsChildFacet({}, { types: ['VULNERABILITY'] }, 'sansTop25')).toBe(
      false
    );
    expect(
      shouldOpenStandardsChildFacet({}, { types: ['SECURITY_HOTSPOT'] }, 'sonarsourceSecurity')
    ).toBe(false);
    expect(
      shouldOpenStandardsChildFacet(
        {},
        { sansTop25: ['insecure-interactions'], sonarsourceSecurity: ['sql-injection'] },
        'owaspTop10'
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
