/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import Select from 'sonar-ui-common/components/controls/Select';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { GraphType } from '../../types/project-activity';
import AddGraphMetric from './AddGraphMetric';
import './styles.css';
import { getGraphTypes, isCustomGraph } from './utils';

interface Props {
  addCustomMetric?: (metric: string) => void;
  className?: string;
  removeCustomMetric?: (metric: string) => void;
  graph: GraphType;
  metrics: T.Metric[];
  metricsTypeFilter?: string[];
  selectedMetrics?: string[];
  updateGraph: (graphType: string) => void;
}

export default class GraphsHeader extends React.PureComponent<Props> {
  handleGraphChange = (option: { value: string }) => {
    if (option.value !== this.props.graph) {
      this.props.updateGraph(option.value);
    }
  };

  render() {
    const {
      addCustomMetric,
      className,
      graph,
      metrics,
      metricsTypeFilter,
      removeCustomMetric,
      selectedMetrics = []
    } = this.props;

    const types = getGraphTypes(addCustomMetric === undefined || removeCustomMetric === undefined);

    const selectOptions = types.map(type => ({
      label: translate('project_activity.graphs', type),
      value: type
    }));

    return (
      <div className={classNames(className, 'position-relative')}>
        <Select
          className="pull-left input-medium"
          clearable={false}
          onChange={this.handleGraphChange}
          options={selectOptions}
          searchable={false}
          value={graph}
        />
        {isCustomGraph(graph) &&
          addCustomMetric !== undefined &&
          removeCustomMetric !== undefined && (
            <AddGraphMetric
              addMetric={addCustomMetric}
              className="pull-left spacer-left"
              metrics={metrics}
              metricsTypeFilter={metricsTypeFilter}
              removeMetric={removeCustomMetric}
              selectedMetrics={selectedMetrics}
            />
          )}
      </div>
    );
  }
}
