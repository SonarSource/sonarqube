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
import Footer from './Footer';
import Pricing from './Pricing';
import StartUsing from './StartUsing';
import GlobalContainer from '../../../app/components/GlobalContainer';
import { CurrentUser, isLoggedIn } from '../../../app/types';
import ChevronRightIcon from '../../../components/icons-components/ChevronRightcon';
import './style.css';

interface Props {
  currentUser: CurrentUser;
  location: { pathname: string };
}

export default class Home extends React.PureComponent<Props> {
  componentDidMount() {
    document.documentElement.classList.add('white-page');
    document.body.classList.add('white-page');
  }

  componentWillUnmount() {
    document.documentElement.classList.remove('white-page');
    document.body.classList.remove('white-page');
  }

  render() {
    return (
      <GlobalContainer footer={<Footer />} location={this.props.location}>
        <div className="page page-limited sc-page">
          <h1 className="sc-page-title">Continuous Code Quality Online</h1>
          <p className="sc-page-subtitle">
            Analyze the quality of your source code to detect bugs, vulnerabilities <br />and code
            smells throughout the development process.
          </p>

          <ul className="sc-features-list">
            <li className="sc-feature">
              <h2 className="sc-feature-title">Built on SonarQube</h2>
              <p className="sc-feature-description">
                The broadly used code review tool to detect bugs, code smells and vulnerability
                issues.
              </p>
            </li>

            <li className="sc-feature">
              <h2 className="sc-feature-title">17 languages</h2>
              <p className="sc-feature-description">
                Java, JS, C#, C/C++, Objective-C, TypeScript, Python, Go, ABAP, PL/SQL, T-SQL and
                more.
              </p>
            </li>

            <li className="sc-feature">
              <h2 className="sc-feature-title">Thousands of rules</h2>
              <p className="sc-feature-description">
                Track down hard-to-find bugs and quality issues thanks to powerful static code
                analyzers.
              </p>
            </li>

            <li className="sc-feature">
              <h2 className="sc-feature-title">Cloud CI Integrations</h2>
              <p className="sc-feature-description">
                Schedule the execution of an analysis from Cloud CI engines: Travis, VSTS, AppVeyor
                and more.
              </p>
            </li>

            <li className="sc-feature">
              <h2 className="sc-feature-title">Deep code analysis</h2>
              <p className="sc-feature-description">
                Explore all your source files, whether in branches or pull requests, to reach a
                green quality gate and promote the build.
              </p>
            </li>

            <li className="sc-feature">
              <h2 className="sc-feature-title">Fast and Scalable</h2>
              <p className="sc-feature-description">Scale on-demand as your projects grow.</p>
            </li>
          </ul>

          <Pricing />

          {!isLoggedIn(this.props.currentUser) && <StartUsing />}

          <div className="sc-narrow-container text-center">
            <h2 className="sc-feature-title">Explore open source projects on SonarCloud</h2>
            <p className="sc-feature-description">
              SonarCloud offers free analysis for open source projects. <br />It is public and open
              to anyone who wants to browse the service.
            </p>
          </div>

          <div className="sc-narrow-container text-center">
            <Link className="sc-browse" to="/explore/projects">
              Browse
            </Link>
          </div>

          <div className="sc-narrow-container sc-news">
            <h2 className="sc-news-title">News</h2>
            <ChevronRightIcon className="big-spacer-left" fill="#cfd3d7" />
            <a
              className="sc-news-link big-spacer-left"
              href="http://feedburner.google.com/fb/a/mailverify?uri=NewsSonarCloud&loc=en_US"
              rel="noopener noreferrer"
              target="_blank">
              Subscribe by email
            </a>
            <a
              className="sc-news-link big-spacer-left"
              href="http://feeds.feedburner.com/NewsSonarCloud"
              rel="noopener noreferrer"
              target="_blank">
              Subscribe by feed
            </a>
            <a
              className="sc-news-link big-spacer-left"
              href="https://blog.sonarsource.com/product/SonarCloud"
              rel="noopener noreferrer"
              target="_blank">
              See all
            </a>
          </div>
        </div>
      </GlobalContainer>
    );
  }
}
