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
import { subDays } from 'date-fns';
import { throttle } from 'lodash';
import * as React from 'react';
import Select from 'sonar-ui-common/components/controls/Select';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { parseDate, toShortNotSoISOString } from 'sonar-ui-common/helpers/dates';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { scrollToElement } from 'sonar-ui-common/helpers/scrolling';
import { getProjectActivity } from '../../../api/projectActivity';
import DateFormatter from '../../../components/intl/DateFormatter';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import Events from '../../projectActivity/components/Events';
import { getAnalysesByVersionByDay } from '../../projectActivity/utils';

interface Props {
  analysis: string;
  branch: string;
  component: string;
  onSelectAnalysis: (analysis: T.ParsedAnalysis) => void;
}

interface State {
  analyses: T.ParsedAnalysis[];
  loading: boolean;
  range: number;
  scroll: number;
}

export default class BranchAnalysisList extends React.PureComponent<Props, State> {
  mounted = false;
  badges: T.Dict<HTMLDivElement> = {};
  rootNodeRef: React.RefObject<HTMLDivElement>;
  state: State = {
    analyses: [],
    loading: true,
    range: 30,
    scroll: 0
  };

  constructor(props: Props) {
    super(props);
    this.rootNodeRef = React.createRef<HTMLDivElement>();
    this.updateScroll = throttle(this.updateScroll, 20);
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchAnalyses(true);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  scrollToSelected() {
    const selectedNode = document.querySelector('.branch-analysis.selected');
    if (this.rootNodeRef.current && selectedNode) {
      scrollToElement(selectedNode, { parent: this.rootNodeRef.current, bottomOffset: 40 });
    }
  }

  fetchAnalyses(initial = false) {
    const { analysis, branch, component } = this.props;
    const { range } = this.state;
    this.setState({ loading: true });

    return getProjectActivity({
      branch,
      project: component,
      from: range ? toShortNotSoISOString(subDays(new Date(), range)) : undefined
    }).then((result: { analyses: T.Analysis[] }) => {
      // If the selected analysis wasn't found in the default 30 days range, redo the search
      if (initial && analysis && !result.analyses.find(a => a.key === analysis)) {
        this.handleRangeChange({ value: 0 });
        return;
      }

      this.setState(
        {
          analyses: result.analyses.map(analysis => ({
            ...analysis,
            date: parseDate(analysis.date)
          })) as T.ParsedAnalysis[],
          loading: false
        },
        () => {
          this.scrollToSelected();
        }
      );
    });
  }

  handleScroll = (e: React.SyntheticEvent<HTMLDivElement>) => {
    if (e.currentTarget) {
      this.updateScroll(e.currentTarget.scrollTop);
    }
  };

  updateScroll = (scroll: number) => {
    this.setState({ scroll });
  };

  registerBadgeNode = (version: string) => (el: HTMLDivElement) => {
    if (el) {
      if (!el.getAttribute('originOffsetTop')) {
        el.setAttribute('originOffsetTop', String(el.offsetTop));
      }
      this.badges[version] = el;
    }
  };

  shouldStick = (version: string) => {
    const badge = this.badges[version];
    return badge && Number(badge.getAttribute('originOffsetTop')) < this.state.scroll + 10;
  };

  getRangeOptions() {
    return [
      {
        label: translate('baseline.branch_analyses.ranges.30days'),
        value: 30
      },
      {
        label: translate('baseline.branch_analyses.ranges.allTime'),
        value: 0
      }
    ];
  }

  handleRangeChange = ({ value }: { value: number }) => {
    this.setState({ range: value }, () => this.fetchAnalyses());
  };

  render() {
    const { analyses, loading, range } = this.state;

    const byVersionByDay = getAnalysesByVersionByDay(analyses, {
      category: ''
    });

    const hasFilteredData =
      byVersionByDay.length > 1 ||
      (byVersionByDay.length === 1 && Object.keys(byVersionByDay[0].byDay).length > 0);

    return (
      <>
        <div className="spacer-bottom">
          {translate('baseline.analysis_from')}
          <Select
            autoBlur={true}
            className="input-medium spacer-left"
            clearable={false}
            onChange={this.handleRangeChange}
            options={this.getRangeOptions()}
            searchable={false}
            value={range}
          />
        </div>
        <div className="branch-analysis-list-wrapper">
          <div
            className="bordered branch-analysis-list"
            onScroll={this.handleScroll}
            ref={this.rootNodeRef}>
            {loading && <DeferredSpinner className="big-spacer-top" />}

            {!loading && !hasFilteredData ? (
              <div className="big-spacer-top big-spacer-bottom strong">
                {translate('baseline.no_analyses')}
              </div>
            ) : (
              <ul>
                {byVersionByDay.map((version, idx) => {
                  const days = Object.keys(version.byDay);
                  if (days.length <= 0) {
                    return null;
                  }
                  return (
                    <li key={version.key || 'noversion'}>
                      {version.version && (
                        <div
                          className={classNames('branch-analysis-version-badge', {
                            first: idx === 0,
                            sticky: this.shouldStick(version.version)
                          })}
                          ref={this.registerBadgeNode(version.version)}>
                          <Tooltip
                            mouseEnterDelay={0.5}
                            overlay={`${translate('version')} ${version.version}`}>
                            <span className="badge">{version.version}</span>
                          </Tooltip>
                        </div>
                      )}
                      <ul className="branch-analysis-days-list">
                        {days.map(day => (
                          <li
                            className="branch-analysis-day"
                            data-day={toShortNotSoISOString(Number(day))}
                            key={day}>
                            <div className="branch-analysis-date">
                              <DateFormatter date={Number(day)} long={true} />
                            </div>
                            <ul className="branch-analysis-analyses-list">
                              {version.byDay[day] != null &&
                                version.byDay[day].map(analysis => (
                                  <li
                                    className={classNames('branch-analysis', {
                                      selected: analysis.key === this.props.analysis
                                    })}
                                    data-date={parseDate(analysis.date).valueOf()}
                                    key={analysis.key}
                                    onClick={() => this.props.onSelectAnalysis(analysis)}>
                                    <div className="branch-analysis-time spacer-right">
                                      <TimeFormatter date={parseDate(analysis.date)} long={false}>
                                        {formattedTime => (
                                          <time
                                            className="text-middle"
                                            dateTime={parseDate(analysis.date).toISOString()}>
                                            {formattedTime}
                                          </time>
                                        )}
                                      </TimeFormatter>
                                    </div>

                                    {analysis.events.length > 0 && (
                                      <Events
                                        analysisKey={analysis.key}
                                        events={analysis.events}
                                        isFirst={analyses[0].key === analysis.key}
                                      />
                                    )}

                                    <div className="analysis-selection-button">
                                      <i
                                        className={classNames('icon-radio', {
                                          'is-checked': analysis.key === this.props.analysis
                                        })}
                                      />
                                    </div>
                                  </li>
                                ))}
                            </ul>
                          </li>
                        ))}
                      </ul>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      </>
    );
  }
}
