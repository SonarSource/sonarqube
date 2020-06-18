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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import DateFromNow from 'sonar-ui-common/components/intl/DateFromNow';
import DateTimeFormatter from 'sonar-ui-common/components/intl/DateTimeFormatter';
import SizeRating from 'sonar-ui-common/components/ui/SizeRating';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import PrivacyBadgeContainer from '../../../../components/common/PrivacyBadgeContainer';
import Favorite from '../../../../components/controls/Favorite';
import Measure from '../../../../components/measure/Measure';
import TagsList from '../../../../components/tags/TagsList';
import { getProjectUrl } from '../../../../helpers/urls';
import { isLoggedIn } from '../../../../helpers/users';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { Project } from '../../types';
import './ProjectCard.css';
import ProjectCardLanguagesContainer from './ProjectCardLanguagesContainer';
import ProjectCardMeasure from './ProjectCardMeasure';
import ProjectCardMeasures from './ProjectCardMeasures';
import ProjectCardQualityGate from './ProjectCardQualityGate';

interface Props {
  currentUser: T.CurrentUser;
  handleFavorite: (component: string, isFavorite: boolean) => void;
  height: number;
  project: Project;
  type?: string;
}

function renderFirstLine(props: Props) {
  const {
    project: {
      analysisDate,
      tags,
      qualifier,
      isFavorite,
      key,
      name,
      measures,
      needIssueSync,
      visibility
    }
  } = props;

  return (
    <div className="display-flex-center">
      <div className="project-card-main big-padded padded-bottom display-flex-center">
        {isFavorite !== undefined && (
          <Favorite
            className="spacer-right"
            component={key}
            favorite={isFavorite}
            handleFavorite={props.handleFavorite}
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
            }>
            <span className="spacer-right">
              <QualifierIcon qualifier={qualifier} />
            </span>
          </Tooltip>
        )}
        <h2 className="project-card-name text-ellipsis" title={name}>
          {needIssueSync ? name : <Link to={getProjectUrl(key)}>{name}</Link>}
        </h2>

        {analysisDate && (
          <>
            <ProjectCardQualityGate status={measures[MetricKey.alert_status]} />
            <span className="flex-grow" />
            <DateTimeFormatter date={analysisDate}>
              {formattedAnalysisDate => (
                <span className="note big-spacer-left text-ellipsis" title={formattedAnalysisDate}>
                  <FormattedMessage
                    id="projects.last_analysis_on_x"
                    defaultMessage={translate('projects.last_analysis_on_x')}
                    values={{
                      date: <DateFromNow date={analysisDate} />
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

function renderSecondLine(props: Props, isNewCode: boolean) {
  const {
    project: { measures }
  } = props;

  return (
    <div
      className={classNames('display-flex-end flex-grow', {
        'project-card-leak': isNewCode
      })}>
      <div className="project-card-main big-padded-left big-padded-right big-padded-bottom">
        {renderMeasures(props, isNewCode)}
      </div>
      <div className="project-card-meta display-flex-end big-padded-left big-padded-right big-padded-bottom">
        {isNewCode
          ? measures[MetricKey.new_lines] != null && (
              <ProjectCardMeasure
                metricKey={MetricKey.new_lines}
                label={translate('metric.lines.name')}>
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
                label={translate('metric.lines.name')}>
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
                  <ProjectCardLanguagesContainer
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

function renderMeasures(props: Props, isNewCode: boolean) {
  const {
    currentUser,
    project: { measures, analysisDate, leakPeriodDate, qualifier, key }
  } = props;

  if (analysisDate && (!isNewCode || leakPeriodDate)) {
    return (
      <ProjectCardMeasures
        measures={measures}
        componentQualifier={qualifier}
        isNewCode={isNewCode}
        newCodeStartingDate={leakPeriodDate}
      />
    );
  } else {
    return (
      <div className="spacer-top spacer-bottom">
        <span className="note">
          {isNewCode && analysisDate
            ? translate('projects.no_new_code_period', qualifier)
            : translate('projects.not_analyzed', qualifier)}
        </span>
        {qualifier !== ComponentQualifier.Application && !analysisDate && isLoggedIn(currentUser) && (
          <Link className="button spacer-left" to={getProjectUrl(key)}>
            {translate('projects.configure_analysis')}
          </Link>
        )}
      </div>
    );
  }
}

export default function ProjectCard(props: Props) {
  const {
    height,
    type,
    project: { needIssueSync, key }
  } = props;
  const isNewCode = type === 'leak';

  return (
    <div
      className={classNames('display-flex-column boxed-group it_project_card', {
        'project-card-disabled': needIssueSync
      })}
      data-key={key}
      style={{ height }}>
      {renderFirstLine(props)}
      {renderSecondLine(props, isNewCode)}
    </div>
  );
}
