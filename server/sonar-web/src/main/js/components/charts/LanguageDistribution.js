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
import { find, sortBy } from 'lodash';
import PropTypes from 'prop-types';
import React from 'react';
import { Histogram } from './histogram';
import { formatMeasure } from '../../helpers/measures';
import { getLanguages } from '../../api/languages';
import { translate } from '../../helpers/l10n';

export default class LanguageDistribution extends React.PureComponent {
  static propTypes = {
    alignTicks: PropTypes.bool,
    distribution: PropTypes.string.isRequired
  };

  state = {};

  componentDidMount() {
    this.mounted = true;
    this.requestLanguages();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  requestLanguages() {
    getLanguages().then(languages => {
      if (this.mounted) {
        this.setState({ languages });
      }
    });
  }

  getLanguageName(langKey) {
    if (this.state.languages) {
      const lang = find(this.state.languages, { key: langKey });
      return lang ? lang.name : translate('unknown');
    } else {
      return langKey;
    }
  }

  cutLanguageName(name) {
    return name.length > 10 ? `${name.substr(0, 7)}...` : name;
  }

  render() {
    let data = this.props.distribution.split(';').map((point, index) => {
      const tokens = point.split('=');
      return { x: parseInt(tokens[1], 10), y: index, value: tokens[0] };
    });

    data = sortBy(data, d => -d.x);

    const yTicks = data.map(point => this.getLanguageName(point.value)).map(this.cutLanguageName);
    const yValues = data.map(point => formatMeasure(point.x, 'SHORT_INT'));

    return (
      <Histogram
        alignTicks={this.props.alignTicks}
        data={data}
        yTicks={yTicks}
        yValues={yValues}
        barsWidth={10}
        height={data.length * 25}
        padding={[0, 60, 0, 80]}
      />
    );
  }
}
