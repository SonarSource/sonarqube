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
import * as classNames from 'classnames';
import Component from './Component';
import ComponentsEmpty from './ComponentsEmpty';
import ComponentsHeader from './ComponentsHeader';
import { isDefined } from '../../../helpers/types';
import { getCodeMetrics, showLeakMeasure } from '../utils';

interface Props {
  baseComponent?: T.ComponentMeasure;
  branchLike?: T.BranchLike;
  components: T.ComponentMeasure[];
  metrics: { [metric: string]: T.Metric };
  rootComponent: T.ComponentMeasure;
  selected?: T.ComponentMeasure;
}

export default function Components(props: Props) {
  const { baseComponent, branchLike, components, rootComponent, selected } = props;
  const metricKeys = getCodeMetrics(rootComponent.qualifier, branchLike);
  const metrics = metricKeys.map(metric => props.metrics[metric]).filter(isDefined);
  const isLeak = Boolean(baseComponent && showLeakMeasure(branchLike));
  return (
    <table className="data boxed-padding zebra">
      {baseComponent && (
        <ComponentsHeader
          baseComponent={baseComponent}
          isLeak={isLeak}
          metrics={metricKeys}
          rootComponent={rootComponent}
        />
      )}
      {baseComponent && (
        <tbody>
          <Component
            branchLike={branchLike}
            component={baseComponent}
            isLeak={isLeak}
            key={baseComponent.key}
            metrics={metrics}
            rootComponent={rootComponent}
          />
          <tr className="blank">
            <td colSpan={3}>&nbsp;</td>
            <td className={classNames({ leak: isLeak })} colSpan={10}>
              {' '}
              &nbsp;{' '}
            </td>
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
              isLeak={isLeak}
              key={component.key}
              metrics={metrics}
              previous={index > 0 ? list[index - 1] : undefined}
              rootComponent={rootComponent}
              selected={component === selected}
            />
          ))
        ) : (
          <ComponentsEmpty isLeak={isLeak} />
        )}

        <tr className="blank">
          <td colSpan={3} />
          <td className={classNames({ leak: isLeak })} colSpan={10} />
        </tr>
      </tbody>
    </table>
  );
}
