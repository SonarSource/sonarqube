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
import {
  renderCWECategory,
  renderOwaspTop10Category,
  renderSansTop25Category,
  renderSonarSourceSecurityCategory
} from '../security-standard';

describe('renderCWECategory', () => {
  const standards: T.Standards = {
    cwe: {
      '1004': {
        title: "Sensitive Cookie Without 'HttpOnly' Flag"
      },
      unknown: {
        title: 'No CWE associated'
      }
    },
    owaspTop10: {},
    sansTop25: {},
    sonarsourceSecurity: {}
  };
  it('should render categories correctly', () => {
    expect(renderCWECategory(standards, '1004')).toEqual(
      "CWE-1004 - Sensitive Cookie Without 'HttpOnly' Flag"
    );
    expect(renderCWECategory(standards, '124')).toEqual('CWE-124');
    expect(renderCWECategory(standards, 'unknown')).toEqual('No CWE associated');
  });
});

describe('renderOwaspTop10Category', () => {
  const standards: T.Standards = {
    cwe: {},
    owaspTop10: {
      a1: {
        title: 'Injection'
      }
    },
    sansTop25: {},
    sonarsourceSecurity: {}
  };
  it('should render categories correctly', () => {
    expect(renderOwaspTop10Category(standards, 'a1')).toEqual('A1 - Injection');
    expect(renderOwaspTop10Category(standards, 'a1', true)).toEqual('OWASP A1 - Injection');
    expect(renderOwaspTop10Category(standards, 'a2')).toEqual('A2');
    expect(renderOwaspTop10Category(standards, 'a2', true)).toEqual('OWASP A2');
  });
});

describe('renderSansTop25Category', () => {
  const standards: T.Standards = {
    cwe: {},
    owaspTop10: {},
    sansTop25: {
      'insecure-interaction': {
        title: 'Insecure Interaction Between Components'
      }
    },
    sonarsourceSecurity: {}
  };
  it('should render categories correctly', () => {
    expect(renderSansTop25Category(standards, 'insecure-interaction')).toEqual(
      'Insecure Interaction Between Components'
    );
    expect(renderSansTop25Category(standards, 'insecure-interaction', true)).toEqual(
      'SANS Insecure Interaction Between Components'
    );
    expect(renderSansTop25Category(standards, 'unknown')).toEqual('unknown');
    expect(renderSansTop25Category(standards, 'unknown', true)).toEqual('SANS unknown');
  });
});

describe('renderSonarSourceSecurityCategory', () => {
  const standards: T.Standards = {
    cwe: {},
    owaspTop10: {},
    sansTop25: {},
    sonarsourceSecurity: {
      xss: {
        title: 'Cross-Site Scripting (XSS)'
      },
      others: {
        title: 'Others'
      }
    }
  };
  it('should render categories correctly', () => {
    expect(renderSonarSourceSecurityCategory(standards, 'xss')).toEqual(
      'Cross-Site Scripting (XSS)'
    );
    expect(renderSonarSourceSecurityCategory(standards, 'xss', true)).toEqual(
      'SONAR Cross-Site Scripting (XSS)'
    );
    expect(renderSonarSourceSecurityCategory(standards, 'others')).toEqual('Others');
    expect(renderSonarSourceSecurityCategory(standards, 'others', true)).toEqual('Others');
  });
});
