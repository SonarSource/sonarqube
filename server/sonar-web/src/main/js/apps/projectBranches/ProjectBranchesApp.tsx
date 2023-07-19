/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Helmet } from 'react-helmet-async';
import withComponentContext from '../../app/components/componentContext/withComponentContext';
import { translate } from '../../helpers/l10n';
import { withBranchLikes } from '../../queries/branch';
import { Component } from '../../types/types';
import BranchLikeTabs from './components/BranchLikeTabs';
import LifetimeInformation from './components/LifetimeInformation';

export interface ProjectBranchesAppProps {
  component: Component;
  fetchComponent: () => Promise<void>;
}

function ProjectBranchesApp(props: ProjectBranchesAppProps) {
  const { component, fetchComponent } = props;

  return (
    <div className="page page-limited" id="project-branch-like">
      <header className="page-header">
        <Helmet defer={false} title={translate('project_branch_pull_request.page')} />
        <h1>{translate('project_branch_pull_request.page')}</h1>
        <LifetimeInformation />
      </header>

      <BranchLikeTabs component={component} fetchComponent={fetchComponent} />
    </div>
  );
}

export default withComponentContext(withBranchLikes(React.memo(ProjectBranchesApp)));
