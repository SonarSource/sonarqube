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
import * as React from 'react';
import { Link } from 'react-router';
import BugIcon from 'sonar-ui-common/components/icons/BugIcon';
import CodeSmellIcon from 'sonar-ui-common/components/icons/CodeSmellIcon';
import SecurityHotspotIcon from 'sonar-ui-common/components/icons/SecurityHotspotIcon';
import VulnerabilityIcon from 'sonar-ui-common/components/icons/VulnerabilityIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { getIssuesUrl } from '../../../helpers/urls';

interface Props {
  bugs?: number;
  codeSmells?: number;
  loading: boolean;
  securityHotspots?: number;
  vulnerabilities?: number;
}

export default function EntryIssueTypes({
  bugs,
  codeSmells,
  loading,
  securityHotspots,
  vulnerabilities
}: Props) {
  return (
    <div className="about-page-projects">
      {loading ? (
        <i className="spinner" />
      ) : (
        <table className="about-page-issue-types">
          <tbody>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  className="about-page-issue-type-link"
                  to={getIssuesUrl({ resolved: 'false', types: 'BUG', s: 'CREATION_DATE' })}>
                  {formatMeasure(bugs, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right">
                  <BugIcon />
                </span>
                {translate('issue.type.BUG.plural')}
              </td>
            </tr>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  className="about-page-issue-type-link"
                  to={getIssuesUrl({
                    resolved: 'false',
                    types: 'VULNERABILITY',
                    s: 'CREATION_DATE'
                  })}>
                  {formatMeasure(vulnerabilities, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right">
                  <VulnerabilityIcon />
                </span>
                {translate('issue.type.VULNERABILITY.plural')}
              </td>
            </tr>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  className="about-page-issue-type-link"
                  to={getIssuesUrl({ resolved: 'false', types: 'CODE_SMELL', s: 'CREATION_DATE' })}>
                  {formatMeasure(codeSmells, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right">
                  <CodeSmellIcon />
                </span>
                {translate('issue.type.CODE_SMELL.plural')}
              </td>
            </tr>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  className="about-page-issue-type-link"
                  to={getIssuesUrl({
                    resolved: 'false',
                    types: 'SECURITY_HOTSPOT',
                    s: 'CREATION_DATE'
                  })}>
                  {formatMeasure(securityHotspots, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right">
                  <SecurityHotspotIcon />
                </span>
                {translate('issue.type.SECURITY_HOTSPOT.plural')}
              </td>
            </tr>
          </tbody>
        </table>
      )}
    </div>
  );
}
