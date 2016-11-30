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
import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import ReactDOM from 'react-dom';
import { STATUSES } from '../../../../apps/background-tasks/constants';
import { getTasksForComponent } from '../../../../api/ce';
import ComponentNavFavorite from './component-nav-favorite';
import ComponentNavBreadcrumbs from './component-nav-breadcrumbs';
import ComponentNavMeta from './component-nav-meta';
import ComponentNavMenu from './component-nav-menu';
import RecentHistory from './RecentHistory';
import './ComponentNav.css';

export default React.createClass({
  componentDidMount() {
    this.loadStatus();
    this.populateRecentHistory();
  },

  loadStatus() {
    getTasksForComponent(this.props.component.uuid).then(r => {
      this.setState({
        isPending: !!_.findWhere(r.queue, { status: STATUSES.PENDING }),
        isInProgress: !!_.findWhere(r.queue, { status: STATUSES.IN_PROGRESS }),
        isFailed: r.current && r.current.status === STATUSES.FAILED
      }, this.initTooltips);
    });
  },

  populateRecentHistory() {
    const qualifier = _.last(this.props.component.breadcrumbs).qualifier;
    if (['TRK', 'VW', 'DEV'].indexOf(qualifier) !== -1) {
      RecentHistory.add(this.props.component.key, this.props.component.name, qualifier.toLowerCase());
    }
  },

  initTooltips() {
    $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this)).tooltip({
      container: 'body',
      placement: 'bottom',
      delay: { show: 0, hide: 2000 },
      html: true
    });
  },

  render() {
    return (
        <nav className="navbar navbar-context page-container" id="context-navigation">
          <div className="navbar-context-inner">
            <div className="container">
              <ComponentNavFavorite
                  component={this.props.component.key}
                  favorite={this.props.component.isFavorite}
                  canBeFavorite={this.props.component.canBeFavorite}/>

              <ComponentNavBreadcrumbs
                  breadcrumbs={this.props.component.breadcrumbs}/>

              <ComponentNavMeta
                  {...this.props}
                  {...this.state}
                  version={this.props.component.version}
                  snapshotDate={this.props.component.snapshotDate}/>

              <ComponentNavMenu
                  component={this.props.component}
                  conf={this.props.conf}/>
            </div>
          </div>
        </nav>
    );
  }
});
