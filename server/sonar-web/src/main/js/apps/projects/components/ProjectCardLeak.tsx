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
import * as difference from 'date-fns/difference_in_milliseconds';
import * as React from 'react';
import { Link } from 'react-router';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import PrivacyBadgeContainer from '../../../components/common/PrivacyBadgeContainer';
import Favorite from '../../../components/controls/Favorite';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import TagsList from '../../../components/tags/TagsList';
import { getProjectUrl } from '../../../helpers/urls';
import { isLoggedIn } from '../../../helpers/users';
import { Project } from '../types';
import { formatDuration } from '../utils';
import ProjectCardLeakMeasures from './ProjectCardLeakMeasures';
import ProjectCardOrganizationContainer from './ProjectCardOrganizationContainer';
import ProjectCardQualityGate from './ProjectCardQualityGate';

interface Props {
  currentUser: T.CurrentUser;
  handleFavorite: (component: string, isFavorite: boolean) => void;
  height: number;
  organization: T.Organization | undefined;
  project: Project;
}

export default class ProjectCardLeak extends React.PureComponent<Props> {
  render() {
    const { currentUser, handleFavorite, height, organization, project } = this.props;
    const { measures } = project;
    const hasTags = project.tags.length > 0;
    const periodMs = project.leakPeriodDate ? difference(Date.now(), project.leakPeriodDate) : 0;

    return (
      <div className="boxed-group project-card" data-key={project.key} style={{ height }}>
        <div className="boxed-group-header clearfix">
          <div className="project-card-header">
            {project.isFavorite != null && (
              <Favorite
                className="spacer-right"
                component={project.key}
                favorite={project.isFavorite}
                handleFavorite={handleFavorite}
                qualifier="TRK"
              />
            )}
            <h2 className="project-card-name">
              {!organization && (
                <ProjectCardOrganizationContainer organization={project.organization} />
              )}
              <Link to={{ pathname: '/dashboard', query: { id: project.key } }}>
                {project.name}
              </Link>
            </h2>
            {project.analysisDate && <ProjectCardQualityGate status={measures['alert_status']} />}
            <div className="project-card-header-right">
              <PrivacyBadgeContainer
                className="spacer-left"
                organization={organization || project.organization}
                qualifier="TRK"
                tooltipProps={{ projectKey: project.key }}
                visibility={project.visibility}
              />

              {hasTags && <TagsList className="spacer-left note" tags={project.tags} />}
            </div>
          </div>
          {project.analysisDate && project.leakPeriodDate && (
            <div className="project-card-dates note text-right pull-right">
              <span className="project-card-leak-date pull-right">
                {translateWithParameters('projects.new_code_period_x', formatDuration(periodMs))}
              </span>
              <DateTimeFormatter date={project.analysisDate}>
                {formattedDate => (
                  <span>
                    {translateWithParameters('projects.last_analysis_on_x', formattedDate)}
                  </span>
                )}
              </DateTimeFormatter>
            </div>
          )}
        </div>

        {project.analysisDate && project.leakPeriodDate ? (
          <div className="boxed-group-inner">
            <ProjectCardLeakMeasures measures={measures} />
          </div>
        ) : (
          <div className="boxed-group-inner">
            <div className="project-card-not-analyzed">
              <span className="note">
                {project.analysisDate
                  ? translate('projects.no_new_code_period')
                  : translate('projects.not_analyzed')}
              </span>
              {!project.analysisDate && isLoggedIn(currentUser) && (
                <Link className="button spacer-left" to={getProjectUrl(project.key)}>
                  {translate('projects.configure_analysis')}
                </Link>
              )}
            </div>
          </div>
        )}
      </div>
    );
  }
}
