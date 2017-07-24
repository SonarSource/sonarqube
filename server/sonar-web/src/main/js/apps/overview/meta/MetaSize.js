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
import React from 'react';
import PropTypes from 'prop-types';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import LanguageDistribution from '../../../components/charts/LanguageDistribution';
import { formatMeasure } from '../../../helpers/measures';
import { getMetricName } from '../helpers/metrics';
import SizeRating from '../../../components/ui/SizeRating';

export default class MetaSize extends React.PureComponent {
  static propTypes = {
    component: PropTypes.object.isRequired,
    measures: PropTypes.array.isRequired
  };

  render() {
    const ncloc = this.props.measures.find(measure => measure.metric.key === 'ncloc');
    const languageDistribution = this.props.measures.find(
      measure => measure.metric.key === 'ncloc_language_distribution'
    );

    if (ncloc == null || languageDistribution == null) {
      return null;
    }

    return (
      <div id="overview-size" className="overview-meta-card">
        <div id="overview-ncloc" className="overview-meta-size-ncloc">
          <span className="spacer-right">
            <SizeRating value={ncloc.value} />
          </span>
          <DrilldownLink component={this.props.component.key} metric="ncloc">
            {formatMeasure(ncloc.value, 'SHORT_INT')}
          </DrilldownLink>
          <div className="overview-domain-measure-label text-muted">
            {getMetricName('ncloc')}
          </div>
        </div>
        <div id="overview-language-distribution" className="overview-meta-size-lang-dist">
          <LanguageDistribution distribution={languageDistribution.value} />
        </div>
      </div>
    );
  }
}
