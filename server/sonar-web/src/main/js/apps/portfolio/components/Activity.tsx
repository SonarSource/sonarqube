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
import * as React from 'react';
import {
  getDisplayedHistoryMetrics,
  DEFAULT_GRAPH,
  getProjectActivityGraph
} from '../../projectActivity/utils';
import PreviewGraph from '../../../components/preview-graph/PreviewGraph';
import { getAllTimeMachineData } from '../../../api/time-machine';
import { parseDate } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';

interface Props {
  component: string;
  metrics: T.Dict<T.Metric>;
}

interface State {
  history?: {
    [metric: string]: Array<{ date: Date; value?: string }>;
  };
  loading: boolean;
}

export default class Activity extends React.PureComponent<Props> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchHistory();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component !== this.props.component) {
      this.fetchHistory();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchHistory = () => {
    const { component } = this.props;

    const { graph, customGraphs } = getProjectActivityGraph(component);
    let graphMetrics = getDisplayedHistoryMetrics(graph, customGraphs);
    if (!graphMetrics || graphMetrics.length <= 0) {
      graphMetrics = getDisplayedHistoryMetrics(DEFAULT_GRAPH, []);
    }

    this.setState({ loading: true });
    return getAllTimeMachineData({ component, metrics: graphMetrics.join() }).then(
      timeMachine => {
        if (this.mounted) {
          const history: T.Dict<Array<{ date: Date; value?: string }>> = {};
          timeMachine.measures.forEach(measure => {
            const measureHistory = measure.history.map(analysis => ({
              date: parseDate(analysis.date),
              value: analysis.value
            }));
            history[measure.metric] = measureHistory;
          });
          this.setState({ history, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  renderWhenEmpty = () => <div className="note">{translate('component_measures.no_history')}</div>;

  render() {
    return (
      <div className="big-spacer-bottom">
        <h4>{translate('project_activity.page')}</h4>

        {this.state.loading ? (
          <i className="spinner" />
        ) : (
          this.state.history !== undefined && (
            <PreviewGraph
              history={this.state.history}
              metrics={this.props.metrics}
              project={this.props.component}
              renderWhenEmpty={this.renderWhenEmpty}
            />
          )
        )}
      </div>
    );
  }
}
