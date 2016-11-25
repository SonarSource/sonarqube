/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { getIssuesUrl } from '../../../helpers/urls';

export default class EntryIssueTypes extends React.Component {
  static propTypes = {
    bugs: React.PropTypes.number.isRequired,
    vulnerabilities: React.PropTypes.number.isRequired,
    codeSmells: React.PropTypes.number.isRequired
  };

  render () {
    const { bugs, vulnerabilities, codeSmells } = this.props;

    return (
        <div className="about-page-projects">
          <ul className="about-page-issue-types">
            <li>
              <div className="about-page-issue-type-number">
                <a className="about-page-issue-type-link"
                   href={getIssuesUrl({ resolved: false, types: 'BUG' })}>
                  {formatMeasure(bugs, 'SHORT_INT')}
                </a>
              </div>
              {translate('issue.type.BUG.plural')}
            </li>
            <li>
              <div className="about-page-issue-type-number">
                <a className="about-page-issue-type-link"
                   href={getIssuesUrl({ resolved: false, types: 'VULNERABILITY' })}>
                  {formatMeasure(vulnerabilities, 'SHORT_INT')}
                </a>
              </div>
              {translate('issue.type.VULNERABILITY.plural')}
            </li>
            <li>
              <div className="about-page-issue-type-number">
                <a className="about-page-issue-type-link"
                   href={getIssuesUrl({ resolved: false, types: 'CODE_SMELL' })}>
                  {formatMeasure(codeSmells, 'SHORT_INT')}
                </a>
              </div>
              {translate('issue.type.CODE_SMELL.plural')}
            </li>
          </ul>
        </div>
    );
  }
}
