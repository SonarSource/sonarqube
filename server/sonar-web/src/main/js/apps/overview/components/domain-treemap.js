import _ from 'underscore';
import React from 'react';

import { Treemap } from '../../../components/charts/treemap';
import { getChildren } from '../../../api/components';
import { formatMeasure } from '../../../helpers/measures';


const HEIGHT = 302;


export class DomainTreemap extends React.Component {
  constructor (props) {
    super(props);
    this.state = {
      loading: true,
      files: [],
      sizeMetric: this.getMetricObject(props.metrics, props.sizeMetric),
      colorMetric: props.colorMetric ? this.getMetricObject(props.metrics, props.colorMetric) : null
    };
  }

  componentDidMount () {
    this.requestComponents();
  }

  requestComponents () {
    let metrics = [this.props.sizeMetric, this.props.colorMetric];
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
  }

  getMetricObject (metrics, metricKey) {
    return _.findWhere(metrics, { key: metricKey });
  }

  getTooltip (component) {
    let inner = [
      component.name,
      `${this.state.sizeMetric.name}: ${formatMeasure(component.measures[this.props.sizeMetric], this.state.sizeMetric.type)}`
    ];
    if (this.state.colorMetric) {
      inner.push(`${this.state.colorMetric.name}: ${formatMeasure(component.measures[this.props.colorMetric], this.state.colorMetric.type)}`);
    }
    inner = inner.join('<br>');
    return `<div class="text-left">${inner}</div>`;
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderTreemap () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    // TODO filter out zero sized components
    let items = this.state.components.map(component => {
      let colorMeasure = this.props.colorMetric ? component.measures[this.props.colorMetric] : null;
      return {
        size: component.measures[this.props.sizeMetric],
        color: colorMeasure != null ? this.props.scale(colorMeasure) : '#777',
        tooltip: this.getTooltip(component),
        label: component.name
      };
    });
    return <Treemap items={items} height={HEIGHT}/>;
  }

  render () {
    let color = this.props.colorMetric ? <li>Color: {this.state.colorMetric.name}</li> : null;
    return <div className="overview-domain-chart">
      <div className="overview-card-header">
        <h2 className="overview-title">Treemap</h2>
        <ul className="list-inline small">
          <li>Size: {this.state.sizeMetric.name}</li>
          {color}
        </ul>
      </div>
      <div className="overview-treemap">
        {this.renderTreemap()}
      </div>
    </div>;
  }
}

DomainTreemap.propTypes = {
  sizeMetric: React.PropTypes.string.isRequired,
  colorMetric: React.PropTypes.string,
  scale: React.PropTypes.func
};
