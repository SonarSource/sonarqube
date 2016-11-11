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

const link = 'http://docs.sonarqube.org/display/SONAR/Rules';

export default class AboutStandards extends React.Component {
  render () {
    return (
        <div className="about-page-section">
          <div className="about-page-container clearfix">
            <img className="pull-right" src="/images/recognized-standards.svg" width={500} height={175}
                 alt="Conform to recognized standards"/>
            <h2 className="about-page-header">Conform to recognized standards</h2>
            <p className="about-page-text">
              SonarAnalyzers offer rules that support industry standards: MISRA, CERT, CWE, OWASP Top 10 and SANS Top
              25. Configure your Quality Profile with standard-related rules to ensure adherence.
            </p>
            <div className="big-spacer-top">
              <a className="about-page-link-more" href={link} target="_blank">
                <span>Read more</span>
                <i className="icon-detach spacer-left"/>
              </a>
            </div>
          </div>
        </div>
    );
  }
}
