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
import React from 'react';
import classNames from 'classnames';
import { Link } from 'react-router';
import ProjectCardQualityGate from './ProjectCardQualityGate';
import ProjectCardMeasures from './ProjectCardMeasures';
import FavoriteContainer from '../../../components/controls/FavoriteContainer';
import { translate } from '../../../helpers/l10n';
import Organization from '../../../components/shared/Organization';

export default class ProjectCard extends React.Component {
  static propTypes = {
    project: React.PropTypes.object,
    organization: React.PropTypes.object
  };

  render () {
    const { project } = this.props;

    if (project == null) {
      return null;
    }

    const areProjectMeasuresLoaded = this.props.measures != null;
    const isProjectAnalyzed = areProjectMeasuresLoaded &&
        (this.props.measures['ncloc'] != null || this.props.measures['sqale_rating'] != null);

    const className = classNames('boxed-group', 'project-card', { 'boxed-group-loading': !areProjectMeasuresLoaded });


    return (
        <div data-key={project.key} className={className}>
          {isProjectAnalyzed && (
              <div className="boxed-group-actions">
                <ProjectCardQualityGate status={this.props.measures['alert_status']}/>
              </div>
          )}
          <div className="boxed-group-header">
            {project.isFavorite != null && (
                <FavoriteContainer className="spacer-right" componentKey={project.key}/>
            )}
            <h2 className="project-card-name">
              {this.props.organization == null && project.organization != null && (
                <span className="text-normal">
                  <Organization organizationKey={project.organization}/>
                </span>
              )}
              <Link to={{ pathname: '/dashboard', query: { id: project.key } }}>
                {project.name}
              </Link>
            </h2>
          </div>
          {isProjectAnalyzed ? (
                  <div className="boxed-group-inner">
                    <ProjectCardMeasures measures={this.props.measures}/>
                  </div>
              ) : (
                  <div className="boxed-group-inner">
                    <div className="note project-card-not-analyzed">
                      {translate('projects.not_analyzed')}
                    </div>
                  </div>
              )}
        </div>
    );
  }
}
