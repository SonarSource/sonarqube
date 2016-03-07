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
import React from 'react';

import { Domain,
         DomainHeader,
         DomainPanel,
         DomainNutshell,
         DomainLeak,
         MeasuresList,
         Measure,
         DomainMixin } from './components';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure, formatMeasureVariation } from '../../../helpers/measures';
import { LanguageDistribution } from '../components/language-distribution';
import { translate } from '../../../helpers/l10n';


export const GeneralStructure = React.createClass({
  propTypes: {
    leakPeriodLabel: React.PropTypes.string,
    leakPeriodDate: React.PropTypes.object
  },

  mixins: [TooltipsMixin, DomainMixin],

  renderLeak () {
    if (!this.hasLeakPeriod()) {
      return null;
    }
    const measure = this.props.leak['ncloc'];
    const formatted = measure != null ? formatMeasureVariation(measure, 'SHORT_INT') : '—';
    return <DomainLeak>
      <MeasuresList>
        <Measure label={getMetricName('ncloc')}>{formatted}</Measure>
      </MeasuresList>
      {this.renderTimeline('after')}
    </DomainLeak>;
  },

  renderLanguageDistribution() {
    if (!this.props.measures['ncloc'] || !this.props.measures['ncloc_language_distribution']) {
      return null;
    }
    return <Measure composite={true}>
      <div style={{ width: 200 }}>
        <LanguageDistribution lines={Number(this.props.measures['ncloc'])}
                              distribution={this.props.measures['ncloc_language_distribution']}/>
      </div>
    </Measure>;
  },

  render () {
    return <Domain>
      <DomainHeader component={this.props.component}
                    title={translate('overview.domain.structure')}/>

      <DomainPanel>
        <DomainNutshell>
          <MeasuresList>
            {this.renderLanguageDistribution()}
            <Measure label={getMetricName('ncloc')}>
              <DrilldownLink component={this.props.component.key} metric="ncloc">
                {formatMeasure(this.props.measures['ncloc'], 'SHORT_INT')}
              </DrilldownLink>
            </Measure>
          </MeasuresList>
          {this.renderTimeline('before')}
        </DomainNutshell>
        {this.renderLeak()}
      </DomainPanel>
    </Domain>;
  }
});
