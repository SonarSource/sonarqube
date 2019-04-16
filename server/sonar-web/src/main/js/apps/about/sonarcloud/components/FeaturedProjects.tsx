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
import * as classNames from 'classnames';
import CountUp from 'react-countup';
import { throttle } from 'lodash';
import { FeaturedProject } from '../utils';
import CoverageRating from '../../../../components/ui/CoverageRating';
import DuplicationsRating from '../../../../components/ui/DuplicationsRating';
import OrganizationAvatar from '../../../../components/common/OrganizationAvatar';
import ProjectCardLanguagesContainer from '../../../projects/components/ProjectCardLanguagesContainer';
import Rating from '../../../../components/ui/Rating';
import { formatMeasure } from '../../../../helpers/measures';
import { getMetricName } from '../../../overview/utils';
import { getProjectUrl, getBaseUrl, getPathUrlAsString } from '../../../../helpers/urls';
import './FeaturedProjects.css';

interface Props {
  projects: FeaturedProject[];
}

interface State {
  reversing: boolean;
  slides: Array<{
    order: number;
    project: FeaturedProject;
  }>;
  sliding: boolean;
  viewable: boolean;
}

export default class FeaturedProjects extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      reversing: false,
      slides: this.orderProjectsFromProps(),
      sliding: false,
      viewable: false
    };
    this.handleScroll = throttle(this.handleScroll, 10);
  }

  componentDidMount() {
    this.mounted = true;
    document.addEventListener('scroll', this.handleScroll, true);
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.projects !== this.props.projects) {
      this.setState({ slides: this.orderProjectsFromProps() });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.removeEventListener('scroll', this.handleScroll, true);
  }

  handleScroll = () => {
    if (this.container) {
      const rect = this.container.getBoundingClientRect();
      const windowHeight =
        window.innerHeight ||
        (document.documentElement ? document.documentElement.clientHeight : 0);
      if (rect.top <= windowHeight && rect.top + rect.height >= 0) {
        this.setState({ viewable: true });
      }
    }
  };

  orderProjectsFromProps = () => {
    const { projects } = this.props;
    if (projects.length === 0) {
      return [];
    }

    // Last element should be put at the begining for proper carousel animation
    return [projects.pop(), ...projects].map((project: FeaturedProject, id) => {
      return {
        order: id,
        project
      };
    });
  };

  handlePrevClick = () => {
    this.setState(({ slides }) => ({
      reversing: true,
      sliding: true,
      slides: slides.map(slide => {
        slide.order = slide.order === slides.length - 1 ? 0 : slide.order + 1;
        return slide;
      })
    }));
    setTimeout(() => {
      if (this.mounted) {
        this.setState({ sliding: false });
      }
    }, 50);
  };

  handleNextClick = () => {
    this.setState(({ slides }) => ({
      reversing: false,
      sliding: true,
      slides: slides.map(slide => {
        slide.order = slide.order === 0 ? slides.length - 1 : slide.order - 1;
        return slide;
      })
    }));
    setTimeout(() => {
      this.setState({ sliding: false });
    }, 50);
  };

  render() {
    const { reversing, sliding, viewable } = this.state;
    return (
      <div
        className="sc-featured-projects sc-big-spacer-bottom"
        ref={node => (this.container = node)}>
        <button className="js-prev sc-project-button" onClick={this.handlePrevClick} type="button">
          <img alt="" src={`${getBaseUrl()}/images/sonarcloud/chevron-left.svg`} />
        </button>

        <div className="sc-featured-projects-container">
          <div
            className={classNames('sc-featured-projects-inner', {
              reversing,
              ready: !sliding
            })}>
            {this.state.slides.map(slide => (
              <ProjectCard
                key={slide.project.key}
                order={slide.order}
                project={slide.project}
                viewable={viewable}
              />
            ))}
          </div>
        </div>

        <button className="js-next sc-project-button" onClick={this.handleNextClick} type="button">
          <img alt="" src={`${getBaseUrl()}/images/sonarcloud/chevron-right.svg`} />
        </button>
      </div>
    );
  }
}

interface ProjectCardProps {
  order: number;
  project: FeaturedProject;
  viewable: boolean;
}

export function ProjectCard({ project, order, viewable }: ProjectCardProps) {
  return (
    <div className="sc-project-card-container" style={{ order }}>
      <a className="sc-project-card" href={getPathUrlAsString(getProjectUrl(project.key))}>
        <div className="sc-project-card-header">
          <OrganizationAvatar
            className="no-border spacer-bottom"
            organization={{
              name: project.organizationName,
              avatar: project.avatarUrl || undefined
            }}
          />
          <p className="sc-project-card-limited" title={project.organizationName}>
            {project.organizationName}
          </p>
          <h5 className="sc-project-card-limited big-spacer-bottom" title={project.name}>
            {project.name}
          </h5>
        </div>
        <ul className="sc-project-card-measures">
          <ProjectIssues
            metric={project.bugs}
            metricKey="bugs"
            ratingMetric={project.reliabilityRating}
            viewable={viewable}
          />
          <ProjectIssues
            metric={project.vulnerabilities}
            metricKey="vulnerabilities"
            ratingMetric={project.securityRating}
            viewable={viewable}
          />
          <ProjectIssues
            metric={project.codeSmells}
            metricKey="code_smells"
            ratingMetric={project.maintainabilityRating}
            viewable={viewable}
          />
          <li>
            <span>{getMetricName('coverage')}</span>
            {project.coverage !== undefined ? (
              <div>
                {viewable && (
                  <CountUp
                    decimal="."
                    decimals={1}
                    delay={0}
                    duration={4}
                    end={project.coverage}
                    suffix="%">
                    {(data: { countUpRef?: React.RefObject<HTMLHeadingElement> }) => (
                      <h6 className="display-inline-block big-spacer-right" ref={data.countUpRef}>
                        0
                      </h6>
                    )}
                  </CountUp>
                )}
                <CoverageRating value={project.coverage} />
              </div>
            ) : (
              <span className="huge little-spacer-right">—</span>
            )}
          </li>
          <li>
            <span>{getMetricName('duplications')}</span>
            <div>
              {viewable && (
                <CountUp
                  decimal="."
                  decimals={1}
                  delay={0}
                  duration={4}
                  end={project.duplications}
                  suffix="%">
                  {(data: { countUpRef?: React.RefObject<HTMLHeadingElement> }) => (
                    <h6 className="display-inline-block big-spacer-right" ref={data.countUpRef}>
                      0
                    </h6>
                  )}
                </CountUp>
              )}
              <DuplicationsRating value={project.duplications} />
            </div>
          </li>
        </ul>
        <div className="sc-mention text-left big-spacer-top">
          {formatMeasure(project.ncloc, 'SHORT_INT')} lines of code /{' '}
          <ProjectCardLanguagesContainer
            className="display-inline-block"
            distribution={project.languages.join(';')}
          />
        </div>
      </a>
    </div>
  );
}

interface ProjectIssues {
  metricKey: string;
  metric: number;
  ratingMetric: number;
  viewable: boolean;
}

export function ProjectIssues({ metric, metricKey, ratingMetric, viewable }: ProjectIssues) {
  const formattedValue = formatMeasure(metric, 'SHORT_INT');
  const value = parseFloat(formattedValue);
  const suffix = formattedValue.replace(value.toString(), '');
  return (
    <li>
      <span>{getMetricName(metricKey)}</span>
      <div>
        {viewable && (
          <CountUp delay={0} duration={4} end={value} suffix={suffix}>
            {(data: { countUpRef?: React.RefObject<HTMLHeadingElement> }) => (
              <h6 className="display-inline-block big-spacer-right" ref={data.countUpRef}>
                0
              </h6>
            )}
          </CountUp>
        )}
        <Rating value={ratingMetric} />
      </div>
    </li>
  );
}
