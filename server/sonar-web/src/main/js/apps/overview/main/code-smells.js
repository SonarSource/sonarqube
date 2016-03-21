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
    DomainHeader,
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
    const createdAfter = moment(this.props.leakPeriodDate).format('YYYY-MM-DDTHH:mm:ssZZ');
    const newDebt = this.props.leak['new_technical_debt'] || 0;
    const newCodeSmells = this.props.leak['new_code_smells'] || 0;

    return <DomainLeak>
      <MeasuresList>
        <Measure label={getMetricName('new_effort')}>
          <IssuesLink
              component={this.props.component.key}
              params={{ resolved: 'false', createdAfter, types: 'CODE_SMELL', facetMode: 'debt' }}>
            <span
                title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                data-toggle="tooltip">
              {formatMeasure(newDebt, 'SHORT_WORK_DUR')}
            </span>
          </IssuesLink>
        </Measure>
        <Measure label={getMetricName('new_code_smells')}>
          <IssuesLink
              component={this.props.component.key}
              params={{ resolved: 'false', types: 'CODE_SMELL', createdAfter }}>
            <span
                title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                data-toggle="tooltip">
              {formatMeasure(newCodeSmells, 'SHORT_INT')}
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

    return <Domain>
      <DomainHeader component={this.props.component}
                    title={translate('overview.domain.code_smells')}/>

      <DomainPanel>
        <DomainNutshell>
          <MeasuresList>

            <Measure composite={true}>
              <div className="display-inline-block text-middle big-spacer-right">
                <div
                    className="overview-domain-measure-value"
                    title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                    data-toggle="tooltip">
                  <DrilldownLink component={this.props.component.key} metric="sqale_rating">
                    <Rating value={this.props.measures['sqale_rating']}/>
                  </DrilldownLink>
                </div>
              </div>
              <div className="display-inline-block text-middle">
                <div
                    className="overview-domain-measure-value"
                    title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                    data-toggle="tooltip">
                  <IssuesLink
                      component={this.props.component.key}
                      params={{ resolved: 'false', types: 'CODE_SMELL', facetMode: 'debt' }}>
                    {formatMeasure(debt, 'SHORT_WORK_DUR')}
                  </IssuesLink>
                </div>
                <div className="overview-domain-measure-label">{getMetricName('effort')}</div>
              </div>
            </Measure>

            <Measure label={getMetricName('code_smells')}>
              <IssuesLink
                  component={this.props.component.key}
                  params={{ resolved: 'false', types: 'CODE_SMELL' }}>
                <span
                    title={translateWithParameters('widget.as_calculated_on_x', formattedSnapshotDate)}
                    data-toggle="tooltip">
                  {formatMeasure(codeSmells, 'SHORT_INT')}
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
