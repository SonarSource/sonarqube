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
import { isEqual } from 'date-fns';
import { throttle } from 'lodash';
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import DateFormatter from '../../../components/intl/DateFormatter';
import { toShortNotSoISOString } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { ComponentQualifier } from '../../../types/component';
import { ParsedAnalysis } from '../../../types/project-activity';
import { activityQueryChanged, getAnalysesByVersionByDay, Query } from '../utils';
import ProjectActivityAnalysis from './ProjectActivityAnalysis';

interface Props {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<void>;
  addVersion: (analysis: string, version: string) => Promise<void>;
  analyses: ParsedAnalysis[];
  analysesLoading: boolean;
  canAdmin?: boolean;
  canDeleteAnalyses?: boolean;
  changeEvent: (event: string, name: string) => Promise<void>;
  deleteAnalysis: (analysis: string) => Promise<void>;
  deleteEvent: (analysis: string, event: string) => Promise<void>;
  initializing: boolean;
  leakPeriodDate?: Date;
  project: { qualifier: string };
  query: Query;
  updateQuery: (changes: Partial<Query>) => void;
}

const LIST_MARGIN_TOP = 36;

export default class ProjectActivityAnalysesList extends React.PureComponent<Props> {
  analyses?: HTMLCollectionOf<HTMLElement>;
  badges?: HTMLCollectionOf<HTMLElement>;
  scrollContainer?: HTMLUListElement | null;

  constructor(props: Props) {
    super(props);
    this.handleScroll = throttle(this.handleScroll, 20);
  }

  componentDidMount() {
    this.badges = document.getElementsByClassName(
      'project-activity-version-badge'
    ) as HTMLCollectionOf<HTMLElement>;
    this.analyses = document.getElementsByClassName(
      'project-activity-analysis'
    ) as HTMLCollectionOf<HTMLElement>;
  }

  componentDidUpdate(prevProps: Props) {
    if (!this.scrollContainer) {
      return;
    }
    if (activityQueryChanged(prevProps.query, this.props.query)) {
      this.resetScrollTop(0, true);
    }
  }

  handleScroll = () => this.updateStickyBadges(true);

  resetScrollTop = (newScrollTop: number, forceBadgeAlignement?: boolean) => {
    if (this.scrollContainer) {
      this.scrollContainer.scrollTop = newScrollTop;
    }
    if (this.badges) {
      for (let i = 1; i < this.badges.length; i++) {
        this.badges[i].removeAttribute('originOffsetTop');
        this.badges[i].classList.remove('sticky');
      }
    }
    this.updateStickyBadges(forceBadgeAlignement);
  };

  updateStickyBadges = (forceBadgeAlignement?: boolean) => {
    if (!this.scrollContainer || !this.badges) {
      return;
    }

    const { scrollTop } = this.scrollContainer;
    if (scrollTop == null) {
      return;
    }

    let newScrollTop;
    for (let i = 1; i < this.badges.length; i++) {
      const badge = this.badges[i];
      let originOffsetTop = badge.getAttribute('originOffsetTop');
      if (originOffsetTop == null) {
        // Set the originOffsetTop attribute, to avoid using getBoundingClientRect
        originOffsetTop = String(badge.offsetTop);
        badge.setAttribute('originOffsetTop', originOffsetTop);
      }
      if (Number(originOffsetTop) < scrollTop + 18 + i * 2) {
        if (forceBadgeAlignement && !badge.classList.contains('sticky')) {
          newScrollTop = originOffsetTop;
        }
        badge.classList.add('sticky');
      } else {
        badge.classList.remove('sticky');
      }
    }

    if (forceBadgeAlignement && newScrollTop != null) {
      this.scrollContainer.scrollTop = Number(newScrollTop) - 6;
    }
  };

  updateSelectedDate = (date: Date) => {
    this.props.updateQuery({ selectedDate: date });
  };

  shouldRenderBaselineMarker(analysis: ParsedAnalysis): boolean {
    return Boolean(this.props.leakPeriodDate && isEqual(this.props.leakPeriodDate, analysis.date));
  }

  renderAnalysis(analysis: ParsedAnalysis) {
    const firstAnalysisKey = this.props.analyses[0].key;

    const selectedDate = this.props.query.selectedDate
      ? this.props.query.selectedDate.valueOf()
      : null;

    return (
      <ProjectActivityAnalysis
        addCustomEvent={this.props.addCustomEvent}
        addVersion={this.props.addVersion}
        analysis={analysis}
        canAdmin={this.props.canAdmin}
        canCreateVersion={this.props.project.qualifier === ComponentQualifier.Project}
        canDeleteAnalyses={this.props.canDeleteAnalyses}
        changeEvent={this.props.changeEvent}
        deleteAnalysis={this.props.deleteAnalysis}
        deleteEvent={this.props.deleteEvent}
        isBaseline={this.shouldRenderBaselineMarker(analysis)}
        isFirst={analysis.key === firstAnalysisKey}
        key={analysis.key}
        selected={analysis.date.valueOf() === selectedDate}
        updateSelectedDate={this.updateSelectedDate}
      />
    );
  }

  render() {
    const byVersionByDay = getAnalysesByVersionByDay(this.props.analyses, this.props.query);
    const hasFilteredData =
      byVersionByDay.length > 1 ||
      (byVersionByDay.length === 1 && Object.keys(byVersionByDay[0].byDay).length > 0);
    if (this.props.analyses.length === 0 || !hasFilteredData) {
      return (
        <div className="boxed-group-inner">
          {this.props.initializing ? (
            <div className="text-center">
              <i className="spinner" />
            </div>
          ) : (
            <span className="note">{translate('no_results')}</span>
          )}
        </div>
      );
    }

    return (
      <ul
        className="project-activity-versions-list"
        onScroll={this.handleScroll}
        ref={(element) => (this.scrollContainer = element)}
        style={{
          marginTop:
            this.props.project.qualifier === ComponentQualifier.Project
              ? LIST_MARGIN_TOP
              : undefined,
        }}
      >
        {byVersionByDay.map((version, idx) => {
          const days = Object.keys(version.byDay);
          if (days.length <= 0) {
            return null;
          }
          return (
            <li key={version.key || 'noversion'}>
              {version.version && (
                <div className={classNames('project-activity-version-badge', { first: idx === 0 })}>
                  <Tooltip
                    mouseEnterDelay={0.5}
                    overlay={`${translate('version')} ${version.version}`}
                  >
                    <h2 className="analysis-version">{version.version}</h2>
                  </Tooltip>
                </div>
              )}
              <ul className="project-activity-days-list">
                {days.map((day) => (
                  <li
                    className="project-activity-day"
                    data-day={toShortNotSoISOString(Number(day))}
                    key={day}
                  >
                    <h3>
                      <DateFormatter date={Number(day)} long={true} />
                    </h3>
                    <ul className="project-activity-analyses-list">
                      {version.byDay[day] != null &&
                        version.byDay[day].map((analysis) => this.renderAnalysis(analysis))}
                    </ul>
                  </li>
                ))}
              </ul>
            </li>
          );
        })}
        {this.props.analysesLoading && (
          <li className="text-center">
            <i className="spinner" />
          </li>
        )}
      </ul>
    );
  }
}
