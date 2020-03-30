/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as difference from 'date-fns/difference_in_milliseconds';
import * as React from 'react';
import { Link } from 'react-router';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import PrivacyBadgeContainer from '../../../components/common/PrivacyBadgeContainer';
import Favorite from '../../../components/controls/Favorite';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import TagsList from '../../../components/tags/TagsList';
import { getProjectUrl } from '../../../helpers/urls';
import { isLoggedIn } from '../../../helpers/users';
import { ComponentQualifier } from '../../../types/component';
import { Project } from '../types';
import { formatDuration } from '../utils';
import ProjectCardLeakMeasures from './ProjectCardLeakMeasures';
import ProjectCardOrganizationContainer from './ProjectCardOrganizationContainer';
import ProjectCardOverallMeasures from './ProjectCardOverallMeasures';
import ProjectCardQualityGate from './ProjectCardQualityGate';

interface Props {
  currentUser: T.CurrentUser;
  handleFavorite: (component: string, isFavorite: boolean) => void;
  height: number;
  organization: T.Organization | undefined;
  project: Project;
  type?: string;
}

interface Dates {
  analysisDate: string;
  leakPeriodDate?: string;
}

function getDates(project: Project, type: string | undefined) {
  const { analysisDate, leakPeriodDate } = project;
  if (!analysisDate || (type === 'leak' && !leakPeriodDate)) {
    return undefined;
  } else {
    return { analysisDate, leakPeriodDate };
  }
}

function renderHeader(props: Props) {
  const { organization, project } = props;
  const hasTags = project.tags.length > 0;
  return (
    <div className="project-card-header">
      {project.isFavorite !== undefined && (
        <Favorite
          className="spacer-right"
          component={project.key}
          favorite={project.isFavorite}
          handleFavorite={props.handleFavorite}
          qualifier={project.qualifier}
        />
      )}
      <h2 className="project-card-name">
        {!organization && <ProjectCardOrganizationContainer organization={project.organization} />}
        <Link to={getProjectUrl(project.key)}>{project.name}</Link>
      </h2>
      {project.analysisDate && <ProjectCardQualityGate status={project.measures['alert_status']} />}
      <div className="project-card-header-right">
        <PrivacyBadgeContainer
          className="spacer-left"
          organization={organization || project.organization}
          qualifier={project.qualifier}
          tooltipProps={{ projectKey: project.key }}
          visibility={project.visibility}
        />

        {hasTags && <TagsList className="spacer-left note" tags={project.tags} />}
      </div>
    </div>
  );
}

function renderDates(dates: Dates, type: string | undefined) {
  const { analysisDate, leakPeriodDate } = dates;
  const periodMs = leakPeriodDate ? difference(Date.now(), leakPeriodDate) : 0;

  return (
    <>
      <DateTimeFormatter date={analysisDate}>
        {formattedDate => (
          <span className="note">
            {translateWithParameters('projects.last_analysis_on_x', formattedDate)}
          </span>
        )}
      </DateTimeFormatter>
      {type === 'leak' && periodMs !== undefined && (
        <span className="project-card-leak-date big-spacer-left big-spacer-right">
          {translateWithParameters('projects.new_code_period_x', formatDuration(periodMs))}
        </span>
      )}
    </>
  );
}

function renderDateRow(project: Project, dates: Dates | undefined, type: string | undefined) {
  if (project.qualifier === ComponentQualifier.Application || dates) {
    return (
      <div
        className={classNames('display-flex-center project-card-dates spacer-top', {
          'big-spacer-left padded-left': project.isFavorite !== undefined
        })}>
        {dates && renderDates(dates, type)}

        {project.qualifier === ComponentQualifier.Application && (
          <div className="text-right flex-1-0-auto">
            <QualifierIcon className="spacer-right" qualifier={project.qualifier} />
            {translate('qualifier.APP')}
            {project.measures.projects && (
              <>
                {' ‒ '}
                {translateWithParameters('x_projects_', project.measures.projects)}
              </>
            )}
          </div>
        )}
      </div>
    );
  } else {
    return null;
  }
}

function renderMeasures(props: Props, dates: Dates | undefined) {
  const { currentUser, project, type } = props;

  const { measures } = project;

  if (dates) {
    return type === 'leak' ? (
      <ProjectCardLeakMeasures measures={measures} />
    ) : (
      <ProjectCardOverallMeasures componentQualifier={project.qualifier} measures={measures} />
    );
  } else {
    return (
      <div className="project-card-not-analyzed">
        <span className="note">
          {type === 'leak' && project.analysisDate
            ? translate('projects.no_new_code_period', project.qualifier)
            : translate('projects.not_analyzed', project.qualifier)}
        </span>
        {project.qualifier !== ComponentQualifier.Application &&
          !project.analysisDate &&
          isLoggedIn(currentUser) && (
            <Link className="button spacer-left" to={getProjectUrl(project.key)}>
              {translate('projects.configure_analysis')}
            </Link>
          )}
      </div>
    );
  }
}

export default function ProjectCard(props: Props) {
  const { height, project, type } = props;

  const dates = getDates(project, type);

  return (
    <div
      className="boxed-group project-card big-padded display-flex-column display-flex-space-between"
      data-key={project.key}
      style={{ height }}>
      <div>
        {renderHeader(props)}
        {renderDateRow(project, dates, type)}
      </div>
      {renderMeasures(props, dates)}
    </div>
  );
}
