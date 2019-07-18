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
import ContextNavBar from 'sonar-ui-common/components/ui/ContextNavBar';
import { STATUSES } from '../../../../apps/background-tasks/constants';
import { rawSizes } from '../../../theme';
import RecentHistory from '../../RecentHistory';
import './ComponentNav.css';
import ComponentNavBgTaskNotif from './ComponentNavBgTaskNotif';
import ComponentNavHeader from './ComponentNavHeader';
import ComponentNavMenu from './ComponentNavMenu';
import ComponentNavMeta from './ComponentNavMeta';

interface Props {
  branchLikes: T.BranchLike[];
  currentBranchLike: T.BranchLike | undefined;
  component: T.Component;
  currentTask?: T.Task;
  currentTaskOnSameBranch?: boolean;
  isInProgress?: boolean;
  isPending?: boolean;
  location: {};
  warnings: string[];
}

export default class ComponentNav extends React.PureComponent<Props> {
  mounted = false;

  componentDidMount() {
    this.populateRecentHistory();
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.component.key !== prevProps.component.key) {
      this.populateRecentHistory();
    }
  }

  populateRecentHistory = () => {
    const { breadcrumbs } = this.props.component;
    const { qualifier } = breadcrumbs[breadcrumbs.length - 1];
    if (['TRK', 'VW', 'APP', 'DEV'].indexOf(qualifier) !== -1) {
      RecentHistory.add(
        this.props.component.key,
        this.props.component.name,
        qualifier.toLowerCase(),
        this.props.component.organization
      );
    }
  };

  render() {
    const { component, currentBranchLike, currentTask, isInProgress, isPending } = this.props;
    const contextNavHeight = rawSizes.contextNavHeightRaw;
    let notifComponent;
    if (isInProgress || isPending || (currentTask && currentTask.status === STATUSES.FAILED)) {
      notifComponent = (
        <ComponentNavBgTaskNotif
          component={component}
          currentTask={currentTask}
          currentTaskOnSameBranch={this.props.currentTaskOnSameBranch}
          isInProgress={isInProgress}
          isPending={isPending}
        />
      );
    }
    return (
      <ContextNavBar
        height={notifComponent ? contextNavHeight + 30 : contextNavHeight}
        id="context-navigation"
        notif={notifComponent}>
        <div className="navbar-context-justified">
          <ComponentNavHeader
            branchLikes={this.props.branchLikes}
            component={component}
            currentBranchLike={currentBranchLike}
            // to close dropdown on any location change
            location={this.props.location}
          />
          <ComponentNavMeta
            branchLike={currentBranchLike}
            component={component}
            warnings={this.props.warnings}
          />
        </div>
        <ComponentNavMenu
          branchLike={currentBranchLike}
          component={component}
          // to re-render selected menu item
          location={this.props.location}
        />
      </ContextNavBar>
    );
  }
}
