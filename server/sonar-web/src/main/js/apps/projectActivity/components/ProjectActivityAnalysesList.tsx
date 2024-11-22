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
import { Spinner, Text } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { isEqual } from 'date-fns';
import * as React from 'react';
import { Badge, HelperHintIcon, themeColor } from '~design-system';
import Tooltip from '../../../components/controls/Tooltip';
import DateFormatter from '../../../components/intl/DateFormatter';
import { toShortISO8601String } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';

import { ComponentQualifier } from '~sonar-aligned/types/component';
import { ParsedAnalysis } from '../../../types/project-activity';
import { AnalysesByDay, Query, activityQueryChanged, getAnalysesByVersionByDay } from '../utils';
import ProjectActivityAnalysis, { BaselineMarker } from './ProjectActivityAnalysis';

interface Props {
  analyses: ParsedAnalysis[];
  analysesLoading: boolean;
  canAdmin?: boolean;
  canDeleteAnalyses?: boolean;
  initializing: boolean;
  leakPeriodDate?: Date;
  onUpdateQuery: (changes: Partial<Query>) => void;
  project: { qualifier: string };
  query: Query;
}

const LIST_MARGIN_TOP = 24;

export default class ProjectActivityAnalysesList extends React.PureComponent<Props> {
  scrollContainer?: HTMLUListElement | null;

  componentDidUpdate(prevProps: Props) {
    const selectedDate = this.props.query.selectedDate
      ? this.props.query.selectedDate.valueOf()
      : null;

    if (
      this.scrollContainer &&
      activityQueryChanged(prevProps.query, this.props.query) &&
      !this.props.analyses.some(({ date }) => date.valueOf() === selectedDate)
    ) {
      this.scrollContainer.scrollTop = 0;
    }
  }

  handleUpdateSelectedDate = (date: Date) => {
    this.props.onUpdateQuery({ selectedDate: date });
  };

  getNewCodePeriodStartKey(versionByDay: AnalysesByDay[]): {
    baselineAnalysisKey: string | undefined;
    firstNewCodeAnalysisKey: string | undefined;
  } {
    const { leakPeriodDate } = this.props;
    if (!leakPeriodDate) {
      return { firstNewCodeAnalysisKey: undefined, baselineAnalysisKey: undefined };
    }
    // In response, the first new code analysis comes before the baseline analysis
    // This variable is to track the previous analysis and return when next is baseline analysis
    let prevAnalysis;
    for (const version of versionByDay) {
      const days = Object.keys(version.byDay);
      for (const day of days) {
        for (const analysis of version.byDay[day]) {
          if (isEqual(leakPeriodDate, analysis.date)) {
            return {
              firstNewCodeAnalysisKey: prevAnalysis?.key,
              baselineAnalysisKey: analysis.key,
            };
          }
          prevAnalysis = analysis;
        }
      }
    }

    return { firstNewCodeAnalysisKey: undefined, baselineAnalysisKey: undefined };
  }

  renderAnalysis(analysis: ParsedAnalysis, newCodeKey?: string) {
    const firstAnalysisKey = this.props.analyses[0].key;

    const selectedDate = this.props.query.selectedDate
      ? this.props.query.selectedDate.valueOf()
      : null;

    return (
      <ProjectActivityAnalysis
        analysis={analysis}
        canAdmin={this.props.canAdmin}
        canCreateVersion={this.props.project.qualifier === ComponentQualifier.Project}
        canDeleteAnalyses={this.props.canDeleteAnalyses}
        isBaseline={analysis.key === newCodeKey}
        isFirst={analysis.key === firstAnalysisKey}
        key={analysis.key}
        selected={analysis.date.valueOf() === selectedDate}
        onUpdateSelectedDate={this.handleUpdateSelectedDate}
      />
    );
  }

  render() {
    const { analyses, query, initializing } = this.props;
    const byVersionByDay = getAnalysesByVersionByDay(analyses, query);
    const newCodePeriod = this.getNewCodePeriodStartKey(byVersionByDay);
    const hasFilteredData =
      byVersionByDay.length > 1 ||
      (byVersionByDay.length === 1 && Object.keys(byVersionByDay[0].byDay).length > 0);
    const hasData = analyses.length > 0 && hasFilteredData;

    return (
      <>
        <div aria-live="polite">
          <Spinner isLoading={initializing}>
            {!hasData && (
              <div className="sw-p-4 sw-typo-default">
                <Text isSubdued>{translate('no_results')}</Text>
              </div>
            )}
          </Spinner>
        </div>

        {hasData && (
          <ul
            className="it__project-activity-versions-list sw-box-border sw-overflow-auto sw-grow sw-shrink-0 sw-py-0 sw-px-4"
            ref={(element) => (this.scrollContainer = element)}
            style={{
              height: 'calc(100vh - 250px)',
              marginTop:
                this.props.project.qualifier === ComponentQualifier.Project
                  ? LIST_MARGIN_TOP
                  : undefined,
            }}
          >
            {newCodePeriod.baselineAnalysisKey !== undefined &&
              newCodePeriod.firstNewCodeAnalysisKey === undefined && (
                <BaselineMarker className="sw-typo-default sw-mb-2">
                  <span className="sw-py-1/2 sw-px-1">
                    {translate('project_activity.new_code_period_start')}
                  </span>
                  <Tooltip
                    content={translate('project_activity.new_code_period_start.help')}
                    side="top"
                  >
                    <HelperHintIcon className="sw-ml-1" />
                  </Tooltip>
                </BaselineMarker>
              )}

            {byVersionByDay.map((version, idx) => {
              const days = Object.keys(version.byDay);
              if (days.length <= 0) {
                return null;
              }

              return (
                <li key={version.key || 'noversion'}>
                  {version.version && (
                    <VersionTagStyled
                      className={classNames(
                        'sw-sticky sw-top-0 sw-left-0 sw-pb-1 -sw-ml-4 sw-z-normal',
                        {
                          'sw-top-0 sw-pt-0': idx === 0,
                        },
                      )}
                    >
                      <Tooltip
                        mouseEnterDelay={0.5}
                        content={`${translate('version')} ${version.version}`}
                      >
                        <Badge className="sw-p-1">{version.version}</Badge>
                      </Tooltip>
                    </VersionTagStyled>
                  )}
                  <ul className="it__project-activity-days-list">
                    {days.map((day) => (
                      <li
                        className="it__project-activity-day sw-mt-1 sw-mb-4"
                        data-day={toShortISO8601String(Number(day))}
                        key={day}
                      >
                        <div className="sw-typo-lg-semibold sw-mb-3">
                          <DateFormatter date={Number(day)} long />
                        </div>
                        <ul className="it__project-activity-analyses-list">
                          {version.byDay[day]?.map((analysis) =>
                            this.renderAnalysis(analysis, newCodePeriod.firstNewCodeAnalysisKey),
                          )}
                        </ul>
                      </li>
                    ))}
                  </ul>
                </li>
              );
            })}
            {this.props.analysesLoading && (
              <li className="sw-text-center">
                <Spinner />
              </li>
            )}
          </ul>
        )}
      </>
    );
  }
}

const VersionTagStyled = styled.div`
  background-color: ${themeColor('backgroundSecondary')};
`;
