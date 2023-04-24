/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import {
  ButtonSecondary,
  ChevronDownIcon,
  Dropdown,
  ItemButton,
  PopupPlacement,
  PopupZLevel,
  TextMuted,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { GraphType } from '../../types/project-activity';
import { Metric } from '../../types/types';
import Select from '../controls/Select';
import AddGraphMetric from './AddGraphMetric';
import './styles.css';
import { getGraphTypes, isCustomGraph } from './utils';

interface Props {
  onAddCustomMetric?: (metric: string) => void;
  className?: string;
  onRemoveCustomMetric?: (metric: string) => void;
  graph: GraphType;
  metrics: Metric[];
  metricsTypeFilter?: string[];
  selectedMetrics?: string[];
  onUpdateGraph: (graphType: string) => void;
}

export default class GraphsHeader extends React.PureComponent<Props> {
  handleGraphChange = (option: { value: string }) => {
    if (option.value !== this.props.graph) {
      this.props.onUpdateGraph(option.value);
    }
  };

  render() {
    const { className, graph, metrics, metricsTypeFilter, selectedMetrics = [] } = this.props;

    const noCustomGraph =
      this.props.onAddCustomMetric === undefined || this.props.onRemoveCustomMetric === undefined;

    const types = getGraphTypes(noCustomGraph);

    const overlayItems: JSX.Element[] = [];

    const selectOptions: Array<{
      label: string;
      value: GraphType;
    }> = [];

    types.forEach((type) => {
      const label = translate('project_activity.graphs', type);

      selectOptions.push({ label, value: type });

      overlayItems.push(
        <ItemButton key={label} onClick={() => this.handleGraphChange({ value: type })}>
          {label}
        </ItemButton>
      );
    });

    const selectedOption = selectOptions.find((option) => option.value === graph);
    const selectedLabel = selectedOption?.label ?? '';

    return (
      <div className={className}>
        <div className="display-flex-end">
          <div className="display-flex-column">
            {noCustomGraph ? (
              <Dropdown
                id="activity-graph-type"
                size="auto"
                placement={PopupPlacement.BottomLeft}
                zLevel={PopupZLevel.Content}
                overlay={overlayItems}
              >
                <ButtonSecondary
                  aria-label={translate('project_activity.graphs.choose_type')}
                  className={
                    'sw-body-sm sw-flex sw-flex-row sw-justify-between sw-pl-3 sw-pr-2 sw-w-32 ' +
                    'sw-z-normal' // needed because the legends overlap part of the button
                  }
                >
                  <TextMuted text={selectedLabel} />
                  <ChevronDownIcon className="sw-ml-1 sw-mr-0 sw-pr-0" />
                </ButtonSecondary>
              </Dropdown>
            ) : (
              <Select
                aria-label={translate('project_activity.graphs.choose_type')}
                className="input-medium"
                isSearchable={false}
                onChange={this.handleGraphChange}
                options={selectOptions}
                value={selectedOption}
              />
            )}
          </div>
          {isCustomGraph(graph) &&
            this.props.onAddCustomMetric !== undefined &&
            this.props.onRemoveCustomMetric !== undefined && (
              <AddGraphMetric
                onAddMetric={this.props.onAddCustomMetric}
                metrics={metrics}
                metricsTypeFilter={metricsTypeFilter}
                onRemoveMetric={this.props.onRemoveCustomMetric}
                selectedMetrics={selectedMetrics}
              />
            )}
        </div>
      </div>
    );
  }
}
