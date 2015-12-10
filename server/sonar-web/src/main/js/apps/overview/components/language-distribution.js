import _ from 'underscore';
import React from 'react';

import { Histogram } from '../../../components/charts/histogram';
import { formatMeasure } from '../../../helpers/measures';
import { getLanguages } from '../../../api/languages';


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
      let lang = _.findWhere(this.state.languages, { key: langKey });
      return lang ? lang.name : window.t('unknown');
    } else {
      return langKey;
    }
  },

  cutLanguageName (name) {
    return name.length > 10 ? `${name.substr(0, 7)}...` : name;
  },

  renderBarChart () {
    let data = this.props.distribution.split(';').map((point, index) => {
      let tokens = point.split('=');
      return { x: parseInt(tokens[1], 10), y: index, value: tokens[0] };
    });

    data = _.sortBy(data, d => -d.x);

    let yTicks = data.map(point => this.getLanguageName(point.value)).map(this.cutLanguageName);

    let yValues = data.map(point => formatMeasure(point.x / this.props.lines * 100, 'PERCENT'));

    return <Histogram data={data}
                      yTicks={yTicks}
                      yValues={yValues}
                      height={data.length * 25}
                      barsWidth={10}
                      padding={[0, 60, 0, 80]}/>;
  },

  render () {
    return <div className="overview-bar-chart">
      {this.renderBarChart()}
    </div>;
  }
});
