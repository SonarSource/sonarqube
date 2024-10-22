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

import { times } from 'lodash';
import { ContentCell, NumericalCell, Table, TableRow, TableRowInteractive } from '~design-system';
import { getLocalizedMetricName } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { MeasurePageView } from '../../../types/measures';
import { ComponentMeasure, ComponentMeasureEnhanced, Dict, Metric } from '../../../types/types';
import { complementary } from '../config/complementary';
import ComponentCell from './ComponentCell';
import EmptyResult from './EmptyResult';
import MeasureCell from './MeasureCell';

interface Props {
  branchLike?: BranchLike;
  components: ComponentMeasureEnhanced[];
  metric: Metric;
  metrics: Dict<Metric>;
  rootComponent: ComponentMeasure;
  selectedComponent?: ComponentMeasureEnhanced;
  view: MeasurePageView;
}

export default function ComponentsList({ components, metric, metrics, ...props }: Props) {
  const { branchLike, rootComponent, selectedComponent } = props;

  if (!components.length) {
    return <EmptyResult />;
  }

  const otherMetrics = (complementary[metric.key] || []).map((key) => metrics[key]);
  return (
    <Table
      columnCount={otherMetrics.length + 2}
      columnWidths={['auto', ...times(otherMetrics.length + 1, () => '1%')]}
      header={
        otherMetrics.length > 0 && (
          <TableRow>
            <ContentCell />
            <NumericalCell className="sw-typo-default">
              {getLocalizedMetricName(metric)}
            </NumericalCell>
            {otherMetrics.map((metric) => (
              <NumericalCell className="sw-typo-default" key={metric.key}>
                {getLocalizedMetricName(metric)}
              </NumericalCell>
            ))}
          </TableRow>
        )
      }
    >
      {components.map((component) => (
        <TableRowInteractive
          key={component.key}
          className="it__measures-component-row"
          selected={component.key === selectedComponent?.key}
        >
          <ComponentCell
            branchLike={branchLike}
            component={component}
            metric={metric}
            rootComponent={rootComponent}
            view={props.view}
          />

          <MeasureCell branchLike={branchLike} component={component} metric={metric} />

          {otherMetrics.map((metric) => (
            <MeasureCell
              branchLike={branchLike}
              key={metric.key}
              component={component}
              measure={component.measures.find((measure) => measure.metric.key === metric.key)}
              metric={metric}
            />
          ))}
        </TableRowInteractive>
      ))}
    </Table>
  );
}
