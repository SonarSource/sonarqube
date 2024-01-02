/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { isEqual, uniqBy } from 'lodash';
import * as React from 'react';
import DeferredSpinner from '../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { GraphType, MeasureHistory, ParsedAnalysis, Serie } from '../../types/project-activity';
import GraphHistory from './GraphHistory';
import './styles.css';
import { getSeriesMetricType, hasHistoryData, isCustomGraph } from './utils';

interface Props {
  analyses: ParsedAnalysis[];
  ariaLabel?: string;
  canShowDataAsTable?: boolean;
  graph: GraphType;
  graphs: Serie[][];
  graphEndDate?: Date;
  graphStartDate?: Date;
  leakPeriodDate?: Date;
  loading: boolean;
  measuresHistory: MeasureHistory[];
  removeCustomMetric?: (metric: string) => void;
  selectedDate?: Date;
  series: Serie[];
  updateGraphZoom?: (from?: Date, to?: Date) => void;
  updateSelectedDate?: (selectedDate?: Date) => void;
}

interface State {
  selectedDate?: Date;
}

export default class GraphsHistory extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      selectedDate: props.selectedDate,
    };
  }

  componentDidUpdate(prevProps: Props) {
    if (!isEqual(prevProps.selectedDate, this.props.selectedDate)) {
      this.setState({ selectedDate: this.props.selectedDate });
    }
  }

  updateTooltip = (selectedDate?: Date) => {
    this.setState({ selectedDate });
  };

  render() {
    const { analyses, graph, loading, series, ariaLabel, canShowDataAsTable } = this.props;
    const isCustom = isCustomGraph(graph);

    if (loading) {
      return (
        <div className="activity-graph-container flex-grow display-flex-column display-flex-stretch display-flex-justify-center">
          <div className="text-center">
            <DeferredSpinner ariaLabel={translate('loading')} loading={loading} />
          </div>
        </div>
      );
    }

    if (!hasHistoryData(series)) {
      return (
        <div className="activity-graph-container flex-grow display-flex-column display-flex-stretch display-flex-justify-center">
          <div className="display-flex-center display-flex-justify-center">
            <img
              alt="" /* Make screen readers ignore this image; it's purely eye candy. */
              className="spacer-right"
              height={52}
              src={`${getBaseUrl()}/images/activity-chart.svg`}
            />
            <div className="big-spacer-left big text-muted" style={{ maxWidth: 300 }}>
              {translate(
                isCustom
                  ? 'project_activity.graphs.custom.no_history'
                  : 'component_measures.no_history'
              )}
            </div>
          </div>
        </div>
      );
    }
    const showAreas = [GraphType.coverage, GraphType.duplications].includes(graph);
    return (
      <div className="display-flex-justify-center display-flex-column display-flex-stretch flex-grow">
        {this.props.graphs.map((graphSeries, idx) => {
          return (
            <GraphHistory
              analyses={analyses}
              canShowDataAsTable={canShowDataAsTable}
              graph={graph}
              graphEndDate={this.props.graphEndDate}
              graphStartDate={this.props.graphStartDate}
              isCustom={isCustom}
              key={idx}
              leakPeriodDate={this.props.leakPeriodDate}
              measuresHistory={this.props.measuresHistory}
              metricsType={getSeriesMetricType(graphSeries)}
              removeCustomMetric={this.props.removeCustomMetric}
              selectedDate={this.state.selectedDate}
              series={graphSeries}
              graphDescription={
                ariaLabel ||
                translateWithParameters(
                  'project_activity.graphs.explanation_x',
                  uniqBy(graphSeries, 'name')
                    .map(({ translatedName }) => translatedName)
                    .join(', ')
                )
              }
              showAreas={showAreas}
              updateGraphZoom={this.props.updateGraphZoom}
              updateSelectedDate={this.props.updateSelectedDate}
              updateTooltip={this.updateTooltip}
            />
          );
        })}
      </div>
    );
  }
}
