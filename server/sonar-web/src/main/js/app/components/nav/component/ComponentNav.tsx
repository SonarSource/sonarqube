/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import ContextNavBar from 'sonar-ui-common/components/ui/ContextNavBar';
import { BranchLike } from '../../../../types/branch-like';
import { ComponentQualifier } from '../../../../types/component';
import { Task, TaskStatuses, TaskWarning } from '../../../../types/tasks';
import { rawSizes } from '../../../theme';
import RecentHistory from '../../RecentHistory';
import ComponentNavBgTaskNotif from './ComponentNavBgTaskNotif';
import Header from './Header';
import HeaderMeta from './HeaderMeta';
import Menu from './Menu';
import InfoDrawer from './projectInformation/InfoDrawer';
import ProjectInformation from './projectInformation/ProjectInformation';

export interface ComponentNavProps {
  branchLikes: BranchLike[];
  currentBranchLike: BranchLike | undefined;
  component: T.Component;
  currentTask?: Task;
  currentTaskOnSameBranch?: boolean;
  isInProgress?: boolean;
  isPending?: boolean;
  onComponentChange: (changes: Partial<T.Component>) => void;
  onWarningDismiss: () => void;
  warnings: TaskWarning[];
}

export default function ComponentNav(props: ComponentNavProps) {
  const {
    branchLikes,
    component,
    currentBranchLike,
    currentTask,
    currentTaskOnSameBranch,
    isInProgress,
    isPending,
    warnings
  } = props;
  const { contextNavHeightRaw, globalNavHeightRaw } = rawSizes;

  const [displayProjectInfo, setDisplayProjectInfo] = React.useState(false);

  React.useEffect(() => {
    const { breadcrumbs, key, name, organization } = component;
    const { qualifier } = breadcrumbs[breadcrumbs.length - 1];
    if (
      [
        ComponentQualifier.Project,
        ComponentQualifier.Portfolio,
        ComponentQualifier.Application,
        ComponentQualifier.Developper
      ].includes(qualifier as ComponentQualifier)
    ) {
      RecentHistory.add(key, name, qualifier.toLowerCase(), organization);
    }
  }, [component, component.key]);

  let notifComponent;
  if (isInProgress || isPending || (currentTask && currentTask.status === TaskStatuses.Failed)) {
    notifComponent = (
      <ComponentNavBgTaskNotif
        component={component}
        currentTask={currentTask}
        currentTaskOnSameBranch={currentTaskOnSameBranch}
        isInProgress={isInProgress}
        isPending={isPending}
      />
    );
  }

  const contextNavHeight = notifComponent ? contextNavHeightRaw + 30 : contextNavHeightRaw;

  return (
    <ContextNavBar height={contextNavHeight} id="context-navigation" notif={notifComponent}>
      <div
        className={classNames('display-flex-center display-flex-space-between little-padded-top', {
          'padded-bottom': warnings.length === 0
        })}>
        <Header
          branchLikes={branchLikes}
          component={component}
          currentBranchLike={currentBranchLike}
        />
        <HeaderMeta
          branchLike={currentBranchLike}
          component={component}
          onWarningDismiss={props.onWarningDismiss}
          warnings={warnings}
        />
      </div>
      <Menu
        branchLike={currentBranchLike}
        branchLikes={branchLikes}
        component={component}
        isInProgress={isInProgress}
        isPending={isPending}
        onToggleProjectInfo={() => setDisplayProjectInfo(!displayProjectInfo)}
      />
      <InfoDrawer
        displayed={displayProjectInfo}
        onClose={() => setDisplayProjectInfo(false)}
        top={globalNavHeightRaw + contextNavHeightRaw}>
        <ProjectInformation
          branchLike={currentBranchLike}
          component={component}
          onComponentChange={props.onComponentChange}
        />
      </InfoDrawer>
    </ContextNavBar>
  );
}
