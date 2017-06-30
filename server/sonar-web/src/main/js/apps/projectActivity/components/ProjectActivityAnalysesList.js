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
import { throttle } from 'lodash';
import ProjectActivityAnalysis from './ProjectActivityAnalysis';
import FormattedDate from '../../../components/ui/FormattedDate';
import { translate } from '../../../helpers/l10n';
import { activityQueryChanged, getAnalysesByVersionByDay } from '../utils';
import type { Analysis, Query } from '../types';

type Props = {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<*>,
  addVersion: (analysis: string, version: string) => Promise<*>,
  analyses: Array<Analysis>,
  analysesLoading: boolean,
  canAdmin: boolean,
  className?: string,
  changeEvent: (event: string, name: string) => Promise<*>,
  deleteAnalysis: (analysis: string) => Promise<*>,
  deleteEvent: (analysis: string, event: string) => Promise<*>,
  loading: boolean,
  query: Query
};

export default class ProjectActivityAnalysesList extends React.PureComponent {
  scrollContainer: HTMLElement;
  badges: HTMLCollection<HTMLElement>;
  props: Props;

  constructor(props: Props) {
    super(props);
    this.handleScroll = throttle(this.handleScroll, 20);
  }

  componentDidMount() {
    this.badges = document.getElementsByClassName('project-activity-version-badge');
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.analysis !== this.props.analyses && this.scrollContainer) {
      if (activityQueryChanged(prevProps.query, this.props.query)) {
        this.scrollContainer.scrollTop = 0;
      }
      for (let i = 1; i < this.badges.length; i++) {
        this.badges[i].removeAttribute('originOffsetTop');
        this.badges[i].classList.remove('sticky');
      }
      this.handleScroll();
    }
  }

  handleScroll = () => {
    if (this.scrollContainer && this.badges) {
      const scrollTop = this.scrollContainer.scrollTop;
      if (scrollTop != null) {
        let newScrollTop;
        for (let i = 1; i < this.badges.length; i++) {
          const badge = this.badges[i];
          let originOffsetTop = badge.getAttribute('originOffsetTop');
          if (originOffsetTop == null) {
            originOffsetTop = badge.offsetTop;
            badge.setAttribute('originOffsetTop', originOffsetTop.toString());
          }
          if (Number(originOffsetTop) < scrollTop + 18 + i * 2) {
            if (!badge.classList.contains('sticky')) {
              newScrollTop = originOffsetTop;
            }
            badge.classList.add('sticky');
          } else {
            badge.classList.remove('sticky');
          }
        }
        if (newScrollTop != null) {
          this.scrollContainer.scrollTop = newScrollTop - 6;
        }
      }
    }
  };

  render() {
    if (this.props.analyses.length === 0) {
      return (
        <div className={this.props.className}>
          {this.props.loading
            ? <div className="text-center"><i className="spinner" /></div>
            : <span className="note">{translate('no_results')}</span>}
        </div>
      );
    }

    const firstAnalysisKey = this.props.analyses[0].key;
    const byVersionByDay = getAnalysesByVersionByDay(this.props.analyses);
    return (
      <ul
        className={classNames('project-activity-versions-list', this.props.className)}
        onScroll={this.handleScroll}
        ref={element => (this.scrollContainer = element)}>
        {byVersionByDay.map((version, idx) => (
          <li key={version.key || 'noversion'}>
            {version.version &&
              <div className={classNames('project-activity-version-badge', { first: idx === 0 })}>
                <span className="badge">
                  {version.version}
                </span>
              </div>}
            <ul className="project-activity-days-list">
              {Object.keys(version.byDay).map(day => (
                <li
                  key={day}
                  className="project-activity-day"
                  data-day={moment(Number(day)).format('YYYY-MM-DD')}>
                  <div className="project-activity-date">
                    <FormattedDate date={Number(day)} format="LL" />
                  </div>
                  <ul className="project-activity-analyses-list">
                    {version.byDay[day] != null &&
                      version.byDay[day].map(analysis => (
                        <ProjectActivityAnalysis
                          addCustomEvent={this.props.addCustomEvent}
                          addVersion={this.props.addVersion}
                          analysis={analysis}
                          canAdmin={this.props.canAdmin}
                          changeEvent={this.props.changeEvent}
                          deleteAnalysis={this.props.deleteAnalysis}
                          deleteEvent={this.props.deleteEvent}
                          isFirst={analysis.key === firstAnalysisKey}
                          key={analysis.key}
                        />
                      ))}
                  </ul>
                </li>
              ))}
            </ul>
          </li>
        ))}
        {this.props.analysesLoading && <li className="text-center"><i className="spinner" /></li>}
      </ul>
    );
  }
}
