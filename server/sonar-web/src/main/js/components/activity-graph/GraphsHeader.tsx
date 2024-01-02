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
import classNames from 'classnames';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { GraphType } from '../../types/project-activity';
import { Metric } from '../../types/types';
import Select from '../controls/Select';
import AddGraphMetric from './AddGraphMetric';
import './styles.css';
import { getGraphTypes, isCustomGraph } from './utils';

interface Props {
  addCustomMetric?: (metric: string) => void;
  className?: string;
  removeCustomMetric?: (metric: string) => void;
  graph: GraphType;
  metrics: Metric[];
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
      selectedMetrics = [],
    } = this.props;

    const types = getGraphTypes(addCustomMetric === undefined || removeCustomMetric === undefined);

    const selectOptions = types.map((type) => ({
      label: translate('project_activity.graphs', type),
      value: type,
    }));

    return (
      <div className={classNames(className, 'position-relative')}>
        <div className="display-flex-end">
          <div className="display-flex-column">
            <label className="text-bold little-spacer-bottom" id="graph-select-label">
              {translate('project_activity.graphs.choose_type')}
            </label>
            <Select
              aria-labelledby="graph-select-label"
              className="input-medium"
              isSearchable={false}
              onChange={this.handleGraphChange}
              options={selectOptions}
              value={selectOptions.find((option) => option.value === graph)}
            />
          </div>
          {isCustomGraph(graph) &&
            addCustomMetric !== undefined &&
            removeCustomMetric !== undefined && (
              <AddGraphMetric
                addMetric={addCustomMetric}
                metrics={metrics}
                metricsTypeFilter={metricsTypeFilter}
                removeMetric={removeCustomMetric}
                selectedMetrics={selectedMetrics}
              />
            )}
        </div>
      </div>
    );
  }
}
