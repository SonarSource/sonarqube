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
import React from 'react';
import { Link } from 'react-router';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { getIssuesUrl } from '../../../helpers/urls';
import BugIconForSonarQubeDotCom from './BugIconForSonarQubeDotCom';
import VulnerabilityIconForSonarQubeDotCom from './VulnerabilityIconForSonarQubeDotCom';
import CodeSmellIconForSonarQubeDotCom from './CodeSmellIconForSonarQubeDotCom';

export default class EntryIssueTypesForSonarQubeDotCom extends React.Component {
  static propTypes = {
    bugs: React.PropTypes.number.isRequired,
    vulnerabilities: React.PropTypes.number.isRequired,
    codeSmells: React.PropTypes.number.isRequired
  };

  render() {
    const { bugs, vulnerabilities, codeSmells } = this.props;

    return (
      <div className="about-page-projects">
        <table className="about-page-issue-types">
          <tbody>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  to={getIssuesUrl({ resolved: false, types: 'BUG' })}
                  className="about-page-issue-type-link">
                  {formatMeasure(bugs, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right"><BugIconForSonarQubeDotCom /></span>
                {translate('issue.type.BUG.plural')}
              </td>
            </tr>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  to={getIssuesUrl({ resolved: false, types: 'VULNERABILITY' })}
                  className="about-page-issue-type-link">
                  {formatMeasure(vulnerabilities, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right"><VulnerabilityIconForSonarQubeDotCom /></span>
                {translate('issue.type.VULNERABILITY.plural')}
              </td>
            </tr>
            <tr>
              <td className="about-page-issue-type-number">
                <Link
                  to={getIssuesUrl({ resolved: false, types: 'CODE_SMELL' })}
                  className="about-page-issue-type-link">
                  {formatMeasure(codeSmells, 'SHORT_INT')}
                </Link>
              </td>
              <td>
                <span className="little-spacer-right"><CodeSmellIconForSonarQubeDotCom /></span>
                {translate('issue.type.CODE_SMELL.plural')}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    );
  }
}
