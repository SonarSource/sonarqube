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
import _ from 'underscore';
import React from 'react';

import { Histogram } from '../../../components/charts/histogram';
import { formatMeasure } from '../../../helpers/measures';
import { getLanguages } from '../../../api/languages';
import { translate } from '../../../helpers/l10n';

export const LanguageDistribution = React.createClass({
  propTypes: {
    distribution: React.PropTypes.string.isRequired,
    lines: React.PropTypes.number.isRequired
  },

  componentDidMount () {
    this.requestLanguages();
  },

  requestLanguages () {
    getLanguages().then(languages => this.setState({ languages }));
  },

  getLanguageName (langKey) {
    if (this.state && this.state.languages) {
      const lang = _.findWhere(this.state.languages, { key: langKey });
      return lang ? lang.name : translate('unknown');
    } else {
      return langKey;
    }
  },

  cutLanguageName (name) {
    return name.length > 10 ? `${name.substr(0, 7)}...` : name;
  },

  renderBarChart () {
    let data = this.props.distribution.split(';').map((point, index) => {
      const tokens = point.split('=');
      return { x: parseInt(tokens[1], 10), y: index, value: tokens[0] };
    });

    data = _.sortBy(data, d => -d.x);

    const yTicks = data.map(point => this.getLanguageName(point.value)).map(this.cutLanguageName);
    const yValues = data.map(point => {
      const percent = point.x / this.props.lines * 100;
      return percent >= 0.1 ? formatMeasure(percent, 'PERCENT') : '<0.1%';
    });

    return <Histogram data={data}
                      yTicks={yTicks}
                      yValues={yValues}
                      height={data.length * 25}
                      barsWidth={10}
                      padding={[0, 60, 0, 80]}/>;
  },

  render () {
    const count = this.props.distribution.split(';').length;
    const height = count * 25;

    return <div className="overview-bar-chart" style={{ height }}>
      {this.renderBarChart()}
    </div>;
  }
});
