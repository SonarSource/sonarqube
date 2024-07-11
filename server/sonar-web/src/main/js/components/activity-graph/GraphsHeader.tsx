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

import { DropdownMenu } from '@sonarsource/echoes-react';
import { ButtonSecondary, ChevronDownIcon, TextMuted } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { GraphType } from '../../types/project-activity';
import { Metric } from '../../types/types';
import AddGraphMetric from './AddGraphMetric';
import { getGraphTypes, isCustomGraph } from './utils';

interface Props {
  className?: string;
  graph: GraphType;
  metrics: Metric[];
  metricsTypeFilter?: string[];
  onAddCustomMetric?: (metric: string) => void;
  onRemoveCustomMetric?: (metric: string) => void;
  onUpdateGraph: (graphType: string) => void;
  selectedMetrics?: string[];
}

export default function GraphsHeader(props: Props) {
  const {
    className,
    graph,
    metrics,
    metricsTypeFilter,
    onUpdateGraph,
    selectedMetrics = [],
  } = props;

  const handleGraphChange = React.useCallback(
    (value: GraphType) => {
      if (value !== graph) {
        onUpdateGraph(value);
      }
    },
    [graph, onUpdateGraph],
  );

  const noCustomGraph =
    props.onAddCustomMetric === undefined || props.onRemoveCustomMetric === undefined;

  const options = React.useMemo(() => {
    const types = getGraphTypes(noCustomGraph);

    return types.map((type) => {
      const label = translate('project_activity.graphs', type);

      return (
        <DropdownMenu.ItemButton key={label} onClick={() => handleGraphChange(type)}>
          {label}
        </DropdownMenu.ItemButton>
      );
    });
  }, [noCustomGraph, handleGraphChange]);

  return (
    <div className={className}>
      <div className="sw-flex">
        <DropdownMenu.Root id="activity-graph-type" align="start" items={options}>
          <ButtonSecondary
            aria-label={translate('project_activity.graphs.choose_type')}
            className={
              'sw-body-sm sw-flex sw-flex-row sw-justify-between sw-pl-3 sw-pr-2 sw-w-32 ' +
              'sw-z-normal' // needed because the legends overlap part of the button
            }
          >
            <TextMuted text={translate('project_activity.graphs', graph)} />
            <ChevronDownIcon className="sw-ml-1 sw-mr-0 sw-pr-0" />
          </ButtonSecondary>
        </DropdownMenu.Root>

        {isCustomGraph(graph) &&
          props.onAddCustomMetric !== undefined &&
          props.onRemoveCustomMetric !== undefined && (
            <AddGraphMetric
              onAddMetric={props.onAddCustomMetric}
              metrics={metrics}
              metricsTypeFilter={metricsTypeFilter}
              onRemoveMetric={props.onRemoveCustomMetric}
              selectedMetrics={selectedMetrics}
            />
          )}
      </div>
    </div>
  );
}
