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
// @flow
import React from 'react';
import { Link } from 'react-router';
import { getRulesUrl } from '../../../helpers/urls';

/*::
type Props = {
  appState: {
    defaultOrganization: string,
    organizationsEnabled: boolean
  }
};
*/

export default function AboutRulesForSonarQubeDotCom(props /*: Props */) {
  const organization = props.appState.defaultOrganization;

  return (
    <div className="sqcom-about-rules">
      <div className="page-limited">
        <Link to={getRulesUrl(null, organization)} className="sqcom-about-rules-link">
          +3,000 rules
          <span className="spacer-left">
            <svg width="15" height="36" viewBox="0 0 15 36">
              <g transform="matrix(1,0,0,1,-267,-362)">
                <path
                  d="M268,363L281,380L269,397"
                  style={{ fill: 'none', stroke: '#c1d9ea', strokeWidth: 1 }}
                />
              </g>
            </svg>
          </span>
        </Link>
        <Link
          to={getRulesUrl({ languages: 'js' }, organization)}
          className="sqcom-about-rules-link">
          JavaScript
        </Link>
        <Link
          to={getRulesUrl({ languages: 'java' }, organization)}
          className="sqcom-about-rules-link">
          Java
        </Link>
        <Link
          to={getRulesUrl({ languages: 'c,cpp' }, organization)}
          className="sqcom-about-rules-link">
          C/C++
        </Link>
        <Link
          to={getRulesUrl({ languages: 'cs' }, organization)}
          className="sqcom-about-rules-link">
          C#
        </Link>
        <Link to={getRulesUrl(null, organization)} className="button">
          And More
        </Link>
      </div>
    </div>
  );
}
