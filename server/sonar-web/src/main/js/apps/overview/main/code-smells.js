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
    Domain,
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
import { getMetricName } from '../helpers/metrics';
import { formatMeasure } from '../../../helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export const CodeSmells = React.createClass({
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
    const newDebt = this.props.leak['new_technical_debt'] || 0;
    const newCodeSmells = this.props.leak['new_code_smells'] || 0;

    return <DomainLeak>
      <MeasuresList>
        <Measure label={getMetricName('new_code_smells')}>
          <IssuesLink
              component={this.props.component.key}
              params={{ resolved: 'false', types: 'CODE_SMELL', sinceLeakPeriod: 'true' }}>
            <span
                title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                data-toggle="tooltip">
              {formatMeasure(newCodeSmells, 'SHORT_INT')}
            </span>
          </IssuesLink>
        </Measure>
        <Measure label={getMetricName('new_effort')}>
          <IssuesLink
              component={this.props.component.key}
              params={{ resolved: 'false', types: 'CODE_SMELL', facetMode: 'debt', sinceLeakPeriod: 'true' }}>
            <span
                title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                data-toggle="tooltip">
              {formatMeasure(newDebt, 'SHORT_WORK_DUR')}
            </span>
          </IssuesLink>
        </Measure>
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  render () {
    const debt = this.props.measures['sqale_index'] || 0;
    const codeSmells = this.props.measures['code_smells'] || 0;
    const { snapshotDate } = this.props.component;
    const formattedSnapshotDate = moment(snapshotDate).format('LLL');

    const domainUrl = window.baseUrl + '/component_measures/domain/Maintainability?id=' +
        encodeURIComponent(this.props.component.key);

    return <Domain>
      <div className="overview-card-header">
        <div className="overview-title">
          <a href={domainUrl}>
            {translate('metric.code_smells.name')}
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
                      params={{ resolved: 'false', types: 'CODE_SMELL' }}>
                    <span
                        title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                        data-toggle="tooltip">
                      {formatMeasure(codeSmells, 'SHORT_INT')}
                    </span>
                  </IssuesLink>
                  <div className="overview-domain-measure-sup">
                    <DrilldownLink component={this.props.component.key} metric="sqale_rating">
                      <Rating value={this.props.measures['sqale_rating']}/>
                    </DrilldownLink>
                  </div>
                </div>
                <div className="overview-domain-measure-label">{getMetricName('code_smells')}</div>
              </div>
            </Measure>

            <Measure label={getMetricName('effort')}>
              <IssuesLink
                  component={this.props.component.key}
                  params={{ resolved: 'false', types: 'CODE_SMELL', facetMode: 'debt' }}>
                <span
                    title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                    data-toggle="tooltip">
                  {formatMeasure(debt, 'SHORT_WORK_DUR')}
                </span>
              </IssuesLink>
            </Measure>
          </MeasuresList>
          {this.renderTimeline('before', true)}
        </DomainNutshell>
        {this.renderLeak()}
      </DomainPanel>
    </Domain>;
  }
});
