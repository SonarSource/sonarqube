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
import Component from './Component';
import ComponentsEmpty from './ComponentsEmpty';
import ComponentsHeader from './ComponentsHeader';
import withKeyboardNavigation from '../../../components/hoc/withKeyboardNavigation';
import { getCodeMetrics } from '../utils';
import { isDefined } from '../../../helpers/types';

interface Props {
  baseComponent?: T.ComponentMeasure;
  branchLike?: T.BranchLike;
  components: T.ComponentMeasure[];
  metrics: T.Dict<T.Metric>;
  rootComponent: T.ComponentMeasure;
  selected?: T.ComponentMeasure;
}

export class Components extends React.PureComponent<Props> {
  render() {
    const { baseComponent, branchLike, components, rootComponent, selected } = this.props;
    const metricKeys = getCodeMetrics(rootComponent.qualifier, branchLike);
    const metrics = metricKeys.map(metric => this.props.metrics[metric]).filter(isDefined);
    return (
      <table className="data boxed-padding zebra">
        {baseComponent && (
          <ComponentsHeader
            baseComponent={baseComponent}
            metrics={metricKeys}
            rootComponent={rootComponent}
          />
        )}
        {baseComponent && (
          <tbody>
            <Component
              branchLike={branchLike}
              component={baseComponent}
              key={baseComponent.key}
              metrics={metrics}
              rootComponent={rootComponent}
            />
            <tr className="blank">
              <td colSpan={3}>&nbsp;</td>
              <td colSpan={10}>&nbsp;</td>
            </tr>
          </tbody>
        )}
        <tbody>
          {components.length ? (
            components.map((component, index, list) => (
              <Component
                branchLike={branchLike}
                canBrowse={true}
                component={component}
                key={component.key}
                metrics={metrics}
                previous={index > 0 ? list[index - 1] : undefined}
                rootComponent={rootComponent}
                selected={selected && component.key === selected.key}
              />
            ))
          ) : (
            <ComponentsEmpty />
          )}

          <tr className="blank">
            <td colSpan={3} />
            <td colSpan={10} />
          </tr>
        </tbody>
      </table>
    );
  }
}

export default withKeyboardNavigation(Components);
