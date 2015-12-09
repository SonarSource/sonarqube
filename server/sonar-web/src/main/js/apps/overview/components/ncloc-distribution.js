import _ from 'underscore';
import React from 'react';

import { Histogram } from '../../../components/charts/histogram';
import { formatMeasure } from '../../../helpers/measures';
import { collapsePath } from '../../../helpers/path';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { getChildren } from '../../../api/components';


const HEIGHT = 302;
const METRIC = 'ncloc';


export const NclocDistribution = React.createClass({
  propTypes: {
    component: React.PropTypes.object.isRequired
  },

  getInitialState() {
    return { loading: true, files: [] };
  },

  componentDidMount () {
    this.requestComponents();
  },

  requestComponents () {
    let metrics = [METRIC];
    return getChildren(this.props.component.key, metrics).then(r => {
      let components = r.map(component => {
        let measures = {};
        (component.msr || []).forEach(measure => {
          measures[measure.key] = measure.val;
        });
        return _.extend(component, { measures });
      });
      this.setState({ loading: false, components });
    });
  },

  handleBarClick(d) {
    window.location = getComponentDrilldownUrl(d.component.key, 'ncloc');
  },

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  },

  renderBarChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let data = this.state.components.map((component, index) => {
      return {
        x: parseInt(component.measures[METRIC], 10),
        y: index,
        value: component.name,
        component: component
      };
    });

    data = _.sortBy(data, d => -d.x);

    let yTicks = data.map(d => {
      return {
        label: collapsePath(d.value, 20),
        tooltip: d.value
      };
    });

    let yValues = data.map(d => formatMeasure(d.x, 'SHORT_INT'));

    return <Histogram data={data}
                      yTicks={yTicks}
                      yValues={yValues}
                      height={data.length * 25}
                      barsWidth={10}
                      onBarClick={this.handleBarClick}
                      padding={[0, 50, 0, 240]}/>;
  },

  render () {
    return <div className="overview-domain-chart">
      <div className="overview-card-header">
        <h2 className="overview-title">{window.t('overview.chart.components')}</h2>
        <span className="small">
          {window.tp('overview.chart.legend.size_x', window.t('metric.ncloc.name'))}
        </span>
      </div>
      <div className="overview-bar-chart">
        {this.renderBarChart()}
      </div>
    </div>;
  }
});
