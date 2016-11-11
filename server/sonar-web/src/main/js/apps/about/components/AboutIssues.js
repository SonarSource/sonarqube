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
import { getIssuesUrl } from '../../../helpers/urls';

export default class AboutIssues extends React.Component {
  static propTypes = {
    bugs: React.PropTypes.number.isRequired,
    vulnerabilities: React.PropTypes.number.isRequired,
    codeSmells: React.PropTypes.number.isRequired
  };

  render () {
    return (
        <div className="about-page-section about-page-section-gray">
          <div className="about-page-container">
            <h2 className="about-page-header text-center">Track incoming issues using the SonarQube Quality Model</h2>
            <div className="about-page-issues">
              <div className="about-page-issues-box">
                <a className="about-page-issues-number" href={getIssuesUrl({ resolved: false, types: 'BUG' })}>
                  {formatMeasure(this.props.bugs, 'SHORT_INT')}
                </a>
                <div className="about-page-issues-description">
                  <h3 className="about-page-issues-header">Bugs</h3>
                  <p className="about-page-issues-text">
                    Bugs track code that is demonstrably wrong or highly likely to be yielding unexpected behavior.
                  </p>
                </div>
              </div>
              <div className="about-page-issues-box">
                <a className="about-page-issues-number"
                   href={getIssuesUrl({ resolved: false, types: 'VULNERABILITY' })}>
                  {formatMeasure(this.props.vulnerabilities, 'SHORT_INT')}
                </a>
                <div className="about-page-issues-description">
                  <h3 className="about-page-issues-header">Vulnerabilities</h3>
                  <p className="about-page-issues-text">
                    Vulnerabilities are raised on code that potentially vulnerable to exploitation by hackers.
                  </p>
                </div>
              </div>
              <div className="about-page-issues-box">
                <a className="about-page-issues-number" href={getIssuesUrl({ resolved: false, types: 'CODE_SMELL' })}>
                  {formatMeasure(this.props.codeSmells, 'SHORT_INT')}
                </a>
                <div className="about-page-issues-description">
                  <h3 className="about-page-issues-header">Code Smells</h3>
                  <p className="about-page-issues-text">
                    Code Smells will confuse maintainers or give them pause. They are measured primarily in terms of
                    the time they will take to fix.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
    );
  }
}
