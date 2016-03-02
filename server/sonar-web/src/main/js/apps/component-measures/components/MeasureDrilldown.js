/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import { Link } from 'react-router';

import ListIcon from './ListIcon';
import TreeIcon from './TreeIcon';
import { translate } from '../../../helpers/l10n';

export default class MeasureDrilldown extends React.Component {
  render () {
    const { metric, children } = this.props;
    const { component } = this.context;

    const child = React.cloneElement(children, {
      component,
      metric
    });

    return (
        <div className="measure-details-drilldown">
          <ul className="measure-details-mode">
            <li>
              <Link
                  activeClassName="active"
                  to={{ pathname: `${metric.key}/tree`, query: { id: component.key } }}>
                <TreeIcon/>
                {translate('component_measures.tab.tree')}
              </Link>
            </li>
            <li>
              <Link
                  activeClassName="active"
                  to={{ pathname: `${metric.key}/list`, query: { id: component.key } }}>
                <ListIcon/>
                {translate('component_measures.tab.list')}
              </Link>
            </li>
          </ul>

          {child}
        </div>
    );
  }
}

MeasureDrilldown.contextTypes = {
  component: React.PropTypes.object
};
