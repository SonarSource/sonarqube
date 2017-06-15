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
import ProjectCardLeakMeasures from './ProjectCardLeakMeasures';
import FavoriteContainer from '../../../components/controls/FavoriteContainer';
import Organization from '../../../components/shared/Organization';
import TagsList from '../../../components/tags/TagsList';
import PrivateBadge from '../../../components/common/PrivateBadge';
import { translate, translateWithParameters } from '../../../helpers/l10n';

type Props = {
  measures: { [string]: string },
  organization?: { key: string },
  project?: {
    analysisDate?: string,
    key: string,
    leakPeriodDate?: string,
    name: string,
    tags: Array<string>,
    isFavorite?: boolean,
    organization?: string,
    visibility?: boolean
  }
};

export default function ProjectCardLeak({ measures, organization, project }: Props) {
  if (project == null) {
    return null;
  }

  const isProjectAnalyzed = project.analysisDate != null;
  const isPrivate = project.visibility === 'private';
  const hasLeakPeriodStart = project.leakPeriodDate != null;
  const hasTags = project.tags.length > 0;
  const showOrganization = organization == null && project.organization != null;

  // check for particular measures because only some measures can be loaded
  // if coming from visualizations tab
  const areProjectMeasuresLoaded = measures != null && measures['new_bugs'];

  const displayQualityGate = areProjectMeasuresLoaded && isProjectAnalyzed;
  const className = classNames('boxed-group', 'project-card', {
    'boxed-group-loading': isProjectAnalyzed && hasLeakPeriodStart && !areProjectMeasuresLoaded
  });

  return (
    <div data-key={project.key} className={className}>
      <div className="boxed-group-header clearfix">
        {project.isFavorite != null &&
          <FavoriteContainer className="spacer-right" componentKey={project.key} />}
        <h2 className="project-card-name">
          {showOrganization &&
            <span className="text-normal">
              <Organization organizationKey={project.organization} />
            </span>}
          <Link to={{ pathname: '/dashboard', query: { id: project.key } }}>{project.name}</Link>
        </h2>
        {displayQualityGate && <ProjectCardQualityGate status={measures['alert_status']} />}
        <div className="pull-right text-right">
          {isPrivate && <PrivateBadge className="spacer-left" tooltipPlacement="left" />}
          {hasTags && <TagsList tags={project.tags} customClass="spacer-left" />}
        </div>
        {isProjectAnalyzed &&
          hasLeakPeriodStart &&
          <div className="project-card-dates note text-right pull-right">
            {hasLeakPeriodStart &&
              <span className="project-card-leak-date pull-right">
                {translateWithParameters(
                  'projects.leak_period_x',
                  moment(project.leakPeriodDate).fromNow()
                )}
              </span>}
            {isProjectAnalyzed &&
              <span>
                {translateWithParameters(
                  'projects.last_analysis_on_x',
                  moment(project.analysisDate).format('LLL')
                )}
              </span>}
          </div>}
      </div>

      {isProjectAnalyzed && hasLeakPeriodStart
        ? <div className="boxed-group-inner">
            {areProjectMeasuresLoaded && <ProjectCardLeakMeasures measures={measures} />}
          </div>
        : <div className="boxed-group-inner">
            <div className="note project-card-not-analyzed">
              {isProjectAnalyzed
                ? translate('projects.no_leak_period')
                : translate('projects.not_analyzed')}
            </div>
          </div>}
    </div>
  );
}
