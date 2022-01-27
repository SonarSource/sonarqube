/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { ComponentMeasure, Metric } from '../../../types/types';
import Component from './Component';
import ComponentsEmpty from './ComponentsEmpty';
import ComponentsHeader from './ComponentsHeader';

interface Props {
  baseComponent?: ComponentMeasure;
  branchLike?: BranchLike;
  components: ComponentMeasure[];
  metrics: Metric[];
  rootComponent: ComponentMeasure;
  selected?: ComponentMeasure;
}

const BASE_COLUMN_COUNT = 4;

export class Components extends React.PureComponent<Props> {
  render() {
    const { baseComponent, branchLike, components, rootComponent, selected, metrics } = this.props;

    const colSpan = metrics.length + BASE_COLUMN_COUNT;
    const canBePinned = baseComponent && !['APP', 'VW', 'SVW'].includes(baseComponent.qualifier);

    return (
      <table className="data boxed-padding zebra">
        {baseComponent && (
          <ComponentsHeader
            baseComponent={baseComponent}
            canBePinned={canBePinned}
            metrics={metrics.map(metric => metric.key)}
            rootComponent={rootComponent}
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
              />
              <tr className="blank">
                <td colSpan={3}>
                  <hr className="null-spacer-top" />
                </td>
                <td colSpan={colSpan}>
                  <hr className="null-spacer-top" />
                </td>
              </tr>
            </>
          )}

          {components.length ? (
            sortBy(
              components,
              c => c.qualifier,
              c => c.name.toLowerCase(),
              c => (c.branch ? c.branch.toLowerCase() : '')
            ).map((component, index, list) => (
              <Component
                branchLike={branchLike}
                canBePinned={canBePinned}
                canBrowse={true}
                component={component}
                hasBaseComponent={baseComponent !== undefined}
                key={getComponentMeasureUniqueKey(component)}
                metrics={this.props.metrics}
                previous={index > 0 ? list[index - 1] : undefined}
                rootComponent={rootComponent}
                selected={
                  selected &&
                  getComponentMeasureUniqueKey(component) === getComponentMeasureUniqueKey(selected)
                }
              />
            ))
          ) : (
            <ComponentsEmpty canBePinned={canBePinned} />
          )}

          <tr className="blank">
            <td colSpan={3} />
            <td colSpan={colSpan} />
          </tr>
        </tbody>
      </table>
    );
  }
}

export default withKeyboardNavigation(Components);
