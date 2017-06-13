/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import ComponentNavFavorite from './ComponentNavFavorite';
import ComponentNavBreadcrumbs from './ComponentNavBreadcrumbs';
import ComponentNavMeta from './ComponentNavMeta';
import ComponentNavMenu from './ComponentNavMenu';
import RecentHistory from '../../RecentHistory';
import { TooltipsContainer } from '../../../../components/mixins/tooltips-mixin';
import { getTasksForComponent } from '../../../../api/ce';
import { STATUSES } from '../../../../apps/background-tasks/constants';

export default class ComponentNav extends React.PureComponent {
  componentDidMount() {
    this.mounted = true;

    this.loadStatus();
    this.populateRecentHistory();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadStatus = () => {
    getTasksForComponent(this.props.component.key).then(r => {
      if (this.mounted) {
        this.setState({
          isPending: r.queue.some(task => task.status === STATUSES.PENDING),
          isInProgress: r.queue.some(task => task.status === STATUSES.IN_PROGRESS),
          isFailed: r.current && r.current.status === STATUSES.FAILED
        });
      }
    });
  };

  populateRecentHistory = () => {
    const { breadcrumbs } = this.props.component;
    const { qualifier } = breadcrumbs[breadcrumbs.length - 1];
    if (['TRK', 'VW', 'DEV'].indexOf(qualifier) !== -1) {
      RecentHistory.add(
        this.props.component.key,
        this.props.component.name,
        qualifier.toLowerCase(),
        this.props.component.organization
      );
    }
  };

  render() {
    return (
      <nav className="navbar navbar-context page-container" id="context-navigation">
        <div className="navbar-context-inner">
          <div className="container">
            <ComponentNavFavorite
              component={this.props.component.key}
              favorite={this.props.component.isFavorite}
            />

            <ComponentNavBreadcrumbs
              component={this.props.component}
              breadcrumbs={this.props.component.breadcrumbs}
            />

            <TooltipsContainer options={{ delay: { show: 0, hide: 2000 } }}>
              <ComponentNavMeta
                {...this.props}
                {...this.state}
                version={this.props.component.version}
                analysisDate={this.props.component.analysisDate}
              />
            </TooltipsContainer>

            <ComponentNavMenu
              component={this.props.component}
              conf={this.props.conf}
              location={this.props.location}
            />
          </div>
        </div>
      </nav>
    );
  }
}
