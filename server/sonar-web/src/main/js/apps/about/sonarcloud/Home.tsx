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
import { FixedNavBar, TopNavBar } from './components/NavBars';
import FeaturedProjects from './components/FeaturedProjects';
import Footer from './components/Footer';
import Statistics from './components/Statistics';
import { Languages } from './components/Languages';
import LoginButtons from './components/LoginButtons';
import { getBaseUrl } from '../../../helpers/urls';
import './new_style.css';

// TODO Get this from an external source
const STATISTICS = [
  { icon: 'rules', text: 'Static analysis rules checked', value: 9675 },
  { icon: 'locs', text: 'Lines of code analyzed', value: 20000000 },
  { icon: 'pull-request', text: 'Pull Requests decorated', value: 100675 },
  { icon: 'open-source', text: 'Open source projects inspected', value: 99675 }
];

export default class Home extends React.PureComponent {
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
      <div className="global-container">
        <div className="page-wrapper">
          <div className="page-container sc-page">
            <FixedNavBar />
            <PageBackgroundHeader />
            <TopNavBar />
            <PageTitle />
            <EnhanceWorkflow />
            <Functionality />
            <Languages />
            <Stats />
            <Projects />
          </div>
        </div>
        <Footer />
      </div>
    );
  }
}

function PageBackgroundHeader() {
  return (
    <div className="sc-header-background">
      <div className="sc-background-start" />
      <div className="sc-background-end" />
      <div className="sc-background-center">
        <img alt="" height="418px" src={`${getBaseUrl()}/images/sonarcloud/home-header.svg`} />
      </div>
    </div>
  );
}

function PageTitle() {
  return (
    <div className="sc-section sc-columns">
      <div className="sc-column sc-column-half display-flex-center">
        <div>
          <h1 className="sc-title-orange">Clean Code</h1>
          <h1 className="sc-spacer-bottom">Rockstar Status</h1>
          <h5 className="sc-big-spacer-bottom sc-regular-weight">
            Eliminate bugs and vulnerabilities,
            <br />
            champion quality code in your projects.
          </h5>
          <div>
            <h6>Go ahead! Analyze your repo:</h6>
            <LoginButtons />
            <p className="sc-mention sc-regular-weight big-spacer-top">
              Free for Open Source Projects
            </p>
          </div>
        </div>
      </div>
      <div className="sc-column sc-column-half text-center">
        <img
          alt=""
          src={`${getBaseUrl()}/images/sonarcloud/home-header-people.png`}
          width="480px"
        />
      </div>
    </div>
  );
}

function EnhanceWorkflow() {
  return (
    <div className="sc-section sc-columns">
      <div className="sc-column sc-column-full">
        <h3 className="sc-big-spacer-bottom">
          Enhance Your Workflow
          <br />
          with Continuous Code Quality
        </h3>
        <img
          alt=""
          className="sc-big-spacer-bottom"
          src={`${getBaseUrl()}/images/sonarcloud/home-branch.png`}
          srcSet={`${getBaseUrl()}/images/sonarcloud/home-branch.png 1x, ${getBaseUrl()}/images/sonarcloud/home-branch@2x.png 2x`}
        />
        <h5 className="spacer-bottom">Maximize your throughput, only release clean code</h5>
        <h6 className="sc-regular-weight">
          Sonarcloud automatically analyzes branches and decorates pull requests
        </h6>
      </div>
    </div>
  );
}

function Functionality() {
  return (
    <div className="position-relative">
      <div className="sc-functionality-background">
        <div className="sc-background-center">
          <img
            alt=""
            height="300px"
            src={`${getBaseUrl()}/images/sonarcloud/home-grey-background.svg`}
          />
        </div>
      </div>
      <div className="sc-functionality-container">
        <div className="sc-section">
          <h3 className="sc-big-spacer-bottom text-center">
            Functionality
            <br />
            that Fits Your Projects
          </h3>
          <div className="sc-columns">
            <div className="sc-column sc-column-small">
              <h6 className="sc-regular-weight spacer-bottom">Easy to Use</h6>
              <p>
                With just a few clicks you’re up and running right where your code lives. Immediate
                access to the latest features and enhancements.
              </p>
              <div className="sc-separator" />
              <span className="big-spacer-bottom sc-with-icon">
                <img
                  alt=""
                  className="spacer-right"
                  src={`${getBaseUrl()}/images/sonarcloud/scale.svg`}
                />{' '}
                Scale on-demand as your projects grow.
              </span>
              <span className="sc-with-icon">
                <img
                  alt=""
                  className="spacer-right"
                  src={`${getBaseUrl()}/images/sonarcloud/stop.svg`}
                />{' '}
                No contracts, stop/start anytime.
              </span>
            </div>
            <div className="sc-column sc-column-big">
              <img
                alt=""
                className="sc-rounded-img"
                src={`${getBaseUrl()}/images/sonarcloud/home-easy-to-use.png`}
                srcSet={`${getBaseUrl()}/images/sonarcloud/home-easy-to-use.png 1x, ${getBaseUrl()}/images/sonarcloud/home-easy-to-use@2x.png 2x`}
              />
            </div>
          </div>
          <div className="sc-columns">
            <div className="sc-column sc-column-big">
              <img
                alt=""
                className="sc-rounded-img"
                src={`${getBaseUrl()}/images/sonarcloud/home-open-transparent.png`}
                srcSet={`${getBaseUrl()}/images/sonarcloud/home-open-transparent.png 1x, ${getBaseUrl()}/images/sonarcloud/home-open-transparent@2x.png 2x`}
              />
            </div>
            <div className="sc-column sc-column-small">
              <div>
                <h6 className="sc-regular-weight spacer-bottom">Open and transparent</h6>
                <p className="big-spacer-bottom">
                  Project dashboards keep teams and stakeholders informed on code quality and
                  releasability
                </p>
                <p>Display project badges and show your communities you’re all about awesome</p>
                <img
                  alt=""
                  className="big-spacer-top"
                  src={`${getBaseUrl()}/images/project_badges/sonarcloud-black.svg`}
                  width="200px"
                />
              </div>
            </div>
          </div>
          <div className="sc-columns">
            <div className="sc-column sc-column-full">
              <div>
                <h6 className="sc-regular-weight spacer-bottom">Effective Collaboration</h6>

                <p className="sc-with-inline-icon">
                  Use
                  <img
                    alt="SonarCloud"
                    src={`${getBaseUrl()}/images/sonarcloud/sonarcloud-logo-text-only.svg`}
                  />
                  with your team, share best practices and have fun writing quality code!
                </p>
                <br />
                <p className="sc-with-inline-icon huge-spacer-bottom">
                  Connect with
                  <img
                    alt="SonarCloud"
                    src={`${getBaseUrl()}/images/sonarcloud/sonarlint-logo.svg`}
                  />
                  and get real-time notifications in your IDE as you work.
                </p>
                <div>
                  <img
                    alt=""
                    className="huge-spacer-bottom"
                    src={`${getBaseUrl()}/images/sonarcloud/ide.svg`}
                    width="216px"
                  />
                </div>
                <img alt="" src={`${getBaseUrl()}/images/sonarcloud/collab.svg`} width="540px" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function Stats() {
  return (
    <div className="sc-section sc-columns">
      <div className="sc-column sc-column-full">
        <h5 className="sc-big-spacer-bottom">
          Over 3,000 projects
          <br />
          continuously analyzed
        </h5>
        <Statistics statistics={STATISTICS} />
      </div>
    </div>
  );
}

function Projects() {
  return (
    <div className="sc-section sc-columns">
      <div className="sc-column sc-column-full">
        <h6 className="big-spacer-bottom">
          Transparency makes sense
          <br />
          and that’s why the trend is growing.
        </h6>
        <p className="sc-big-spacer-bottom">
          Check out these open source projects showing users
          <br />
          their commitment to quality.
        </p>
        <FeaturedProjects />
        <h6 className="spacer-bottom">
          Come join the fun, it’s entirely free for open source projects !
        </h6>
        <div className="big-spacer-bottom">
          <LoginButtons />
        </div>
      </div>
    </div>
  );
}
