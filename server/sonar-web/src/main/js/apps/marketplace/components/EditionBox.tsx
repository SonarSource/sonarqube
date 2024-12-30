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

import { SubHeading, UnorderedList } from '~design-system';
import { Image } from '~sonar-aligned/components/common/Image';
import { Edition, EditionKey } from '../../../types/editions';

interface Props {
  edition: Edition;
}

export default function EditionBox({ edition }: Readonly<Props>) {
  switch (edition.key) {
    case EditionKey.datacenter:
      return (
        <div>
          <SubHeading as="h2" id="data-center-edition">
            <Image
              alt="SonarQube logo"
              className="sw-mr-2"
              width={16}
              src="/images/embed-doc/sq-icon.svg"
            />
            <span>Data Center Edition</span>
          </SubHeading>
          <p className="sw-mt-4">
            <em>Designed for High Availability and Scalability</em>
          </p>
          <p className="sw-mt-4">Enterprise Edition functionality plus:</p>
          <UnorderedList className="sw-ml-8" ticks>
            <li>Component redundancy</li>
            <li>Data resiliency</li>
            <li>Horizontal scalability</li>
          </UnorderedList>
        </div>
      );

    case EditionKey.enterprise:
      return (
        <div>
          <SubHeading as="h2" id="enterprise-edition">
            <Image
              alt="SonarQube logo"
              className="sw-mr-2"
              width={16}
              src="/images/embed-doc/sq-icon.svg"
            />
            <span>Enterprise Edition</span>
          </SubHeading>
          <p className="sw-mt-4">
            <em>Designed to Meet Enterprise Requirements</em>
          </p>
          <p className="sw-mt-4">Developer Edition functionality plus:</p>
          <UnorderedList className="sw-ml-8" ticks>
            <li>Faster analysis with parallel processing</li>
            <li>OWASP/CWE security reports</li>
            <li>Portfolio management</li>
            <li>Executive reporting</li>
            <li>Project transfer</li>
            <li>Additional languages: Apex, COBOL, PL/I, RPG &amp; VB6</li>
          </UnorderedList>
        </div>
      );

    case EditionKey.developer:
      return (
        <div>
          <SubHeading as="h2" id="developer-edition">
            <Image
              alt="SonarQube logo"
              className="sw-mr-2"
              width={16}
              src="/images/embed-doc/sq-icon.svg"
            />
            <span>Developer Edition</span>
          </SubHeading>
          <p className="sw-mt-4">
            <em>Built for Developers by Developers</em>
          </p>
          <p className="sw-mt-4">Community Build functionality plus:</p>
          <UnorderedList className="sw-ml-8" ticks>
            <li>
              <span>PR / MR decoration &amp; Quality Gate</span>
              <Image alt="GitHub" className="sw-ml-2" src="/images/alm/github.svg" width={16} />
              <Image alt="GitLab" className="sw-ml-2" src="/images/alm/gitlab.svg" width={16} />
              <Image
                alt="Azure DevOps"
                className="sw-ml-2"
                src="/images/alm/azure.svg"
                width={16}
              />
              <Image
                alt="Bitbucket"
                className="sw-ml-2"
                src="/images/alm/bitbucket.svg"
                width={16}
              />
            </li>
            <li>
              Taint analysis / Injection flaw detection for Java, C#, PHP, Python, JS &amp; TS
            </li>
            <li>Branch analysis</li>
            <li>Project aggregation</li>
            <li>Additional languages: C, C++, Obj-C, PL/SQL, ABAP, TSQL &amp; Swift</li>
          </UnorderedList>
        </div>
      );

    default:
      return null;
  }
}
