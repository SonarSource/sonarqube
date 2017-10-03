/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import { Link } from 'react-router';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { getIssuesUrl } from '../../../helpers/urls';
import BugIconSonarCloud from './BugIconSonarCloud';
import VulnerabilityIconSonarCloud from './VulnerabilityIconSonarCloud';
import CodeSmellIconSonarCloud from './CodeSmellIconSonarCloud';

/*::
type Props = {
  bugs: ?number,
  codeSmells: ?number,
  loading: boolean,
  vulnerabilities: ?number
};
*/

export default function EntryIssueTypesSonarCloud(
  { bugs, codeSmells, loading, vulnerabilities } /*: Props */
) {
  return (
    <div className="about-page-projects">
      {loading && <i className="spinner" />}
      {!loading && (
        <table className="about-page-issue-types">
          <tbody>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  to={getIssuesUrl({ resolved: 'false', types: 'BUG', s: 'CREATION_DATE' })}
                  className="about-page-issue-type-link">
                  {formatMeasure(bugs, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right">
                  <BugIconSonarCloud />
                </span>
                {translate('issue.type.BUG.plural')}
              </td>
            </tr>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  to={getIssuesUrl({
                    resolved: 'false',
                    types: 'VULNERABILITY',
                    s: 'CREATION_DATE'
                  })}
                  className="about-page-issue-type-link">
                  {formatMeasure(vulnerabilities, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right">
                  <VulnerabilityIconSonarCloud />
                </span>
                {translate('issue.type.VULNERABILITY.plural')}
              </td>
            </tr>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  to={getIssuesUrl({ resolved: 'false', types: 'CODE_SMELL', s: 'CREATION_DATE' })}
                  className="about-page-issue-type-link">
                  {formatMeasure(codeSmells, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right">
                  <CodeSmellIconSonarCloud />
                </span>
                {translate('issue.type.CODE_SMELL.plural')}
              </td>
            </tr>
          </tbody>
        </table>
      )}
    </div>
  );
}
