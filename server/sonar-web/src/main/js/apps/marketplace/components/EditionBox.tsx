/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import Link from '../../../components/common/Link';
import { getEditionUrl } from '../../../helpers/editions';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { Edition, EditionKey } from '../../../types/editions';

interface Props {
  currentEdition?: EditionKey;
  edition: Edition;
  ncloc?: number;
  serverId?: string;
}

export default function EditionBox({ edition, ncloc, serverId, currentEdition }: Props) {
  return (
    <div className="boxed-group boxed-group-inner marketplace-edition">
      {edition.key === EditionKey.datacenter && (
        <div className="markdown">
          <div className="markdown-content">
            <div>
              <h3 id="data-center-edition">
                <img
                  alt="SonarQube logo"
                  className="max-width-100 little-spacer-right"
                  src={`${getBaseUrl()}/images/embed-doc/sq-icon.svg`}
                />
                Data Center Edition
              </h3>
              <p>
                <em>Designed for High Availability and Scalability</em>
              </p>
              <p>Enterprise Edition functionality plus:</p>
              <ul>
                <li>Component redundancy</li>
                <li>Data resiliency</li>
                <li>Horizontal scalability</li>
              </ul>
            </div>
          </div>
        </div>
      )}
      {edition.key === EditionKey.developer && (
        <div className="markdown">
          <div className="markdown-content">
            <div>
              <h3 id="developer-edition">
                <img
                  alt="SonarQube logo"
                  className="max-width-100 little-spacer-right"
                  src={`${getBaseUrl()}/images/embed-doc/sq-icon.svg`}
                />
                Developer Edition
              </h3>
              <p>
                <em>Built for Developers by Developers</em>
              </p>
              <p>Community Edition functionality plus:</p>
              <ul>
                <li>
                  PR / MR decoration &amp; Quality Gate
                  <img
                    alt="GitHub"
                    className="little-spacer-left max-width-100"
                    src={`${getBaseUrl()}/images/alm/github.svg`}
                  />
                  <img
                    alt="GitLab"
                    className="little-spacer-left max-width-100"
                    src={`${getBaseUrl()}/images/alm/gitlab.svg`}
                  />
                  <img
                    alt="Azure DevOps"
                    className="little-spacer-left max-width-100"
                    src={`${getBaseUrl()}/images/alm/azure.svg`}
                  />
                  <img
                    alt="Bitbucket"
                    className="little-spacer-left max-width-100"
                    src={`${getBaseUrl()}/images/alm/bitbucket.svg`}
                  />
                </li>
                <li>
                  Taint analysis / Injection flaw detection for Java, C#, PHP, Python, JS &amp; TS
                </li>
                <li>Branch analysis</li>
                <li>Project aggregation</li>
                <li>Additional languages: C, C++, Obj-C, PL/SQL, ABAP, TSQL &amp; Swift</li>
              </ul>
            </div>
          </div>
        </div>
      )}
      {edition.key === EditionKey.enterprise && (
        <div className="markdown">
          <div className="markdown-content">
            <div>
              <h3 id="enterprise-edition">
                <img
                  alt="SonarQube logo"
                  className="max-width-100 little-spacer-right"
                  src={`${getBaseUrl()}/images/embed-doc/sq-icon.svg`}
                />{' '}
                Enterprise Edition
              </h3>
              <p>
                <em>Designed to Meet Enterprise Requirements</em>
              </p>
              <p>Developer Edition functionality plus:</p>
              <ul>
                <li>Faster analysis with parallel processing</li>
                <li>OWASP/CWE security reports</li>
                <li>Portfolio management</li>
                <li>Executive reporting</li>
                <li>Project transfer</li>
                <li>Additional languages: Apex, COBOL, PL/I, RPG &amp; VB6</li>
              </ul>
            </div>
          </div>
        </div>
      )}
      <div className="marketplace-edition-action spacer-top">
        <Link
          to={getEditionUrl(edition, { ncloc, serverId, sourceEdition: currentEdition })}
          target="_blank"
        >
          {translate('marketplace.request_free_trial')}
        </Link>
      </div>
    </div>
  );
}
