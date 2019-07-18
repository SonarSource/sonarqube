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
import Helmet from 'react-helmet';
import { Link } from 'react-router';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { isLoggedIn } from '../../../helpers/users';
import SQPageContainer from './components/SQPageContainer';
import './style.css';

const LANGUAGES = [
  { name: 'JavaScript', file: 'js.svg', width: 60 },
  { name: 'TypeScript', file: 'ts.svg', width: 130 },
  { name: 'C#', file: 'csharp.svg', width: 60 },
  { name: 'C++', file: 'c-c-plus-plus.svg', width: 53 },
  { name: 'T-SQL', file: 't-sql.svg', width: 80 },
  { name: 'VB', file: 'vb.svg', width: 70 }
];

export default function AzureDevOps() {
  return (
    <SQPageContainer>
      {({ currentUser }) => (
        <div className="page page-limited sc-page">
          <Helmet title="Continuous Code Quality in Azure Devops | SonarCloud">
            <meta
              content="Enhance Azure DevOps with continuous code quality and automatically analyze branches and decorate pull requests."
              name="description"
            />
          </Helmet>
          <ul className="sc-top-nav">
            <li className="sc-top-nav-item">
              <Link className="sc-top-nav-link" to="/about/sq">
                Home
              </Link>
            </li>
          </ul>
          <div className="sc-child-header">
            <h1 className="sc-child-title">Get the full experience in Azure DevOps</h1>
          </div>

          <ul className="sc-features-list">
            <li className="sc-feature sc-child-feature">
              <h3 className="sc-feature-title">Native extension</h3>
              <p className="sc-feature-description">
                Using your existing Azure DevOps account and the SonarCloud Azure DevOps build
                tasks, adding and configuring SonarCloud analysis to an existing build is a matter
                of minutes.
              </p>
            </li>
            <li className="sc-feature sc-child-feature">
              <h3 className="sc-feature-title">Branches and PR analysis</h3>
              <p className="sc-feature-description">
                SonarCloud comes with a built-in feature to automatically analyze project branches
                and pull requests as soon as they get created.
              </p>
            </li>
            <li className="sc-feature sc-child-feature">
              <h3 className="sc-feature-title">Built-in Quality Gate</h3>
              <p className="sc-feature-description">
                A quality gate is available out of the box in order to verify code quality criteria
                at any time, enabling to fail build pipelines but also enabling to notify, through a
                webhook, any system that code quality criteria are not met.
              </p>
            </li>
            <li className="sc-feature sc-child-feature">
              <h3 className="sc-feature-title">Dedicated widget</h3>
              <p className="sc-feature-description">
                Once a project is in SonarCloud, a configurable widget can be added to the Azure
                DevOps dashboard in order to add code quality to KPIs already used on the project.
              </p>
            </li>
          </ul>

          <div className="sc-vsts-start-wrapper">
            <div className="sc-vsts-start">
              {!isLoggedIn(currentUser) && (
                <div className="sc-vsts-start-box">
                  <img
                    alt="SonarCloud"
                    height="38"
                    src={`${getBaseUrl()}/images/sonarcloud-square-logo.svg`}
                  />
                  <h3 className="sc-vsts-start-title">Log in or Sign up</h3>
                  <a className="sc-orange-button" href="/sessions/init/microsoft">
                    SonarCloud
                  </a>
                </div>
              )}
              <div className="sc-vsts-start-box">
                <img
                  alt="Azure DevOps Extension"
                  height="38"
                  src={`${getBaseUrl()}/images/sonarcloud/windows.svg`}
                />
                <h3 className="sc-vsts-start-title">Install Azure DevOps Extension</h3>
                <a
                  className="sc-black-button"
                  href="https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarcloud"
                  rel="noopener noreferrer"
                  target="_blank">
                  Marketplace
                </a>
              </div>
            </div>
          </div>

          <div className="sc-integrations">
            <h2 className="sc-sq-header2 sc-integrations-title">Analyze .NET languages and more</h2>
            <ul className="sc-languages-list">
              {LANGUAGES.map(language => (
                <li key={language.name}>
                  <img
                    alt={language.name}
                    src={`${getBaseUrl()}/images/languages/${language.file}`}
                    style={{ width: `${language.width}px` }}
                  />
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </SQPageContainer>
  );
}
