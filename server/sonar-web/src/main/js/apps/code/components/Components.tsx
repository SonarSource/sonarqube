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
import { sortBy } from 'lodash';
import * as React from 'react';
import withKeyboardNavigation from '../../../components/hoc/withKeyboardNavigation';
import { getComponentMeasureUniqueKey } from '../../../helpers/component';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { ComponentMeasure, Metric } from '../../../types/types';
import Component from './Component';
import ComponentsEmpty from './ComponentsEmpty';
import ComponentsHeader from './ComponentsHeader';

interface ComponentsProps {
  baseComponent?: ComponentMeasure;
  branchLike?: BranchLike;
  components: ComponentMeasure[];
  metrics: Metric[];
  rootComponent: ComponentMeasure;
  selected?: ComponentMeasure;
  newCodeSelected?: boolean;
  showAnalysisDate?: boolean;
}

export function Components(props: ComponentsProps) {
  const {
    baseComponent,
    branchLike,
    components,
    rootComponent,
    selected,
    metrics,
    newCodeSelected,
    showAnalysisDate,
  } = props;

  const canBePinned =
    baseComponent &&
    ![
      ComponentQualifier.Application,
      ComponentQualifier.Portfolio,
      ComponentQualifier.SubPortfolio,
    ].includes(baseComponent.qualifier as ComponentQualifier);

  return (
    <div className="big-spacer-bottom table-wrapper">
      <table className="data zebra">
        {baseComponent && (
          <ComponentsHeader
            baseComponent={baseComponent}
            canBePinned={canBePinned}
            metrics={metrics.map((metric) => metric.key)}
            rootComponent={rootComponent}
            showAnalysisDate={showAnalysisDate}
          />
        )}
        <tbody>
          {baseComponent && (
            <>
              <Component
                branchLike={branchLike}
                canBePinned={canBePinned}
                component={baseComponent}
                hasBaseComponent={false}
                isBaseComponent={true}
                key={baseComponent.key}
                metrics={metrics}
                rootComponent={rootComponent}
                newCodeSelected={newCodeSelected}
                showAnalysisDate={showAnalysisDate}
              />
              <tr className="blank">
                <td
                  colSpan={metrics.length + 1 + (canBePinned ? 1 : 0) + (showAnalysisDate ? 1 : 0)}
                />
              </tr>
            </>
          )}

          {components.length ? (
            sortBy(
              components,
              (c) => c.qualifier,
              (c) => c.name.toLowerCase(),
              (c) => (c.branch ? c.branch.toLowerCase() : '')
            ).map((component, index, list) => (
              <Component
                branchLike={branchLike}
                canBePinned={canBePinned}
                canBrowse={true}
                component={component}
                hasBaseComponent={baseComponent !== undefined}
                key={getComponentMeasureUniqueKey(component)}
                metrics={metrics}
                previous={index > 0 ? list[index - 1] : undefined}
                rootComponent={rootComponent}
                newCodeSelected={newCodeSelected}
                showAnalysisDate={showAnalysisDate}
                selected={
                  selected &&
                  getComponentMeasureUniqueKey(component) === getComponentMeasureUniqueKey(selected)
                }
              />
            ))
          ) : (
            <ComponentsEmpty canBePinned={canBePinned} />
          )}
        </tbody>
      </table>
    </div>
  );
}

export default withKeyboardNavigation(Components);
