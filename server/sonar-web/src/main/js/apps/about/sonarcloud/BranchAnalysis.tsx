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
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { isLoggedIn } from '../../../helpers/users';
import SQPageContainer from './components/SQPageContainer';
import SQStartUsing from './components/SQStartUsing';
import SQTopNav from './components/SQTopNav';
import './style.css';

export default function BranchAnalysis() {
  return (
    <SQPageContainer>
      {({ currentUser }) => (
        <div className="page page-limited sc-page">
          <Helmet title="Pull requests analysis in Github, BitBucket and Azure DevOps | SonarCloud">
            <meta
              content="SonarCloud automatically analyzes branches and decorates pull requests with Github, BitBucket and Azure DevOps."
              name="description"
            />
          </Helmet>
          <SQTopNav />

          <div className="sc-child-header">
            <img alt="" height="34" src={`${getBaseUrl()}/images/sonarcloud/branch-analysis.svg`} />
            <h1 className="sc-child-title">
              Branch analysis & <br />
              pull request decoration
            </h1>
            <p className="sc-child-lead">
              SonarCloud comes with a built-in feature to automatically analyze <br />
              project branches and pull requests as soon as they get created.
            </p>
          </div>

          <ul className="sc-features-list sc-branch-features-list">
            <li className="sc-feature sc-branch-feature">
              <img
                alt=""
                className="sc-branch-feature-right flex-0"
                height="270"
                src={`${getBaseUrl()}/images/sonarcloud/branch-01.png`}
                srcSet={`${getBaseUrl()}/images/sonarcloud/branch-01.png 1x, ${getBaseUrl()}/images/sonarcloud/branch-01@2x.png 2x`}
                width="463"
              />
              <div className="flex-1">
                <h3 className="sc-feature-title">Analyze branches and pull requests</h3>
                <p className="sc-feature-description">
                  For all project branches (main, maintenance, version, feature, etc.), you get the
                  full experience in the project space, with a specific focus on that branch.
                </p>
                <p className="sc-feature-description">
                  When analyzing pull requests (PRs), a Quality Gate will be generated along with
                  the list of issues created in the PR.
                </p>
                <p className="sc-feature-description">
                  To save time and insure consistency, the analysis configuration is reused across
                  all branches of a project.
                </p>
              </div>
            </li>
            <li className="sc-feature sc-branch-feature">
              <div className="flex-1">
                <h3 className="sc-feature-title">Decorate PRs on Azure DevOps and GitHub</h3>
                <p className="sc-feature-description">
                  Pull requests get decorated directly on Azure DevOps and GitHub. The result of the
                  PR analysis is available directly in the pull request itself, complementing nicely
                  manual reviews made by peers and enabling to make a more educated decision for
                  merging.
                </p>
              </div>
              <img
                alt=""
                className="sc-branch-feature-left flex-0"
                height="432"
                src={`${getBaseUrl()}/images/sonarcloud/branch-02.png`}
                srcSet={`${getBaseUrl()}/images/sonarcloud/branch-02.png 1x, ${getBaseUrl()}/images/sonarcloud/branch-02@2x.png 2x`}
                width="471"
              />
            </li>
            <li className="sc-feature sc-branch-feature">
              <img
                alt=""
                className="sc-branch-feature-right flex-0"
                height="169"
                src={`${getBaseUrl()}/images/sonarcloud/branch-03.png`}
                srcSet={`${getBaseUrl()}/images/sonarcloud/branch-03.png 1x, ${getBaseUrl()}/images/sonarcloud/branch-03@2x.png 2x`}
                width="460"
              />
              <div className="flex-1">
                <h3 className="sc-feature-title">Add a check in GitHub</h3>
                <p className="sc-feature-description">
                  Finally, a check can be added to the PR to provide the Quality Gate status of the
                  PR, check that can optionally block the merge.
                </p>
              </div>
            </li>
          </ul>

          <div className="sc-branch-bottom">
            There is no longer an excuse for pushing issues to the master.
          </div>

          {!isLoggedIn(currentUser) && <SQStartUsing />}
        </div>
      )}
    </SQPageContainer>
  );
}
