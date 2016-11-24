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
import ReadMore from './ReadMore';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';

const link = 'http://redirect.sonarsource.com/doc/rules.html';

const owaspTags = 'owasp-a1,owasp-a2,owasp-a3,owasp-a4,owasp-a5,owasp-a6,owasp-a7,owasp-a8,owasp-a9,owasp-a10';

export default class AboutStandards extends React.Component {
  render () {
    return (
        <div className="boxed-group">
          <h2>{translate('about_page.standards')}</h2>
          <div className="boxed-group-inner">
            <p className="about-page-text">{translate('about_page.standards.text')}</p>

            <div className="spacer-top">
              <ul className="list-inline">
                <li>
                  <a className="link-with-icon" href={getRulesUrl({ tags: 'misra' })}>
                    <i className="icon-tags"/>
                    <span className="little-spacer-left">MISRA</span>
                  </a>
                </li>
                <li>
                  <a className="link-with-icon" href={getRulesUrl({ tags: 'cert' })}>
                    <i className="icon-tags"/>
                    <span className="little-spacer-left">CERT</span>
                  </a>
                </li>
                <li>
                  <a className="link-with-icon" href={getRulesUrl({ tags: 'cwe' })}>
                    <i className="icon-tags"/>
                    <span className="little-spacer-left">CWE</span>
                  </a>
                </li>
                <li>
                  <a className="link-with-icon" href={getRulesUrl({ tags: owaspTags })}>
                    <i className="icon-tags"/>
                    <span className="little-spacer-left">OWASP Top 10</span>
                  </a>
                </li>
                <li>
                  <a className="link-with-icon"
                     href={getRulesUrl({ tags: 'sans-top25-porous,sans-top25-risky,sans-top25-insecure' })}>
                    <i className="icon-tags"/>
                    <span className="little-spacer-left">SANS Top 25</span>
                  </a>
                </li>
              </ul>
            </div>

            <ReadMore link={link}/>
          </div>
        </div>
    );
  }
}
