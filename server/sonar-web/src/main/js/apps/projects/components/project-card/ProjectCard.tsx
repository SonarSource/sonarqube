/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import styled from '@emotion/styled';
import classNames from 'classnames';
import {
  Badge,
  Card,
  Note,
  QualityGateIndicator,
  SeparatorCircleIcon,
  StandoutLink,
  Tags,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import Favorite from '../../../../components/controls/Favorite';
import Tooltip from '../../../../components/controls/Tooltip';
import DateFromNow from '../../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import Measure from '../../../../components/measure/Measure';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { formatMeasure } from '../../../../helpers/measures';
import { getProjectUrl } from '../../../../helpers/urls';
import { ComponentQualifier, Visibility } from '../../../../types/component';
import { MetricKey, MetricType } from '../../../../types/metrics';
import { Status } from '../../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../../types/users';
import { Project } from '../../types';
import './ProjectCard.css';
import ProjectCardLanguages from './ProjectCardLanguages';
import ProjectCardMeasures from './ProjectCardMeasures';

interface Props {
  currentUser: CurrentUser;
  handleFavorite: (component: string, isFavorite: boolean) => void;
  project: Project;
  type?: string;
}

function renderFirstLine(
  project: Props['project'],
  handleFavorite: Props['handleFavorite'],
  isNewCode: boolean
) {
  const {
    analysisDate,
    tags,
    qualifier,
    isFavorite,
    needIssueSync,
    key,
    name,
    measures,
    visibility,
  } = project;
  const formatted = formatMeasure(measures[MetricKey.alert_status], MetricType.Level);
  const qualityGateLabel = translateWithParameters('overview.quality_gate_x', formatted);
  return (
    <div>
      <div className="sw-flex sw-justify-between sw-items-center ">
        <div className="sw-flex sw-items-center ">
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

          <h3 className="it__project-card-name" title={name}>
            {needIssueSync ? name : <StandoutLink to={getProjectUrl(key)}>{name}</StandoutLink>}
          </h3>

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
              <span className="sw-ml-2">
                <Badge>{translate('qualifier.APP')}</Badge>
              </span>
            </Tooltip>
          )}

          {visibility === Visibility.Private && (
            <Tooltip overlay={translate('visibility', visibility, 'description', qualifier)}>
              <span className="sw-ml-2">
                <Badge>{translate('visibility', visibility)}</Badge>
              </span>
            </Tooltip>
          )}
        </div>
        {analysisDate && (
          <div>
            <Tooltip overlay={qualityGateLabel}>
              <span className="sw-flex sw-items-center">
                <QualityGateIndicator
                  status={(measures[MetricKey.alert_status] as Status) ?? 'NONE'}
                  className="sw-mr-2"
                  ariaLabel={qualityGateLabel}
                />
                <span className="sw-ml-2 sw-body-sm-highlight">{formatted}</span>
              </span>
            </Tooltip>
          </div>
        )}
      </div>
      <div className="sw-flex sw-items-center sw-mt-4">
        {analysisDate && (
          <DateTimeFormatter date={analysisDate}>
            {(formattedAnalysisDate) => (
              <span className="sw-body-sm-highlight" title={formattedAnalysisDate}>
                <FormattedMessage
                  id="projects.last_analysis_on_x"
                  defaultMessage={translate('projects.last_analysis_on_x')}
                  values={{
                    date: <DateFromNow className="sw-body-sm" date={analysisDate} />,
                  }}
                />
              </span>
            )}
          </DateTimeFormatter>
        )}
        {isNewCode
          ? measures[MetricKey.new_lines] != null && (
              <>
                <SeparatorCircleIcon className="sw-mx-1" />
                <div>
                  <span
                    className="js-project-card-measure sw-body-sm-highlight"
                    data-key={MetricKey.ncloc}
                  >
                    <Measure
                      metricKey={MetricKey.ncloc}
                      metricType={MetricType.ShortInteger}
                      value={measures.new_lines}
                    />{' '}
                  </span>
                  <span>{translate('metric.new_lines.name')}</span>
                </div>
              </>
            )
          : measures[MetricKey.ncloc] != null && (
              <>
                <SeparatorCircleIcon className="sw-mx-1" />
                <div>
                  <span
                    className="js-project-card-measure sw-body-sm-highlight"
                    data-key={MetricKey.ncloc}
                  >
                    <Measure
                      metricKey={MetricKey.ncloc}
                      metricType={MetricType.ShortInteger}
                      value={measures.ncloc}
                    />{' '}
                  </span>
                  <span>{translate('metric.ncloc.name')}</span>
                </div>
                <SeparatorCircleIcon className="sw-mx-1" />
                <span
                  className="js-project-card-measure sw-body-sm"
                  data-key={MetricKey.ncloc_language_distribution}
                >
                  <ProjectCardLanguages distribution={measures.ncloc_language_distribution} />
                </span>
              </>
            )}
        {tags.length > 0 && (
          <>
            <SeparatorCircleIcon className="sw-mx-1" />
            <Tags emptyText="random" ariaTagsListLabel="why not" tooltip={Tooltip} tags={tags} />
          </>
        )}
      </div>
    </div>
  );
}

function renderSecondLine(
  currentUser: Props['currentUser'],
  project: Props['project'],
  isNewCode: boolean
) {
  const { measures, analysisDate, needIssueSync, leakPeriodDate, qualifier, key } = project;

  if (analysisDate && (!isNewCode || leakPeriodDate)) {
    return (
      <ProjectCardMeasures
        measures={measures}
        componentQualifier={qualifier}
        isNewCode={isNewCode}
      />
    );
  }

  return (
    <div className="sw-flex">
      <Note>
        {isNewCode && analysisDate
          ? translate('projects.no_new_code_period', qualifier)
          : translate('projects.not_analyzed', qualifier)}
      </Note>
      {qualifier !== ComponentQualifier.Application &&
        !analysisDate &&
        isLoggedIn(currentUser) &&
        !needIssueSync && (
          <StandoutLink className="sw-ml-1" to={getProjectUrl(key)}>
            {translate('projects.configure_analysis')}
          </StandoutLink>
        )}
    </div>
  );
}

export default function ProjectCard(props: Props) {
  const { currentUser, type, project } = props;
  const isNewCode = type === 'leak';

  return (
    <ProjectCardWrapper
      className={classNames(
        'it_project_card sw-relative sw-box-border sw-rounded-1 sw-mb-page sw-h-full',
        {
          'project-card-disabled': project.needIssueSync,
        }
      )}
      aria-disabled={project.needIssueSync}
      data-key={project.key}
    >
      {renderFirstLine(project, props.handleFavorite, isNewCode)}
      <Separator className="sw-h-0 sw-mx-1 sw-my-3" />
      {renderSecondLine(currentUser, project, isNewCode)}
    </ProjectCardWrapper>
  );
}

const Separator = styled.hr`
  border-top: ${themeBorder('default', 'projectCardBorder')};
`;

const ProjectCardWrapper = styled(Card)`
  background-color: ${themeColor('projectCardBackground')};
  border: ${themeBorder('default', 'projectCardBorder')};
  &.project-card-disabled {
    color: ${themeColor('projectCardDisabled')} !important;
  }
`;
