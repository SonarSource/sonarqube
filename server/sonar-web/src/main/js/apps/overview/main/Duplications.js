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

import enhance from './enhance';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { getMetricName } from '../helpers/metrics';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

class Duplications extends React.Component {
  renderHeader () {
    return this.props.renderHeader(
        'Duplications',
        translate('overview.domain.duplications'));
  }

  renderTimeline (range) {
    return this.props.renderTimeline('duplicated_lines_density', range);
  }

  renderDuplicatedBlocks () {
    return this.props.renderMeasure('duplicated_blocks');
  }

  renderDuplicationsDonut (duplications) {
    const data = [
      { value: duplications, fill: '#f3ca8e' },
      { value: Math.max(0, 20 - duplications), fill: '#e6e6e6' }
    ];
    return this.props.renderDonut(data);
  }

  renderDuplications () {
    const { component, measures } = this.props;
    const measure = measures.find(measure => measure.metric.key === 'duplicated_lines_density');
    const duplications = Number(measure.value);

    return (
        <div className="overview-domain-measure">
          {this.renderDuplicationsDonut(duplications)}

          <div className="display-inline-block text-middle">
            <div className="overview-domain-measure-value">
              <DrilldownLink component={component.key} metric="duplicated_lines_density">
                {formatMeasure(duplications, 'PERCENT')}
              </DrilldownLink>
            </div>

            <div className="overview-domain-measure-label">
              {getMetricName('duplications')}
            </div>
          </div>
        </div>
    );
  }

  renderNutshell () {
    return (
        <div className="overview-domain-nutshell">
          <div className="overview-domain-measures">
            {this.renderDuplications()}
            {this.renderDuplicatedBlocks()}
          </div>

          {this.renderTimeline('before')}
        </div>
    );
  }

  renderLeak () {
    const { leakPeriod } = this.props;

    if (leakPeriod == null) {
      return null;
    }

    const measure = this.props.renderMeasureVariation(
        'duplicated_lines_density',
        getMetricName('duplications'));

    return (
        <div className="overview-domain-leak">
          <div className="overview-domain-measures">
            {measure}
          </div>

          {this.renderTimeline('after')}
        </div>
    );
  }

  render () {
    const { measures } = this.props;
    const duplications =
        measures.find(measure => measure.metric.key === 'duplicated_lines_density');

    if (duplications == null) {
      return null;
    }

    return (
        <div className="overview-card">
          {this.renderHeader()}

          <div className="overview-domain-panel">
            {this.renderNutshell()}
            {this.renderLeak()}
          </div>
        </div>
    );
  }
}

export default enhance(Duplications);
