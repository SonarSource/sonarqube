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
import * as classNames from 'classnames';
import CountUp from 'react-countup';
import { throttle } from 'lodash';
import { Link } from 'react-router';
import { Project, requestFeaturedProjects } from '../utils';
import ChevronLeftIcon from '../../../../components/icons-components/ChevronLeftIcon';
import ChevronRightIcon from '../../../../components/icons-components/ChevronRightcon';
import CoverageRating from '../../../../components/ui/CoverageRating';
import DeferredSpinner from '../../../../components/common/DeferredSpinner';
import DuplicationsRating from '../../../../components/ui/DuplicationsRating';
import Level from '../../../../components/ui/Level';
import OrganizationAvatar from '../../../../components/common/OrganizationAvatar';
import ProjectCardLanguagesContainer from '../../../projects/components/ProjectCardLanguagesContainer';
import Rating from '../../../../components/ui/Rating';
import { formatMeasure } from '../../../../helpers/measures';
import { getMetricName } from '../../../overview/utils';
import { getProjectUrl } from '../../../../helpers/urls';
import './FeaturedProjects.css';

interface State {
  loading: boolean;
  reversing: boolean;
  slides: Array<{
    order: number;
    project: Project;
  }>;
  sliding: boolean;
  translate: number;
  viewable: boolean;
}

export default class FeaturedProjects extends React.PureComponent<{}, State> {
  container?: HTMLElement | null;
  mounted = false;

  constructor(props: {}) {
    super(props);
    this.state = {
      loading: true,
      reversing: false,
      slides: [],
      sliding: false,
      translate: 0,
      viewable: false
    };
    this.fetchProjects();
    this.handleScroll = throttle(this.handleScroll, 10);
  }

  componentDidMount() {
    document.addEventListener('scroll', this.handleScroll, true);
  }

  componentWillUnmount() {
    document.removeEventListener('scroll', this.handleScroll, true);
  }

  handleScroll = () => {
    if (this.container) {
      const rect = this.container.getBoundingClientRect();
      const windowHeight = window.innerHeight || document.documentElement.clientHeight;
      if (rect.top <= windowHeight && rect.top + rect.height >= 0) {
        this.setState({ viewable: true });
      }
    }
  };

  fetchProjects = () => {
    requestFeaturedProjects()
      .then(projects =>
        // Move the last element at the begining to properly display the carousel animations
        this.setState({
          loading: false,
          slides: [projects.pop(), ...projects].map((project: Project, id) => {
            return {
              order: id,
              project
            };
          })
        })
      )
      .catch(() => {
        /* Fail silently */
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
      this.setState({ sliding: false });
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
    const { loading, reversing, sliding, viewable } = this.state;
    return (
      <div
        className="sc-featured-projects sc-big-spacer-bottom"
        ref={node => (this.container = node)}>
        {!loading && (
          <button
            className="js-prev sc-project-button"
            onClick={this.handlePrevClick}
            type="button">
            <ChevronLeftIcon className="spacer-left" size={32} />
          </button>
        )}
        <div className="sc-featured-projects-container">
          <div
            className={classNames('sc-featured-projects-inner', {
              reversing,
              ready: !sliding,
              loading
            })}>
            {loading && <DeferredSpinner />}
            {!loading &&
              this.state.slides.map(slide => (
                <ProjectCard
                  key={slide.project.key}
                  order={slide.order}
                  project={slide.project}
                  viewable={viewable}
                />
              ))}
          </div>
        </div>
        {!loading && (
          <button
            className="js-next sc-project-button"
            onClick={this.handleNextClick}
            type="button">
            <ChevronRightIcon className="spacer-left" size={32} />
          </button>
        )}
      </div>
    );
  }
}

interface ProjectCardProps {
  order: number;
  project: Project;
  viewable: boolean;
}

export function ProjectCard({ project, order, viewable }: ProjectCardProps) {
  return (
    <div className="sc-project-card-container" style={{ order }}>
      <Link className="sc-project-card" to={getProjectUrl(project.key)}>
        <div className="sc-project-card-header">
          {project.organization && (
            <>
              <OrganizationAvatar
                className="no-border big-spacer-bottom"
                organization={project.organization}
              />
              <p className="sc-project-card-limited" title={project.organization.name}>
                {project.organization.name}
              </p>
            </>
          )}
          <h5 className="sc-project-card-limited spacer-bottom" title={project.name}>
            {project.name}
          </h5>
          <Level level={project.measures['alert_status']} />
        </div>
        <ul className="sc-project-card-measures">
          <ProjectIssues
            measures={project.measures}
            metric="bugs"
            ratingMetric="reliability_rating"
            viewable={viewable}
          />
          <ProjectIssues
            measures={project.measures}
            metric="vulnerabilities"
            ratingMetric="security_rating"
            viewable={viewable}
          />
          <ProjectIssues
            measures={project.measures}
            metric="code_smells"
            ratingMetric="sqale_rating"
            viewable={viewable}
          />
          <li>
            <span>{getMetricName('coverage')}</span>
            <div>
              {viewable && (
                <CountUp
                  decimal="."
                  decimals={1}
                  delay={0}
                  duration={4}
                  end={parseFloat(project.measures['coverage'])}
                  suffix="%">
                  {(data: { countUpRef?: React.RefObject<HTMLHeadingElement> }) => (
                    <h6 className="display-inline-block big-spacer-right" ref={data.countUpRef}>
                      0
                    </h6>
                  )}
                </CountUp>
              )}
              <CoverageRating value={project.measures['coverage']} />
            </div>
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
                  end={parseFloat(project.measures['duplicated_lines_density'])}
                  suffix="%">
                  {(data: { countUpRef?: React.RefObject<HTMLHeadingElement> }) => (
                    <h6 className="display-inline-block big-spacer-right" ref={data.countUpRef}>
                      0
                    </h6>
                  )}
                </CountUp>
              )}
              <DuplicationsRating value={Number(project.measures['duplicated_lines_density'])} />
            </div>
          </li>
        </ul>
        <div className="sc-mention text-left big-spacer-top">
          {formatMeasure(project.measures['ncloc'], 'SHORT_INT')} lines of code /{' '}
          <ProjectCardLanguagesContainer
            className="display-inline-block"
            distribution={project.measures['ncloc_language_distribution']}
          />
        </div>
      </Link>
    </div>
  );
}

interface ProjectIssues {
  measures: { [key: string]: string };
  metric: string;
  ratingMetric: string;
  viewable: boolean;
}

export function ProjectIssues({ measures, metric, ratingMetric, viewable }: ProjectIssues) {
  const value = parseFloat(formatMeasure(measures[metric], 'SHORT_INT'));
  return (
    <li>
      <span>{getMetricName(metric)}</span>
      <div>
        {viewable && (
          <CountUp delay={0} duration={4} end={value}>
            {(data: { countUpRef?: React.RefObject<HTMLHeadingElement> }) => (
              <h6 className="display-inline-block big-spacer-right" ref={data.countUpRef}>
                0
              </h6>
            )}
          </CountUp>
        )}
        <Rating value={measures[ratingMetric]} />
      </div>
    </li>
  );
}
