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

import styled from '@emotion/styled';
import { Link, LinkStandalone } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import {
  Badge,
  Card,
  LightLabel,
  LightPrimary,
  Note,
  QualityGateIndicator,
  SeparatorCircleIcon,
  SubnavigationFlowSeparator,
  Tags,
  themeBorder,
  themeColor,
} from 'design-system';
import { isEmpty } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import Favorite from '../../../../components/controls/Favorite';
import Tooltip from '../../../../components/controls/Tooltip';
import DateFromNow from '../../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import Measure from '../../../../components/measure/Measure';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { isDefined } from '../../../../helpers/types';
import { getProjectUrl } from '../../../../helpers/urls';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey, MetricType } from '../../../../types/metrics';
import { Status } from '../../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../../types/users';
import { Project } from '../../types';
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
  isNewCode: boolean,
) {
  const { analysisDate, isFavorite, key, measures, name, qualifier, tags, visibility } = project;
  const awaitingScan =
    [
      MetricKey.reliability_issues,
      MetricKey.maintainability_issues,
      MetricKey.security_issues,
    ].every((key) => measures[key] === undefined) &&
    !isNewCode &&
    !isEmpty(analysisDate) &&
    measures.ncloc !== undefined;
  const formatted = formatMeasure(measures[MetricKey.alert_status], MetricType.Level);
  const qualityGateLabel = translateWithParameters('overview.quality_gate_x', formatted);
  return (
    <>
      <div className="sw-flex sw-justify-between sw-items-center ">
        <div className="sw-flex sw-items-center ">
          {isDefined(isFavorite) && (
            <Favorite
              className="sw-mr-2"
              component={key}
              componentName={name}
              favorite={isFavorite}
              handleFavorite={handleFavorite}
              qualifier={qualifier}
            />
          )}

          <span className="it__project-card-name" title={name}>
            <LinkStandalone to={getProjectUrl(key)}>{name}</LinkStandalone>
          </span>

          {qualifier === ComponentQualifier.Application && (
            <Tooltip
              overlay={
                <span>
                  {translate('qualifier.APP')}
                  {measures.projects !== '' && (
                    <span>
                      {' ‒ '}
                      {translateWithParameters('x_projects_', measures.projects)}
                    </span>
                  )}
                </span>
              }
            >
              <span>
                <Badge className="sw-ml-2">{translate('qualifier.APP')}</Badge>
              </span>
            </Tooltip>
          )}

          <Tooltip overlay={translate('visibility', visibility, 'description', qualifier)}>
            <span>
              <Badge className="sw-ml-2">{translate('visibility', visibility)}</Badge>
            </span>
          </Tooltip>

          {awaitingScan && !isNewCode && !isEmpty(analysisDate) && measures.ncloc !== undefined && (
            <Tooltip overlay={translate(`projects.awaiting_scan.description.${qualifier}`)}>
              <span>
                <Badge variant="new" className="sw-ml-2">
                  {translate('projects.awaiting_scan')}
                </Badge>
              </span>
            </Tooltip>
          )}
        </div>

        {isDefined(analysisDate) && analysisDate !== '' && (
          <Tooltip overlay={qualityGateLabel}>
            <span className="sw-flex sw-items-center">
              <QualityGateIndicator
                status={(measures[MetricKey.alert_status] as Status) ?? 'NONE'}
                ariaLabel={qualityGateLabel}
              />
              <LightPrimary className="sw-ml-2 sw-body-sm-highlight">{formatted}</LightPrimary>
            </span>
          </Tooltip>
        )}
      </div>

      <LightLabel as="div" className="sw-flex sw-items-center sw-mt-3">
        {isDefined(analysisDate) && analysisDate !== '' && (
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
                  <span className="sw-body-sm-highlight sw-mr-1" data-key={MetricKey.new_lines}>
                    <Measure
                      metricKey={MetricKey.new_lines}
                      metricType={MetricType.ShortInteger}
                      value={measures.new_lines}
                    />
                  </span>

                  <span className="sw-body-sm">{translate('metric.new_lines.name')}</span>
                </div>
              </>
            )
          : measures[MetricKey.ncloc] != null && (
              <>
                <SeparatorCircleIcon className="sw-mx-1" />

                <div>
                  <span className="sw-body-sm-highlight sw-mr-1" data-key={MetricKey.ncloc}>
                    <Measure
                      metricKey={MetricKey.ncloc}
                      metricType={MetricType.ShortInteger}
                      value={measures.ncloc}
                    />
                  </span>

                  <span className="sw-body-sm">{translate('metric.ncloc.name')}</span>
                </div>

                <SeparatorCircleIcon className="sw-mx-1" />

                <span className="sw-body-sm" data-key={MetricKey.ncloc_language_distribution}>
                  <ProjectCardLanguages distribution={measures.ncloc_language_distribution} />
                </span>
              </>
            )}

        {tags.length > 0 && (
          <>
            <SeparatorCircleIcon className="sw-mx-1" />

            <Tags
              className="sw-body-sm"
              emptyText={translate('issue.no_tag')}
              ariaTagsListLabel={translate('issue.tags')}
              tooltip={Tooltip}
              tags={tags}
              tagsToDisplay={2}
            />
          </>
        )}
      </LightLabel>
    </>
  );
}

function renderSecondLine(
  currentUser: Props['currentUser'],
  project: Props['project'],
  isNewCode: boolean,
) {
  const { analysisDate, key, leakPeriodDate, measures, qualifier, isScannable } = project;

  if (!isEmpty(analysisDate) && (!isNewCode || !isEmpty(leakPeriodDate))) {
    return (
      <ProjectCardMeasures
        measures={measures}
        componentQualifier={qualifier}
        isNewCode={isNewCode}
      />
    );
  }

  return (
    <div className="sw-flex sw-items-center">
      <Note className="sw-py-4">
        {isNewCode && analysisDate
          ? translate('projects.no_new_code_period', qualifier)
          : translate('projects.not_analyzed', qualifier)}
      </Note>

      {qualifier !== ComponentQualifier.Application &&
        isEmpty(analysisDate) &&
        isLoggedIn(currentUser) &&
        isScannable && (
          <Link className="sw-ml-2 sw-body-sm-highlight" to={getProjectUrl(key)}>
            {translate('projects.configure_analysis')}
          </Link>
        )}
    </div>
  );
}

export default function ProjectCard(props: Readonly<Props>) {
  const { currentUser, type, project } = props;
  const isNewCode = type === 'leak';

  return (
    <ProjectCardWrapper
      className={classNames(
        'it_project_card sw-relative sw-box-border sw-rounded-1 sw-mb-page sw-h-full',
      )}
      data-key={project.key}
    >
      {renderFirstLine(project, props.handleFavorite, isNewCode)}

      <SubnavigationFlowSeparator className="sw-my-3" />

      {renderSecondLine(currentUser, project, isNewCode)}
    </ProjectCardWrapper>
  );
}

const ProjectCardWrapper = styled(Card)`
  background-color: ${themeColor('projectCardBackground')};
  border: ${themeBorder('default', 'projectCardBorder')};
  &.project-card-disabled *:not(g):not(path) {
    color: ${themeColor('projectCardDisabled')} !important;
  }
`;
