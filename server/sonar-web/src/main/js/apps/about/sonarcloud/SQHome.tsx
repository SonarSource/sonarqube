/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Helmet from 'react-helmet';
import LoginButtons from './components/LoginButtons';
import Pricing from './components/Pricing';
import SQPageContainer from './components/SQPageContainer';
import StartUsing from './components/StartUsing';
import { isLoggedIn } from '../../../helpers/users';
import { getBaseUrl } from '../../../helpers/urls';
import './style.css';

export default function SQHome() {
  return (
    <SQPageContainer>
      {({ currentUser }) => (
        <div className="page sc-page sc-sq-page">
          <Helmet title="Use SonarQube as a Service, sign up for SonarCloud | SonarCloud">
            <meta
              content="Enhance your workflow with continuous code quality, SonarCloud automatically analyzes and decorates pull requests on GitHub, Bitbucket and Azure DevOps on major languages."
              name="description"
            />
          </Helmet>
          <Jumbotron />

          <h2 className="sc-sq-header2">You use the service, we take care of the rest</h2>
          <Pricing />
          {!isLoggedIn(currentUser) && <StartUsing />}

          <Features />
          <Languages />
          <Integrations />
          <BottomNote />
        </div>
      )}
    </SQPageContainer>
  );
}

function Jumbotron() {
  return (
    <div className="sc-sq-jumbotron">
      <div className="sc-sq-jumbotron-left">
        <h1 className="sc-sq-jumbotron-title">
          Use SonarQube
          <br />
          <span className="sc-sq-jumbotron-title-orange">as a Service</span>
        </h1>
        <div className="sc-sq-jumbotron-login">
          {'—'}
          <br />
          Log in or sign up with
        </div>
        <LoginButtons />
      </div>
      <div className="sc-sq-jumbotron-right">
        <img
          alt="SonarCloud project dashboard"
          src={`${getBaseUrl()}/images/sonarcloud/sq-homepage.png`}
          srcSet={`${getBaseUrl()}/images/sonarcloud/sq-homepage.png 1x, ${getBaseUrl()}/images/sonarcloud/sq-homepage@2x.png 2x`}
        />
      </div>
    </div>
  );
}

function Features() {
  return (
    <>
      <h2 className="sc-sq-header2">The right solution for developers</h2>
      <ul className="sc-features-list">
        <li className="sc-feature">
          <img
            alt=""
            className="big-spacer-bottom"
            height="34"
            src={`${getBaseUrl()}/images/sonarcloud/as-a-service.svg`}
          />
          <h3 className="sc-feature-title">As a Service</h3>
          <p className="sc-feature-description">
            We provide a fully operated version of SonarQube which is hosted on Amazon AWS in Europe
            (Frankfurt, Germany).
          </p>
          <Link className="sc-arrow-link sc-feature-link" to="/about/sq/as-a-service">
            See more
          </Link>
        </li>
        <li className="sc-feature">
          <img
            alt=""
            className="big-spacer-bottom"
            height="34"
            src={`${getBaseUrl()}/images/sonarcloud/branch-analysis.svg`}
          />
          <h3 className="sc-feature-title">Branch &amp; pull request analysis</h3>
          <p className="sc-feature-description">
            SonarCloud comes with a built-in feature to automatically analyze project branches and
            pull requests as soon as they get created.
          </p>
          <Link
            className="sc-arrow-link sc-feature-link"
            to="/about/sq/branch-analysis-and-pr-decoration">
            See more
          </Link>
        </li>
        <li className="sc-feature">
          <img
            alt=""
            className="big-spacer-bottom"
            height="34"
            src={`${getBaseUrl()}/images/sonarcloud/sonarlint-integration.svg`}
          />
          <h3 className="sc-feature-title">SonarLint integration</h3>
          <p className="sc-feature-description">
            The full SonarCloud experience can be enhanced with SonarLint, that enables developers
            to receive real time information directly in their IDEs.
          </p>
          <Link className="sc-arrow-link sc-feature-link" to="/about/sq/sonarlint-integration">
            See more
          </Link>
        </li>
      </ul>
    </>
  );
}

function Languages() {
  return (
    <div className="sc-languages">
      <h3 className="sc-feature-title">On 21 programming languages</h3>
      <ul className="sc-languages-list">
        <li>
          <img
            alt="Java"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/java.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="JavaScript"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/js.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="TypeScript"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/ts.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="C#"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/c-sharp.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="C++"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/c-plus.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="Go"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/go.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="Python"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/python.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="PHP"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/php.svg`}
            width="60"
          />
        </li>
      </ul>
      <ul className="sc-languages-list">
        <li>
          <img
            alt="VB"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/vb.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="Kotlin"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/kotlin.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="Flex"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/flex.png`}
            width="85"
          />
        </li>
        <li>
          <img
            alt="CSS"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/css.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="HTML"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/html5.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="Ruby"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/ruby.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="Scala"
            height="57"
            src={`${getBaseUrl()}/images/languages/black/scala.svg`}
            width="57"
          />
        </li>
        <li>
          <img
            alt="Swift"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/swift.svg`}
            width="60"
          />
        </li>
      </ul>
      <ul className="sc-languages-list">
        <li>
          <img
            alt="Objective-C"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/obj-c.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="T-SQL"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/tsql.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="PL/SQL"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/plsql.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="ABAP"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/abap.svg`}
            width="60"
          />
        </li>
        <li>
          <img
            alt="XML"
            height="60"
            src={`${getBaseUrl()}/images/languages/black/xml.svg`}
            width="60"
          />
        </li>
      </ul>
    </div>
  );
}

function Integrations() {
  return (
    <div className="sc-integrations">
      <h2 className="sc-sq-header2 sc-integrations-title">Fully integrated experience with</h2>
      <ul className="sc-integrations-list">
        <li>
          <h3 className="sc-feature-title">GitHub</h3>
          <img
            alt="GitHub"
            className="big-spacer-top"
            height="60"
            src={`${getBaseUrl()}/images/sonarcloud/github-big.svg`}
          />
        </li>
        <li>
          <h3 className="sc-feature-title">Azure DevOps</h3>
          <img
            alt="Azure DevOps"
            className="big-spacer-top"
            height="60"
            src={`${getBaseUrl()}/images/sonarcloud/azure.svg`}
          />
          <div className="big-spacer-top">
            <Link className="sc-arrow-link sc-feature-link" to="/about/sq/vsts">
              See more
            </Link>
          </div>
        </li>
        <li>
          <h3 className="sc-feature-title">Bitbucket</h3>
          <img
            alt="Bitbucket"
            className="big-spacer-top"
            height="60"
            src={`${getBaseUrl()}/images/sonarcloud/bitbucket-big.svg`}
          />
        </li>
      </ul>
    </div>
  );
}

function BottomNote() {
  return (
    <div className="sc-bottom-note">
      Includes all features of SonarSource{' '}
      <a
        className="sc-bottom-note-link link-base-color"
        href="https://www.sonarsource.com/plans-and-pricing/developer/"
        rel="noopener noreferrer"
        target="_blank">
        Developer Edition
      </a>
    </div>
  );
}
