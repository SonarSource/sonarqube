/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import moment from 'moment';
import { Link } from 'react-router';
import ProjectCardQualityGate from './ProjectCardQualityGate';
import ProjectCardMeasures from './ProjectCardMeasures';
import FavoriteContainer from '../../../components/controls/FavoriteContainer';
import Organization from '../../../components/shared/Organization';
import TagsList from '../../../components/tags/TagsList';
import PrivateBadge from '../../../components/common/PrivateBadge';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default class ProjectCard extends React.PureComponent {
  props: {
    measures: { [string]: string },
    organization?: {},
    project?: {
      analysisDate?: string,
      key: string,
      name: string,
      tags: Array<string>,
      isFavorite?: boolean,
      organization?: string
    }
  };

  render() {
    const { project } = this.props;

    if (project == null) {
      return null;
    }

    const isProjectAnalyzed = project.analysisDate != null;
    // check reliability_rating because only some measures can be loaded
    // if coming from visualizations tab
    const areProjectMeasuresLoaded =
      !isProjectAnalyzed ||
      (this.props.measures != null &&
        this.props.measures['reliability_rating'] != null &&
        this.props.measures['sqale_rating'] != null);
    const displayQualityGate = areProjectMeasuresLoaded && isProjectAnalyzed;

    const className = classNames('boxed-group', 'project-card', {
      'boxed-group-loading': !areProjectMeasuresLoaded
    });

    return (
      <div data-key={project.key} className={className}>
        {displayQualityGate &&
          <div className="boxed-group-actions">
            <ProjectCardQualityGate status={this.props.measures['alert_status']} />
          </div>}

        <div className="boxed-group-header">
          {project.isFavorite != null &&
            <FavoriteContainer className="spacer-right" componentKey={project.key} />}
          <h2 className="project-card-name">
            {this.props.organization == null &&
              project.organization != null &&
              <span className="text-normal">
                <Organization organizationKey={project.organization} />
              </span>}
            <Link to={{ pathname: '/dashboard', query: { id: project.key } }}>
              {project.name}
            </Link>
          </h2>
          {project.visibility === 'private' && <PrivateBadge className="spacer-left" />}
          {project.tags.length > 0 && <TagsList tags={project.tags} customClass="spacer-left" />}
        </div>

        {isProjectAnalyzed
          ? <div className="boxed-group-inner">
              {areProjectMeasuresLoaded && <ProjectCardMeasures measures={this.props.measures} />}
            </div>
          : <div className="boxed-group-inner">
              <div className="note project-card-not-analyzed">
                {translate('projects.not_analyzed')}
              </div>
            </div>}

        {isProjectAnalyzed &&
          <div className="project-card-analysis-date note">
            {translateWithParameters(
              'overview.last_analysis_on_x',
              moment(project.analysisDate).format('LLL')
            )}
          </div>}
      </div>
    );
  }
}
