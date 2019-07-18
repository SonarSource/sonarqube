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
import TagsIcon from 'sonar-ui-common/components/icons/TagsIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import ReadMore from './ReadMore';

const link = 'https://redirect.sonarsource.com/doc/rules.html';

const owaspTags =
  'owasp-a1,owasp-a2,owasp-a3,owasp-a4,owasp-a5,owasp-a6,owasp-a7,owasp-a8,owasp-a9,owasp-a10';
const sans25Tags = 'sans-top25-porous,sans-top25-risky,sans-top25-insecure';

interface Props {
  appState: Pick<T.AppState, 'defaultOrganization' | 'organizationsEnabled'>;
}

export default function AboutStandards({ appState }: Props) {
  const organization = appState.organizationsEnabled ? appState.defaultOrganization : undefined;

  return (
    <div className="boxed-group">
      <h2>{translate('about_page.standards')}</h2>
      <div className="boxed-group-inner">
        <p className="about-page-text">{translate('about_page.standards.text')}</p>

        <div className="spacer-top">
          <ul className="list-inline">
            <li>
              <Link className="link-with-icon" to={getRulesUrl({ tags: 'misra' }, organization)}>
                <TagsIcon />
                <span className="little-spacer-left">MISRA</span>
              </Link>
            </li>
            <li>
              <Link className="link-with-icon" to={getRulesUrl({ tags: 'cert' }, organization)}>
                <TagsIcon />
                <span className="little-spacer-left">CERT</span>
              </Link>
            </li>
            <li>
              <Link className="link-with-icon" to={getRulesUrl({ tags: 'cwe' }, organization)}>
                <TagsIcon />
                <span className="little-spacer-left">CWE</span>
              </Link>
            </li>
            <li>
              <Link className="link-with-icon" to={getRulesUrl({ tags: owaspTags }, organization)}>
                <TagsIcon />
                <span className="little-spacer-left">OWASP Top 10</span>
              </Link>
            </li>
            <li>
              <Link className="link-with-icon" to={getRulesUrl({ tags: sans25Tags }, organization)}>
                <TagsIcon />
                <span className="little-spacer-left">SANS Top 25</span>
              </Link>
            </li>
          </ul>
        </div>

        <ReadMore link={link} />
      </div>
    </div>
  );
}
