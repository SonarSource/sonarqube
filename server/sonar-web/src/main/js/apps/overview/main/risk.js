/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import moment from 'moment';
import React from 'react';

import {
    DomainPanel,
    DomainNutshell,
    DomainLeak,
    MeasuresList,
    Measure,
    DomainMixin
} from './components';
import { Rating } from './../../../components/shared/rating';
import { IssuesLink } from '../../../components/shared/issues-link';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { Legend } from '../components/legend';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure } from '../../../helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export const Risk = React.createClass({
  propTypes: {
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object
  },

  mixins: [TooltipsMixin, DomainMixin],

  renderLeak () {
    if (!this.hasLeakPeriod()) {
      return null;
    }

    const { snapshotDate } = this.props.component;
    const formattedSnapshotDate = moment(snapshotDate).format('LLL');
    const newBugs = this.props.leak['new_bugs'] || 0;
    const newVulnerabilities = this.props.leak['new_vulnerabilities'] || 0;

    return <DomainLeak>
      <Legend leakPeriodLabel={this.props.leakPeriodLabel} leakPeriodDate={this.props.leakPeriodDate}/>

      <MeasuresList>
        <Measure label={getMetricName('new_bugs')}>
          <IssuesLink
              component={this.props.component.key}
              params={{ resolved: 'false', types: 'BUG', sinceLeakPeriod: 'true' }}>
            <span
                title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                data-toggle="tooltip">
              {formatMeasure(newBugs, 'SHORT_INT')}
            </span>
          </IssuesLink>
        </Measure>
        <Measure label={getMetricName('new_vulnerabilities')}>
          <IssuesLink
              component={this.props.component.key}
              params={{ resolved: 'false', types: 'VULNERABILITY', sinceLeakPeriod: 'true' }}>
            <span
                title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                data-toggle="tooltip">
              {formatMeasure(newVulnerabilities, 'SHORT_INT')}
            </span>
          </IssuesLink>
        </Measure>
      </MeasuresList>
    </DomainLeak>;
  },

  render () {
    const { snapshotDate } = this.props.component;
    const formattedSnapshotDate = moment(snapshotDate).format('LLL');
    const bugs = this.props.measures['bugs'] || 0;
    const vulnerabilities = this.props.measures['vulnerabilities'] || 0;

    const bugsDomainUrl = window.baseUrl + '/component_measures/domain/Reliability?id=' +
        encodeURIComponent(this.props.component.key);
    const vulnerabilitiesDomainUrl = window.baseUrl + '/component_measures/domain/Security?id=' +
        encodeURIComponent(this.props.component.key);

    return <div className="overview-card overview-card-special">
      <div className="overview-card-header">
        <div className="overview-title">
          <a href={bugsDomainUrl}>
            {translate('metric.bugs.name')}
          </a>
          {' & '}
          <a href={vulnerabilitiesDomainUrl}>
            {translate('metric.vulnerabilities.name')}
          </a>
        </div>
      </div>

      <DomainPanel>
        <DomainNutshell>
          <MeasuresList>

            <Measure composite={true}>
              <div className="display-inline-block text-middle" style={{ paddingLeft: 56 }}>
                <div className="overview-domain-measure-value">
                  <IssuesLink
                      component={this.props.component.key}
                      params={{ resolved: 'false', types: 'BUG' }}>
                    <span
                        title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                        data-toggle="tooltip">
                      {formatMeasure(bugs, 'SHORT_INT')}
                    </span>
                  </IssuesLink>
                  <div className="overview-domain-measure-sup">
                    <DrilldownLink component={this.props.component.key} metric="reliability_rating">
                      <Rating value={this.props.measures['reliability_rating']}/>
                    </DrilldownLink>
                  </div>
                </div>
                <div className="overview-domain-measure-label">{getMetricName('bugs')}</div>
              </div>
            </Measure>

            <Measure composite={true}>
              <div className="display-inline-block text-middle">
                <div className="overview-domain-measure-value">
                  <IssuesLink
                      component={this.props.component.key}
                      params={{ resolved: 'false', types: 'VULNERABILITY' }}>
                    <span
                        title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                        data-toggle="tooltip">
                      {formatMeasure(vulnerabilities, 'SHORT_INT')}
                    </span>
                  </IssuesLink>
                  <div className="overview-domain-measure-sup">
                    <DrilldownLink component={this.props.component.key} metric="security_rating">
                      <Rating value={this.props.measures['security_rating']}/>
                    </DrilldownLink>
                  </div>
                </div>
                <div className="overview-domain-measure-label">{getMetricName('vulnerabilities')}</div>
              </div>
            </Measure>
          </MeasuresList>
        </DomainNutshell>
        {this.renderLeak()}
      </DomainPanel>
    </div>;
  }
});
