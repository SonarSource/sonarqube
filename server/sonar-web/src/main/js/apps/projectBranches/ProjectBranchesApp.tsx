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
import { Helmet } from 'react-helmet-async';
import { LargeCenteredLayout, PageContentFontWrapper, Title } from '~design-system';
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
    <LargeCenteredLayout id="project-branch-like">
      <PageContentFontWrapper className="sw-my-8 sw-typo-default">
        <header className="sw-mb-5">
          <Helmet defer={false} title={translate('project_branch_pull_request.page')} />
          <Title className="sw-mb-4">{translate('project_branch_pull_request.page')}</Title>
          <LifetimeInformation component={component} />
        </header>

        <BranchLikeTabs component={component} fetchComponent={fetchComponent} />
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default withComponentContext(withBranchLikes(React.memo(ProjectBranchesApp)));
