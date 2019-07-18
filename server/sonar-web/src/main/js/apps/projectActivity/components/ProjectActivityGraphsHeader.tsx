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
import Select from 'sonar-ui-common/components/controls/Select';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { GRAPH_TYPES, isCustomGraph } from '../utils';
import AddGraphMetric from './forms/AddGraphMetric';

interface Props {
  addCustomMetric: (metric: string) => void;
  removeCustomMetric: (metric: string) => void;
  graph: string;
  metrics: T.Metric[];
  metricsTypeFilter?: string[];
  selectedMetrics: string[];
  updateGraph: (graphType: string) => void;
}

export default class ProjectActivityGraphsHeader extends React.PureComponent<Props> {
  handleGraphChange = (option: { value: string }) => {
    if (option.value !== this.props.graph) {
      this.props.updateGraph(option.value);
    }
  };

  render() {
    const selectOptions = GRAPH_TYPES.map(graph => ({
      label: translate('project_activity.graphs', graph),
      value: graph
    }));

    return (
      <header className="page-header">
        <Select
          className="pull-left input-medium"
          clearable={false}
          onChange={this.handleGraphChange}
          options={selectOptions}
          searchable={false}
          value={this.props.graph}
        />
        {isCustomGraph(this.props.graph) && (
          <AddGraphMetric
            addMetric={this.props.addCustomMetric}
            className="pull-left spacer-left"
            metrics={this.props.metrics}
            metricsTypeFilter={this.props.metricsTypeFilter}
            removeMetric={this.props.removeCustomMetric}
            selectedMetrics={this.props.selectedMetrics}
          />
        )}
      </header>
    );
  }
}
