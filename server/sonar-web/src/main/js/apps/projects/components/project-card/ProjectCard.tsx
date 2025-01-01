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
import { IconSparkle, Link, LinkStandalone, Tooltip } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { isEmpty } from 'lodash';
import { FormattedMessage, useIntl } from 'react-intl';
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
} from '~design-system';
import Measure from '~sonar-aligned/components/measure/Measure';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { Status } from '~sonar-aligned/types/common';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { AiCodeAssuranceStatus } from '../../../../api/ai-code-assurance';
import ChangeInCalculation from '../../../../app/components/ChangeInCalculationPill';
import { useCurrentUser } from '../../../../app/components/current-user/CurrentUserContext';
import Favorite from '../../../../components/controls/Favorite';
import AIAssuredIcon, { AiIconColor } from '../../../../components/icon-mappers/AIAssuredIcon';
import DateFromNow from '../../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { isDefined } from '../../../../helpers/types';
import { getProjectUrl } from '../../../../helpers/urls';
import { isLoggedIn } from '../../../../types/users';
import { Project } from '../../types';
import ProjectCardLanguages from './ProjectCardLanguages';
import ProjectCardMeasures from './ProjectCardMeasures';

interface Props {
  project: Project;
  type?: string;
}

function renderFirstLine(project: Props['project'], isNewCode: boolean) {
  const { analysisDate, isFavorite, key, measures, name, qualifier, tags, visibility } = project;
  const noSoftwareQualityMetrics = [
    MetricKey.software_quality_reliability_issues,
    MetricKey.software_quality_maintainability_issues,
    MetricKey.software_quality_security_issues,
  ].every((key) => measures[key] === undefined);
  const noRatingMetrics = [
    MetricKey.software_quality_reliability_rating,
    MetricKey.software_quality_maintainability_rating,
    MetricKey.software_quality_security_rating,
  ].every((key) => measures[key] === undefined);
  const awaitingScan =
    (noSoftwareQualityMetrics || noRatingMetrics) &&
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
              qualifier={qualifier}
            />
          )}

          <span className="it__project-card-name" title={name}>
            <LinkStandalone to={getProjectUrl(key)}>{name}</LinkStandalone>
          </span>

          {qualifier === ComponentQualifier.Application && (
            <Tooltip
              content={
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

          <Tooltip content={translate('visibility', visibility, 'description', qualifier)}>
            <span>
              <Badge className="sw-ml-2">{translate('visibility', visibility)}</Badge>
            </span>
          </Tooltip>

          {project.aiCodeAssurance === AiCodeAssuranceStatus.CONTAINS_AI_CODE && (
            <Tooltip content={translate('projects.ai_code.tooltip.content')}>
              <span>
                <Badge className="sw-ml-2">
                  <IconSparkle className="sw-mr-1 sw-fon" />
                  {translate('ai_code')}
                </Badge>
              </span>
            </Tooltip>
          )}

          {project.aiCodeAssurance === AiCodeAssuranceStatus.AI_CODE_ASSURED && (
            <Tooltip content={translate('projects.ai_code_assurance.tooltip.content')}>
              <span>
                <Badge variant="new" className="sw-ml-2">
                  <AIAssuredIcon
                    className="sw-mr-1 sw-align-bottom"
                    color={AiIconColor.Default}
                    width={16}
                    height={16}
                  />
                  {translate('ai_code_assurance')}
                </Badge>
              </span>
            </Tooltip>
          )}

          {awaitingScan && !isNewCode && !isEmpty(analysisDate) && measures.ncloc !== undefined && (
            <ChangeInCalculation qualifier={qualifier} />
          )}
        </div>

        {isDefined(analysisDate) && analysisDate !== '' && (
          <Tooltip content={qualityGateLabel}>
            <span className="sw-flex sw-items-center">
              <QualityGateIndicator
                status={(measures[MetricKey.alert_status] as Status) ?? 'NONE'}
              />
              <LightPrimary className="sw-ml-2 sw-typo-semibold">{formatted}</LightPrimary>
            </span>
          </Tooltip>
        )}
      </div>

      <LightLabel as="div" className="sw-flex sw-items-center sw-mt-3">
        {isDefined(analysisDate) && analysisDate !== '' && (
          <DateTimeFormatter date={analysisDate}>
            {(formattedAnalysisDate) => (
              <span className="sw-typo-semibold" title={formattedAnalysisDate}>
                <FormattedMessage
                  id="projects.last_analysis_on_x"
                  values={{
                    date: <DateFromNow className="sw-typo-default" date={analysisDate} />,
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
                  <span className="sw-typo-semibold sw-mr-1" data-key={MetricKey.new_lines}>
                    <Measure
                      componentKey={key}
                      metricKey={MetricKey.new_lines}
                      metricType={MetricType.ShortInteger}
                      value={measures.new_lines}
                    />
                  </span>

                  <span className="sw-typo-default">{translate('metric.new_lines.name')}</span>
                </div>
              </>
            )
          : measures[MetricKey.ncloc] != null && (
              <>
                <SeparatorCircleIcon className="sw-mx-1" />

                <div>
                  <span className="sw-typo-semibold sw-mr-1" data-key={MetricKey.ncloc}>
                    <Measure
                      componentKey={key}
                      metricKey={MetricKey.ncloc}
                      metricType={MetricType.ShortInteger}
                      value={measures.ncloc}
                    />
                  </span>

                  <span className="sw-typo-default">{translate('metric.ncloc.name')}</span>
                </div>

                <SeparatorCircleIcon className="sw-mx-1" />

                <span className="sw-typo-default" data-key={MetricKey.ncloc_language_distribution}>
                  <ProjectCardLanguages distribution={measures.ncloc_language_distribution} />
                </span>
              </>
            )}

        {tags.length > 0 && (
          <>
            <SeparatorCircleIcon className="sw-mx-1" />

            <Tags
              className="sw-typo-default"
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

function SecondLine({
  project,
  isNewCode,
}: Readonly<{ isNewCode: boolean; project: Props['project'] }>) {
  const { analysisDate, key, leakPeriodDate, measures, qualifier, isScannable } = project;
  const intl = useIntl();
  const { currentUser } = useCurrentUser();

  if (!isEmpty(analysisDate) && (!isNewCode || !isEmpty(leakPeriodDate))) {
    return (
      <ProjectCardMeasures
        measures={measures}
        componentQualifier={qualifier}
        componentKey={key}
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
          <Link
            aria-label={intl.formatMessage(
              { id: 'projects.configure_analysis_for_x' },
              { project: project.name },
            )}
            className="sw-ml-2 sw-typo-semibold"
            to={getProjectUrl(key)}
          >
            {translate('projects.configure_analysis')}
          </Link>
        )}
    </div>
  );
}

export default function ProjectCard(props: Readonly<Props>) {
  const { type, project } = props;
  const isNewCode = type === 'leak';

  return (
    <ProjectCardWrapper
      className={classNames(
        'it_project_card sw-relative sw-box-border sw-rounded-1 sw-mb-page sw-h-full',
      )}
      data-key={project.key}
    >
      {renderFirstLine(project, isNewCode)}

      <SubnavigationFlowSeparator className="sw-my-3" />

      <SecondLine project={project} isNewCode={isNewCode} />
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
