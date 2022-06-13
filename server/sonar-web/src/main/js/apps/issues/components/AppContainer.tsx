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
import withBranchStatusActions from '../../../app/components/branch-status/withBranchStatusActions';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { PageContext } from '../../../app/components/indexation/PageUnavailableDueToIndexation';
import withIndexationGuard from '../../../components/hoc/withIndexationGuard';
import { withRouter } from '../../../components/hoc/withRouter';
import { lazyLoadComponent } from '../../../components/lazyLoadComponent';

const IssuesAppContainer = lazyLoadComponent(() => import('./IssuesApp'), 'IssuesAppContainer');

export default withIndexationGuard(
  withRouter(withCurrentUserContext(withBranchStatusActions(IssuesAppContainer))),
  PageContext.Issues
);
