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
import { Standards } from '../../types/security';
import {
  renderCASACategory,
  renderCWECategory,
  renderOwaspAsvs40Category,
  renderOwaspTop10Category,
  renderPciDss32Category,
  renderPciDss40Category,
  renderSonarSourceSecurityCategory,
  renderStigCategory,
} from '../security-standard';

describe('standards renderers', () => {
  const standards: Standards = {
    cwe: {
      '1004': {
        title: "Sensitive Cookie Without 'HttpOnly' Flag",
      },
      unknown: {
        title: 'No CWE associated',
      },
    },
    owaspTop10: {
      a1: {
        title: 'Injection',
      },
    },
    'owaspTop10-2021': {
      a1: {
        title: 'Injection',
      },
    },
    sonarsourceSecurity: {
      xss: {
        title: 'Cross-Site Scripting (XSS)',
      },
      others: {
        title: 'Others',
      },
    },
    'pciDss-3.2': {
      '1': {
        title: 'Install and maintain a firewall configuration to protect cardholder data',
      },
    },
    'pciDss-4.0': {
      '1': {
        title: 'Install and maintain a firewall configuration to protect cardholder data',
      },
    },
    'owaspAsvs-4.0': {
      '1': {
        title: 'Main category',
      },
      '1.1': {
        title: 'Sub category',
        level: '2',
      },
    },
    casa: {
      '1': {
        title: 'Main category',
      },
    },
    'stig-ASD_V5R3': {
      'v-123': {
        title: 'Stig requirement',
      },
    },
  };

  it('should render cwe categories correctly', () => {
    expect(renderCWECategory(standards, '1004')).toEqual(
      "CWE-1004 - Sensitive Cookie Without 'HttpOnly' Flag",
    );
    expect(renderCWECategory(standards, '124')).toEqual('CWE-124');
    expect(renderCWECategory(standards, 'unknown')).toEqual('No CWE associated');
  });

  it('should render owasp categories correctly', () => {
    expect(renderOwaspTop10Category(standards, 'a1')).toEqual('A1 - Injection');
    expect(renderOwaspTop10Category(standards, 'a1', true)).toEqual('OWASP A1 - Injection');
    expect(renderOwaspTop10Category(standards, 'a2')).toEqual('A2');
    expect(renderOwaspTop10Category(standards, 'a2', true)).toEqual('OWASP A2');
  });

  it('should render OwaspAsvs 4.0 correctly', () => {
    expect(renderOwaspAsvs40Category(standards, '1')).toEqual('1 - Main category');
    expect(renderOwaspAsvs40Category(standards, '1.1')).toEqual('1.1 - Sub category (Level 2)');
  });

  it('should render Pci Dss 3.2 correctly', () => {
    expect(renderPciDss32Category(standards, '1')).toEqual(
      '1 - Install and maintain a firewall configuration to protect cardholder data',
    );
    expect(renderPciDss32Category(standards, '1.1')).toEqual('1.1');
  });

  it('should render Pci Dss 4.0 correctly', () => {
    expect(renderPciDss40Category(standards, '1')).toEqual(
      '1 - Install and maintain a firewall configuration to protect cardholder data',
    );
    expect(renderPciDss40Category(standards, '1.1')).toEqual('1.1');
  });

  it('should render sonarsource categories correctly', () => {
    expect(renderSonarSourceSecurityCategory(standards, 'xss')).toEqual(
      'Cross-Site Scripting (XSS)',
    );
    expect(renderSonarSourceSecurityCategory(standards, 'xss', true)).toEqual(
      'SONAR Cross-Site Scripting (XSS)',
    );
    expect(renderSonarSourceSecurityCategory(standards, 'others')).toEqual('Others');
    expect(renderSonarSourceSecurityCategory(standards, 'others', true)).toEqual('Others');
  });

  it('should render casa categories correctly', () => {
    expect(renderCASACategory(standards, '1')).toEqual('1 - Main category');
    expect(renderCASACategory(standards, '2')).toEqual('2');
  });

  it('should render stig requirements correctly', () => {
    expect(renderStigCategory(standards, 'v-123')).toEqual('v-123 - Stig requirement');
    expect(renderStigCategory(standards, 'none')).toEqual('none');
  });
});
