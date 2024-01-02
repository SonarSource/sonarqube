/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import BranchLikeTabs from './BranchLikeTabs';
import LifetimeInformation from './LifetimeInformation';

export interface ProjectBranchesAppProps {
  branchLikes: BranchLike[];
  component: Component;
  onBranchesChange: () => void;
}

export function ProjectBranchesApp(props: ProjectBranchesAppProps) {
  const { branchLikes, component } = props;

  return (
    <div className="page page-limited" id="project-branch-like">
      <header className="page-header">
        <h1>{translate('project_branch_pull_request.page')}</h1>
        <LifetimeInformation />
      </header>

      <BranchLikeTabs
        branchLikes={branchLikes}
        component={component}
        onBranchesChange={props.onBranchesChange}
      />
    </div>
  );
}

export default withComponentContext(React.memo(ProjectBranchesApp));
