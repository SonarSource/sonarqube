/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import Link from '../../../../components/common/Link';
import PrivacyBadgeContainer from '../../../../components/common/PrivacyBadgeContainer';
import Favorite from '../../../../components/controls/Favorite';
import Tooltip from '../../../../components/controls/Tooltip';
import QualifierIcon from '../../../../components/icons/QualifierIcon';
import DateFromNow from '../../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import Measure from '../../../../components/measure/Measure';
import TagsList from '../../../../components/tags/TagsList';
import SizeRating from '../../../../components/ui/SizeRating';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { getProjectUrl } from '../../../../helpers/urls';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { CurrentUser, isLoggedIn } from '../../../../types/users';
import { Project } from '../../types';
import './ProjectCard.css';
import ProjectCardLanguages from './ProjectCardLanguages';
import ProjectCardMeasure from './ProjectCardMeasure';
import ProjectCardMeasures from './ProjectCardMeasures';
import ProjectCardQualityGate from './ProjectCardQualityGate';

interface Props {
  currentUser: CurrentUser;
  handleFavorite: (component: string, isFavorite: boolean) => void;
  height: number;
  project: Project;
  type?: string;
}

function renderFirstLine(project: Props['project'], handleFavorite: Props['handleFavorite']) {
  const {
    analysisDate,
    tags,
    qualifier,
    isFavorite,
    key,
    name,
    measures,
    needIssueSync,
    visibility,
  } = project;

  return (
    <div className="display-flex-center">
      <div className="project-card-main big-padded padded-bottom display-flex-center">
        {isFavorite !== undefined && (
          <Favorite
            className="spacer-right"
            component={key}
            componentName={name}
            favorite={isFavorite}
            handleFavorite={handleFavorite}
            qualifier={qualifier}
          />
        )}
        {qualifier === ComponentQualifier.Application && (
          <Tooltip
            placement="top"
            overlay={
              <span>
                {translate('qualifier.APP')}
                {measures.projects && (
                  <span>
                    {' ‒ '}
                    {translateWithParameters('x_projects_', measures.projects)}
                  </span>
                )}
              </span>
            }
          >
            <span className="spacer-right">
              <QualifierIcon qualifier={qualifier} />
            </span>
          </Tooltip>
        )}
        <h3 className="h2 project-card-name text-ellipsis" title={name}>
          {needIssueSync ? name : <Link to={getProjectUrl(key)}>{name}</Link>}
        </h3>

        {analysisDate && (
          <>
            <ProjectCardQualityGate status={measures[MetricKey.alert_status]} />
            <span className="flex-grow" />
            <DateTimeFormatter date={analysisDate}>
              {(formattedAnalysisDate) => (
                <span className="note big-spacer-left text-ellipsis" title={formattedAnalysisDate}>
                  <FormattedMessage
                    id="projects.last_analysis_on_x"
                    defaultMessage={translate('projects.last_analysis_on_x')}
                    values={{
                      date: <DateFromNow date={analysisDate} />,
                    }}
                  />
                </span>
              )}
            </DateTimeFormatter>
          </>
        )}
      </div>
      <div className="project-card-meta big-padded padded-bottom display-flex-center">
        <div className="display-flex-center overflow-hidden">
          <PrivacyBadgeContainer
            className="spacer-right"
            qualifier={qualifier}
            visibility={visibility}
          />
          {tags.length > 0 && <TagsList className="text-ellipsis" tags={tags} />}
        </div>
      </div>
    </div>
  );
}

function renderSecondLine(
  currentUser: Props['currentUser'],
  project: Props['project'],
  isNewCode: boolean
) {
  const { measures } = project;

  return (
    <div
      className={classNames('display-flex-end flex-grow', {
        'project-card-leak': isNewCode,
      })}
    >
      <div className="project-card-main big-padded-left big-padded-right big-padded-bottom">
        {renderMeasures(currentUser, project, isNewCode)}
      </div>
      <div className="project-card-meta display-flex-end big-padded-left big-padded-right big-padded-bottom">
        {isNewCode
          ? measures[MetricKey.new_lines] != null && (
              <ProjectCardMeasure
                metricKey={MetricKey.new_lines}
                label={translate('metric.lines.name')}
              >
                <Measure
                  className="big"
                  metricKey={MetricKey.new_lines}
                  metricType="SHORT_INT"
                  value={measures[MetricKey.new_lines]}
                />
              </ProjectCardMeasure>
            )
          : measures[MetricKey.ncloc] != null && (
              <ProjectCardMeasure
                metricKey={MetricKey.ncloc}
                label={translate('metric.lines.name')}
              >
                <div className="display-flex-center">
                  <Measure
                    className="big"
                    metricKey={MetricKey.ncloc}
                    metricType="SHORT_INT"
                    value={measures[MetricKey.ncloc]}
                  />
                  <span className="spacer-left">
                    <SizeRating value={Number(measures[MetricKey.ncloc])} />
                  </span>
                  <ProjectCardLanguages
                    className="small spacer-left text-ellipsis"
                    distribution={measures[MetricKey.ncloc_language_distribution]}
                  />
                </div>
              </ProjectCardMeasure>
            )}
      </div>
    </div>
  );
}

function renderMeasures(
  currentUser: Props['currentUser'],
  project: Props['project'],
  isNewCode: boolean
) {
  const { measures, needIssueSync, analysisDate, leakPeriodDate, qualifier, key } = project;

  if (analysisDate && (!isNewCode || leakPeriodDate)) {
    return (
      <ProjectCardMeasures
        measures={measures}
        componentQualifier={qualifier}
        isNewCode={isNewCode}
        newCodeStartingDate={leakPeriodDate}
      />
    );
  }

  return (
    <div className="spacer-top spacer-bottom">
      <span className="note">
        {isNewCode && analysisDate
          ? translate('projects.no_new_code_period', qualifier)
          : translate('projects.not_analyzed', qualifier)}
      </span>
      {qualifier !== ComponentQualifier.Application &&
        !analysisDate &&
        isLoggedIn(currentUser) &&
        !needIssueSync && (
          <Link className="button spacer-left" to={getProjectUrl(key)}>
            {translate('projects.configure_analysis')}
          </Link>
        )}
    </div>
  );
}

export default function ProjectCard(props: Props) {
  const { currentUser, height, type, project } = props;
  const isNewCode = type === 'leak';

  return (
    <div
      className={classNames('display-flex-column boxed-group it_project_card', {
        'project-card-disabled': project.needIssueSync,
      })}
      data-key={project.key}
      style={{ height }}
    >
      {renderFirstLine(project, props.handleFavorite)}
      {renderSecondLine(currentUser, project, isNewCode)}
    </div>
  );
}
