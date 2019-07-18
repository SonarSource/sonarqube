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

export default function AsAService() {
  return (
    <SQPageContainer>
      {({ currentUser }) => (
        <div className="page page-limited sc-page">
          <Helmet title="Get started with SonarQube as a Service | SonarCloud">
            <meta
              content="Analyze your code with just a few clicks. Immediate access to the latest features and functionality. You use the service and we take care of the rest."
              name="description"
            />
          </Helmet>
          <SQTopNav />

          <div className="sc-child-header">
            <img alt="" height="34" src={`${getBaseUrl()}/images/sonarcloud/as-a-service.svg`} />
            <h1 className="sc-child-title">As a Service</h1>
            <p className="sc-child-lead">
              We fully operate the SonarQube base service, <br />
              which is hosted in Frankfurt, Germany.
            </p>
            <img
              alt=""
              height="137"
              src={`${getBaseUrl()}/images/sonarcloud/gears.png`}
              srcSet={`${getBaseUrl()}/images/sonarcloud/gears.png 1x, ${getBaseUrl()}/images/sonarcloud/gears@2x.png 2x`}
              width="270"
            />
          </div>

          <ul className="sc-features-list">
            <li className="sc-feature sc-child-feature">
              <h3 className="sc-feature-title">Ready to use</h3>
              <p className="sc-feature-description">
                You need to worry about nothing but enjoying the service, everything else such as
                hardware, provisioning, installation, configuration, monitoring is being taken care
                of by us.
              </p>
            </li>
            <li className="sc-feature sc-child-feature">
              <h3 className="sc-feature-title">Always the latest and greatest</h3>
              <p className="sc-feature-description">
                SonarCloud always provides the latest and greatest features of SonarQube and our
                selection of plugins. As soon as a new feature is fit for production, it will ship
                to SonarCloud and wait that you use it.
              </p>
            </li>
            <li className="sc-feature sc-child-feature">
              <h3 className="sc-feature-title">Get started in minutes</h3>
              <p className="sc-feature-description">
                Simply sign up, create an organization for your team, and you are ready to run your
                builds to get your projects analyzed in minutes.
              </p>
            </li>
            <li className="sc-feature sc-child-feature">
              <h3 className="sc-feature-title">Designed to scale</h3>
              <p className="sc-feature-description">
                You do not need to care about sizing or planning your future needs, we take care of
                this and will make sure that the service can handle the analysis of your code at the
                pace you decide. When you are getting close to your subscription limit, you decide
                whether you want to go to the next level.
              </p>
            </li>
          </ul>
          {!isLoggedIn(currentUser) && <SQStartUsing />}
        </div>
      )}
    </SQPageContainer>
  );
}
