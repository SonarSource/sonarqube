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
import * as React from 'react';
import { Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';

const scanners = [
  {
    key: 'sonarqube',
    link: '/documentation/analysis/scan/sonarscanner/'
  },
  {
    key: 'msbuild',
    link: '/documentation/analysis/scan/sonarscanner-for-msbuild/'
  },
  {
    key: 'maven',
    link: '/documentation/analysis/scan/sonarscanner-for-maven/'
  },
  {
    key: 'gradle',
    link: '/documentation/analysis/scan/sonarscanner-for-gradle/'
  },
  {
    key: 'jenkins',
    link: '/documentation/analysis/scan/sonarscanner-for-jenkins/'
  },
  {
    key: 'ant',
    link: '/documentation/analysis/scan/sonarscanner-for-ant/'
  }
];

export default function AboutScanners() {
  return (
    <div className="boxed-group">
      <h2>{translate('about_page.scanners')}</h2>
      <div className="boxed-group-inner">
        <p className="about-page-text">{translate('about_page.scanners.text')}</p>
        <div className="about-page-analyzers">
          {scanners.map(scanner => (
            <Link
              className="about-page-analyzer-box"
              key={scanner.key}
              to={scanner.link}
              target="_blank">
              <img
                alt={translate('about_page.scanners', scanner.key)}
                height={60}
                src={`${getBaseUrl()}/images/scanner-logos/${scanner.key}.svg`}
              />
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
