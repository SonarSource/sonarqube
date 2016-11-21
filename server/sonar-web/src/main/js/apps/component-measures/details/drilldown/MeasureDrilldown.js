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

import IconList from './../../components/IconList';
import IconTree from './../../components/IconTree';
import IconBubbles from './../../components/IconBubbles';
import IconTreemap from './../../components/IconTreemap';
import IconHistory from './../../components/IconHistory';

import { hasHistory, hasBubbleChart, hasTreemap } from '../../utils';
import { translate } from '../../../../helpers/l10n';

export default class MeasureDrilldown extends React.Component {
  render () {
    const { children, component, metric, ...other } = this.props;

    const child = React.cloneElement(children, { ...other });

    return (
        <div className="measure-details-drilldown">
          <ul className="measure-details-drilldown-mode">
            {component.qualifier !== 'DEV' && (
                <li>
                  <Link
                      activeClassName="active"
                      to={{ pathname: `/component_measures/metric/${metric.key}/list`, query: { id: component.key } }}>
                    <IconList/>
                    {translate('component_measures.tab.list')}
                  </Link>
                </li>
            )}

            <li>
              <Link
                  activeClassName="active"
                  to={{ pathname: `/component_measures/metric/${metric.key}/tree`, query: { id: component.key } }}>
                <IconTree/>
                {translate('component_measures.tab.tree')}
              </Link>
            </li>

            {hasBubbleChart(metric.key) && (
                <li>
                  <Link
                      activeClassName="active"
                      to={{
                        pathname: `/component_measures/metric/${metric.key}/bubbles`,
                        query: { id: component.key }
                      }}>
                    <IconBubbles/>
                    {translate('component_measures.tab.bubbles')}
                  </Link>
                </li>
            )}

            {hasTreemap(metric) && (
                <li>
                  <Link
                      activeClassName="active"
                      to={{
                        pathname: `/component_measures/metric/${metric.key}/treemap`,
                        query: { id: component.key }
                      }}>
                    <IconTreemap/>
                    {translate('component_measures.tab.treemap')}
                  </Link>
                </li>
            )}

            {hasHistory(metric.key) && (
                <li>
                  <Link
                      activeClassName="active"
                      to={{
                        pathname: `/component_measures/metric/${metric.key}/history`,
                        query: { id: component.key }
                      }}>
                    <IconHistory/>
                    {translate('component_measures.tab.history')}
                  </Link>
                </li>
            )}
          </ul>

          {child}
        </div>
    );
  }
}
