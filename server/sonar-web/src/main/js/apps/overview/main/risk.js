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
import { Legend } from '../components/legend';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';


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

    const createdAfter = moment(this.props.leakPeriodDate).format('YYYY-MM-DDTHH:mm:ssZZ');
    const newBugs = this.props.leak['new_bugs'] || 0;
    const newVulnerabilities = this.props.leak['new_vulnerabilities'] || 0;

    return <DomainLeak>
      <Legend leakPeriodLabel={this.props.leakPeriodLabel} leakPeriodDate={this.props.leakPeriodDate}/>

      <MeasuresList>
        <Measure label={getMetricName('new_bugs')}>
          <IssuesLink
              component={this.props.component.key}
              params={{ resolved: 'false', types: 'BUG', createdAfter }}>
            {formatMeasure(newBugs, 'SHORT_INT')}
          </IssuesLink>
        </Measure>
        <Measure label={getMetricName('new_vulnerabilities')}>
          <IssuesLink
              component={this.props.component.key}
              params={{ resolved: 'false', types: 'VULNERABILITY', createdAfter }}>
            {formatMeasure(newVulnerabilities, 'SHORT_INT')}
          </IssuesLink>
        </Measure>
      </MeasuresList>
    </DomainLeak>;
  },

  render () {
    const bugs = this.props.measures['bugs'] || 0;
    const vulnerabilities = this.props.measures['vulnerabilities'] || 0;

    return <div className="overview-card overview-card-special">
      <DomainHeader component={this.props.component}
                    title={translate('overview.domain.risk')}/>

      <DomainPanel>
        <DomainNutshell>
          <MeasuresList>

            <Measure composite={true}>
              <div className="display-inline-block text-middle big-spacer-right">
                <div className="overview-domain-measure-value">
                  <DrilldownLink component={this.props.component.key} metric="reliability_rating">
                    <Rating value={this.props.measures['reliability_rating']}/>
                  </DrilldownLink>
                </div>
              </div>
              <div className="display-inline-block text-middle">
                <div className="overview-domain-measure-value">
                  <IssuesLink
                      component={this.props.component.key}
                      params={{ resolved: 'false', types: 'BUG' }}>
                    {formatMeasure(bugs, 'SHORT_INT')}
                  </IssuesLink>
                </div>
                <div className="overview-domain-measure-label">{getMetricName('bugs')}</div>
              </div>
            </Measure>

            <Measure composite={true}>
              <div className="display-inline-block text-middle big-spacer-right">
                <div className="overview-domain-measure-value">
                  <DrilldownLink component={this.props.component.key} metric="security_rating">
                    <Rating value={this.props.measures['security_rating']}/>
                  </DrilldownLink>
                </div>
              </div>
              <div className="display-inline-block text-middle">
                <div className="overview-domain-measure-value">
                  <IssuesLink
                      component={this.props.component.key}
                      params={{ resolved: 'false', types: 'VULNERABILITY' }}>
                    {formatMeasure(vulnerabilities, 'SHORT_INT')}
                  </IssuesLink>
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
