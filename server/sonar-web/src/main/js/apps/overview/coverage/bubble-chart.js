import _ from 'underscore';
import React from 'react';
import { BubbleChart } from '../../../components/charts/bubble-chart';
import { getProjectUrl } from '../../../helpers/Url';
import { getFiles } from '../../../api/components';
import { formatMeasure } from '../formatting';


const X_METRIC = 'complexity';
const Y_METRIC = 'coverage';
const SIZE_METRIC = 'sqale_index';
const COMPONENTS_METRICS = [X_METRIC, Y_METRIC, SIZE_METRIC];
const HEIGHT = 360;


function formatInt (d) {
  return window.formatMeasure(d, 'SHORT_INT');
}

function formatPercent (d) {
  return window.formatMeasure(d, 'PERCENT');
}

function getMeasure (component, metric) {
  return component.measures[metric] || 0;
}


export class CoverageBubbleChart extends React.Component {
  constructor () {
    super();
    this.state = { loading: true, files: [] };
  }

  componentDidMount () {
    this.requestFiles();
  }

  requestFiles () {
    return getFiles(this.props.component.key, COMPONENTS_METRICS).then(r => {
      let files = r.map(file => {
        let measures = {};
        (file.msr || []).forEach(measure => {
          measures[measure.key] = measure.val;
        });
        return _.extend(file, { measures });
      });
      this.setState({ loading: false, files });
    });
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderBubbleChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let items = this.state.files.map(component => {
      return {
        x: getMeasure(component, X_METRIC),
        y: getMeasure(component, Y_METRIC),
        size: getMeasure(component, SIZE_METRIC),
        link: getProjectUrl(component.key)
      };
    });
    let xGrid = this.state.files.map(component => component.measures[X_METRIC]);
    let tooltips = this.state.files.map(component => {
      let inner = [
        component.name,
        `Complexity: ${formatMeasure(getMeasure(component, X_METRIC), X_METRIC)}`,
        `Coverage: ${formatMeasure(getMeasure(component, Y_METRIC), Y_METRIC)}`,
        `Technical Debt: ${formatMeasure(getMeasure(component, SIZE_METRIC), SIZE_METRIC)}`
      ].join('<br>');
      return `<div class="text-left">${inner}</div>`;
    });
    return <BubbleChart items={items}
                        xGrid={xGrid}
                        tooltips={tooltips}
                        height={HEIGHT}
                        padding={[25, 30, 50, 60]}
                        formatXTick={formatInt}
                        formatYTick={formatPercent}/>;
  }

  render () {
    return <div className="overview-bubble-chart overview-domain-dark">
      <div className="overview-domain-header">
        <h2 className="overview-title">Project Files</h2>
        <ul className="list-inline small">
          <li>X: Complexity</li>
          <li>Y: Coverage</li>
          <li>Size: Technical Debt</li>
        </ul>
      </div>
      <div>
        {this.renderBubbleChart()}
      </div>
    </div>;
  }
}
