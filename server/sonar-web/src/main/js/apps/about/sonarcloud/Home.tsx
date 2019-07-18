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
import { connect } from 'react-redux';
import { addWhitePageClass, removeWhitePageClass } from 'sonar-ui-common/helpers/pages';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { getGlobalSettingValue, Store } from '../../../store/rootReducer';
import FeaturedProjects from './components/FeaturedProjects';
import Footer from './components/Footer';
import { Languages } from './components/Languages';
import LoginButtons from './components/LoginButtons';
import { FixedNavBar, TopNavBar } from './components/NavBars';
import Statistics from './components/Statistics';
import './new_style.css';
import { FeaturedProject, HomepageData, requestHomepageData } from './utils';

interface Props {
  homePageDataUrl?: string;
}

interface State {
  data?: HomepageData;
}

export class Home extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    addWhitePageClass();
    this.fetchData();
  }

  componentWillUnmount() {
    removeWhitePageClass();
    this.mounted = false;
  }

  fetchData = () => {
    const { homePageDataUrl } = this.props;
    if (homePageDataUrl) {
      requestHomepageData(homePageDataUrl).then(
        data => {
          if (this.mounted) {
            this.setState({ data });
          }
        },
        () => {
          /* Fail silently */
        }
      );
    }
  };

  render() {
    const { data } = this.state;

    return (
      <div className="global-container">
        <div className="page-wrapper">
          <div className="page-container sc-page">
            <Helmet title="SonarCloud | Clean Code, Rockstar Status">
              <meta
                content="Enhance your workflow with continuous code quality, SonarCloud automatically analyzes and decorates pull requests on GitHub, Bitbucket and Azure DevOps on major languages."
                name="description"
              />
            </Helmet>
            <FixedNavBar />
            <PageBackgroundHeader />
            <TopNavBar />
            <PageTitle />
            <EnhanceWorkflow />
            <Functionality />
            <Languages />
            <Stats data={data} />
            <Projects featuredProjects={(data && data.featuredProjects) || []} />
          </div>
        </div>
        <Footer />
      </div>
    );
  }
}

const mapStateToProps = (state: Store) => {
  const homePageDataUrl = getGlobalSettingValue(state, 'sonar.homepage.url');
  return {
    homePageDataUrl: homePageDataUrl && homePageDataUrl.value
  };
};

export default connect(mapStateToProps)(Home);

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
    <div className="sc-section sc-columns big-spacer-top">
      <div className="sc-column sc-column-half display-flex-center">
        <div>
          <h1 className="sc-title-orange big-spacer-top">Clean Code</h1>
          <h1 className="sc-spacer-bottom">Rockstar Status</h1>
          <h5 className="sc-big-spacer-bottom sc-regular-weight">
            Eliminate bugs and vulnerabilities.
            <br />
            Champion quality code in your projects.
          </h5>
          <div>
            <h6>Go ahead! Analyze your repo:</h6>
            <LoginButtons />
            <p className="sc-mention sc-regular-weight big-spacer-top">
              Free for Open-Source Projects
            </p>
          </div>
        </div>
      </div>
      <div className="sc-column sc-column-half text-right">
        <img
          alt=""
          src={`${getBaseUrl()}/images/sonarcloud/home-header-people.png`}
          width="430px"
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
        <h5 className="spacer-bottom">Maximize your throughput and only release clean code</h5>
        <h6 className="sc-big-spacer-bottom sc-regular-weight">
          SonarCloud automatically analyzes branches and decorates pull requests
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
            <div className="sc-column sc-column-small big-spacer-top">
              <h6 className="sc-regular-weight spacer-bottom">Easy to Use</h6>
              <p>
                With just a few clicks you’re up and running right where your code lives. Immediate
                access to the latest features and enhancements.
              </p>
              <div className="sc-separator" />
              <span className="big-spacer-bottom sc-with-icon">
                <img
                  alt=""
                  className="big-spacer-right"
                  src={`${getBaseUrl()}/images/sonarcloud/scale.svg`}
                />{' '}
                Scale on-demand as your projects grow.
              </span>
              <span className="sc-with-icon">
                <img
                  alt=""
                  className="big-spacer-right"
                  src={`${getBaseUrl()}/images/sonarcloud/stop.svg`}
                />{' '}
                No contracts, stop/start anytime.
              </span>
            </div>
            <div className="sc-column sc-column-big big-spacer-top">
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
                <h6 className="sc-regular-weight spacer-bottom">Open and Transparent</h6>
                <p className="big-spacer-bottom">
                  Project dashboards keep teams and stakeholders informed on code quality and
                  releasability.
                </p>
                <p>Display project badges and show your communities you’re all about awesome.</p>
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
            <div className="sc-column sc-column-full big-spacer-bottom">
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
                <p className="sc-with-inline-icon">
                  Connect with
                  <img
                    alt="SonarCloud"
                    src={`${getBaseUrl()}/images/sonarcloud/sonarlint-logo.svg`}
                  />
                  and get real-time notifications in your IDE as you work.
                </p>
                <div className="big-spacer-top">
                  <img
                    alt=""
                    className="big-spacer-top huge-spacer-bottom"
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
      <div className="sc-functionality-background sc-functionality-background-bottom">
        <div className="sc-background-center">
          <img
            alt=""
            height="140px"
            src={`${getBaseUrl()}/images/sonarcloud/home-background-grey-bottom.svg`}
          />
        </div>
      </div>
    </div>
  );
}

interface StatsProps {
  data?: HomepageData;
}

function Stats({ data }: StatsProps) {
  return (
    <div className="sc-section sc-columns">
      <div className="sc-column sc-column-full">
        <h3>
          Over 3,000 Projects
          <br />
          Continuously Analyzed
        </h3>
        {data && (
          <Statistics
            statistics={[
              { icon: 'rules', text: 'Static analysis rules checked', value: data.rules },
              { icon: 'locs', text: 'Lines of code analyzed', value: data.publicLoc },
              {
                icon: 'pull-request',
                text: 'Pull Requests decorated/week',
                value: data.newPullRequests7d
              },
              {
                icon: 'open-source',
                text: 'Open-source projects inspected',
                value: data.publicProjects
              }
            ]}
          />
        )}
      </div>
    </div>
  );
}

interface ProjectsProps {
  featuredProjects: FeaturedProject[];
}

function Projects({ featuredProjects }: ProjectsProps) {
  return (
    <div className="sc-section sc-columns">
      <div className="sc-column sc-column-full">
        {featuredProjects.length > 0 && (
          <>
            <h6 className="big-spacer-bottom">
              Transparency makes sense
              <br />
              and that’s why the trend is growing.
            </h6>
            <p>
              Check out these open-source projects showing users
              <br />
              their commitment to quality.
            </p>
            <FeaturedProjects projects={featuredProjects} />
          </>
        )}
        <h6 className="spacer-bottom">
          Come join the fun, it’s entirely free for open-source projects!
        </h6>
        <div className="sc-spacer-bottom">
          <LoginButtons />
        </div>
      </div>
    </div>
  );
}
