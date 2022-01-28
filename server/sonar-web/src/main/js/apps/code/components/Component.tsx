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
import classNames from 'classnames';
import * as React from 'react';
import { withScrollTo } from '../../../components/hoc/withScrollTo';
import { WorkspaceContext } from '../../../components/workspace/context';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { MetricType } from '../../../types/metrics';
import { ComponentMeasure as TypeComponentMeasure, Metric } from '../../../types/types';
import ComponentMeasure from './ComponentMeasure';
import ComponentName from './ComponentName';
import ComponentPin from './ComponentPin';

interface Props {
  branchLike?: BranchLike;
  canBePinned?: boolean;
  canBrowse?: boolean;
  component: TypeComponentMeasure;
  hasBaseComponent: boolean;
  isBaseComponent?: boolean;
  metrics: Metric[];
  previous?: TypeComponentMeasure;
  rootComponent: TypeComponentMeasure;
  selected?: boolean;
}

export class Component extends React.PureComponent<Props> {
  render() {
    const {
      branchLike,
      canBePinned = true,
      canBrowse = false,
      component,
      hasBaseComponent,
      isBaseComponent = false,
      metrics,
      previous,
      rootComponent,
      selected = false
    } = this.props;

    const isFile =
      component.qualifier === ComponentQualifier.File ||
      component.qualifier === ComponentQualifier.TestFile;

    return (
      <tr className={classNames({ selected })}>
        <td className="blank" />
        {canBePinned && (
          <td className="thin nowrap">
            {isFile && (
              <span className="spacer-right">
                <WorkspaceContext.Consumer>
                  {({ openComponent }) => (
                    <ComponentPin
                      branchLike={branchLike}
                      component={component}
                      openComponent={openComponent}
                    />
                  )}
                </WorkspaceContext.Consumer>
              </span>
            )}
          </td>
        )}
        <td className="code-name-cell">
          <div className="display-flex-center">
            {hasBaseComponent && <div className="code-child-component-icon" />}
            <ComponentName
              branchLike={branchLike}
              canBrowse={canBrowse}
              component={component}
              previous={previous}
              rootComponent={rootComponent}
              unclickable={isBaseComponent}
            />
          </div>
        </td>

        {metrics.map(metric => (
          <td
            className={classNames('thin', {
              'text-center': metric.type === MetricType.Rating,
              'nowrap text-right': metric.type !== MetricType.Rating
            })}
            key={metric.key}>
            <div
              className={classNames({
                'code-components-rating-cell': metric.type === MetricType.Rating,
                'code-components-cell': metric.type !== MetricType.Rating
              })}>
              <ComponentMeasure component={component} metric={metric} />
            </div>
          </td>
        ))}
        <td className="blank" />
      </tr>
    );
  }
}

export default withScrollTo(Component);
