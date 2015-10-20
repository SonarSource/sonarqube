import _ from 'underscore';
import React from 'react';
import { BubbleChart } from '../../../components/charts/bubble-chart';
import { getProjectUrl } from '../../../helpers/Url';
import { getFiles } from '../../../api/components';


const X_METRIC = 'violations';
const Y_METRIC = 'sqale_index';
const SIZE_METRIC_1 = 'blocker_violations';
const SIZE_METRIC_2 = 'critical_violations';
const COMPONENTS_METRICS = [X_METRIC, Y_METRIC, SIZE_METRIC_1, SIZE_METRIC_2];
const HEIGHT = 360;


function formatIssues (d) {
  return window.formatMeasure(d, 'SHORT_INT');
}

function formatDebt (d) {
  return window.formatMeasure(d, 'SHORT_WORK_DUR');
}

function getMeasure (component, metric) {
  return component.measures[metric] || 0;
}


export class IssuesBubbleChart extends React.Component {
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
        size: getMeasure(component, SIZE_METRIC_1) + getMeasure(component, SIZE_METRIC_2),
        link: getProjectUrl(component.key)
      };
    });
    let xGrid = this.state.files.map(component => component.measures[X_METRIC]);
    let tooltips = this.state.files.map(component => {
      let inner = [
        component.name,
        `Issues: ${formatIssues(getMeasure(component, X_METRIC))}`,
        `Technical Debt: ${formatDebt(getMeasure(component, Y_METRIC))}`,
        `Blocker & Critical Issues: ${formatIssues(getMeasure(component, SIZE_METRIC_1) + getMeasure(component, SIZE_METRIC_2))}`
      ].join('<br>');
      return `<div class="text-left">${inner}</div>`;
    });
    return <BubbleChart items={items}
                        xGrid={xGrid}
                        tooltips={tooltips}
                        height={HEIGHT}
                        padding={[25, 30, 50, 60]}
                        formatXTick={formatIssues}
                        formatYTick={formatDebt}/>;
  }

  render () {
    return <div className="overview-bubble-chart overview-domain-dark">
      <div className="overview-domain-header">
        <h2 className="overview-title">Project Files</h2>
        <ul className="list-inline small">
          <li>X: Issues</li>
          <li>Y: Technical Debt</li>
          <li>Size: Blocker & Critical Issues</li>
        </ul>
      </div>
      <div>
        {this.renderBubbleChart()}
      </div>
    </div>;
  }
}
