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
import LanguageDistribution from '../../../components/charts/LanguageDistribution';
import { translate } from '../../../helpers/l10n';

class Size extends React.Component {
  renderHeader () {
    return this.props.renderHeader(
        'Size',
        translate('overview.domain.structure'));
  }

  renderTimeline (range) {
    return this.props.renderTimeline('ncloc', range);
  }

  renderLeak () {
    const { leakPeriod } = this.props;

    if (leakPeriod == null) {
      return null;
    }

    return (
        <div className="overview-domain-leak">
          <div className="overview-domain-measures">
            {this.props.renderMeasureVariation('ncloc')}
          </div>

          {this.renderTimeline('after')}
        </div>
    );
  }

  renderLanguageDistribution () {
    const { measures } = this.props;
    const distribution =
        measures.find(measure => measure.metric.key === 'ncloc_language_distribution');

    if (!distribution) {
      return null;
    }

    return (
        <div className="overview-domain-measure">
          <div style={{ width: 200 }}>
            <LanguageDistribution distribution={distribution.value}/>
          </div>
        </div>
    );
  }

  renderNutshell () {
    return (
        <div className="overview-domain-nutshell">
          <div className="overview-domain-measures">
            {this.renderLanguageDistribution()}
            {this.props.renderMeasure('ncloc')}
          </div>

          {this.renderTimeline('before')}
        </div>
    );
  }

  render () {
    const { measures } = this.props;
    const linesOfCode =
        measures.find(measure => measure.metric.key === 'ncloc');

    if (!linesOfCode) {
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

export default enhance(Size);
