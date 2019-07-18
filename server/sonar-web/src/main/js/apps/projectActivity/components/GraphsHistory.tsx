/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { isEqual } from 'lodash';
import * as React from 'react';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  getSeriesMetricType,
  hasHistoryData,
  isCustomGraph,
  MeasureHistory,
  ParsedAnalysis,
  Serie
} from '../utils';
import GraphHistory from './GraphHistory';

interface Props {
  analyses: ParsedAnalysis[];
  eventFilter: string;
  graph: string;
  graphs: Serie[][];
  graphEndDate?: Date;
  graphStartDate?: Date;
  leakPeriodDate?: Date;
  loading: boolean;
  measuresHistory: MeasureHistory[];
  removeCustomMetric: (metric: string) => void;
  selectedDate?: Date;
  series: Serie[];
  updateGraphZoom: (from?: Date, to?: Date) => void;
  updateSelectedDate: (selectedDate?: Date) => void;
}

interface State {
  selectedDate?: Date;
}

export default class GraphsHistory extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      selectedDate: props.selectedDate
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (!isEqual(nextProps.selectedDate, this.props.selectedDate)) {
      this.setState({ selectedDate: nextProps.selectedDate });
    }
  }

  getSelectedDateEvents = () => {
    const { selectedDate } = this.state;
    const { analyses } = this.props;
    if (analyses && selectedDate) {
      const analysis = analyses.find(
        analysis => analysis.date.valueOf() === selectedDate.valueOf()
      );
      if (analysis) {
        return analysis.events;
      }
    }
    return [];
  };

  updateTooltip = (selectedDate?: Date) => {
    this.setState({ selectedDate });
  };

  render() {
    const { graph, loading, series } = this.props;
    const isCustom = isCustomGraph(graph);

    if (loading) {
      return (
        <div className="project-activity-graph-container">
          <div className="text-center">
            <DeferredSpinner className="" loading={loading} />
          </div>
        </div>
      );
    }

    if (!hasHistoryData(series)) {
      return (
        <div className="project-activity-graph-container">
          <div className="note text-center">
            {translate(
              isCustom
                ? 'project_activity.graphs.custom.no_history'
                : 'component_measures.no_history'
            )}
          </div>
        </div>
      );
    }
    const events = this.getSelectedDateEvents();
    const showAreas = ['coverage', 'duplications'].includes(graph);
    return (
      <div className="project-activity-graphs">
        {this.props.graphs.map((series, idx) => (
          <GraphHistory
            events={events}
            graph={graph}
            graphEndDate={this.props.graphEndDate}
            graphStartDate={this.props.graphStartDate}
            isCustom={isCustom}
            key={idx}
            leakPeriodDate={this.props.leakPeriodDate}
            measuresHistory={this.props.measuresHistory}
            metricsType={getSeriesMetricType(series)}
            removeCustomMetric={this.props.removeCustomMetric}
            selectedDate={this.state.selectedDate}
            series={series}
            showAreas={showAreas}
            updateGraphZoom={this.props.updateGraphZoom}
            updateSelectedDate={this.props.updateSelectedDate}
            updateTooltip={this.updateTooltip}
          />
        ))}
      </div>
    );
  }
}
