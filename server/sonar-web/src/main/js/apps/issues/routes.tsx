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

import { useEffect } from 'react';
import { Route, useNavigate, useSearchParams } from 'react-router-dom';
import { lazyLoadComponent } from '~sonar-aligned/helpers/lazyLoadComponent';
import { omitNil } from '../../helpers/request';
import { IssueType } from '../../types/issues';

const IssuesApp = lazyLoadComponent(() => import('./components/IssuesApp'));

export const globalIssuesRoutes = () => <Route path="issues" element={<IssuesApp />} />;

export const projectIssuesRoutes = () => (
  <Route path="project/issues" element={<IssuesNavigate />} />
);

function IssuesNavigate() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    if (searchParams.has('types')) {
      const types = searchParams.get('types') ?? '';

      if (types === IssueType.SecurityHotspot) {
        navigate(
          {
            pathname: '/security_hotspots',
            search: new URLSearchParams(
              omitNil({
                id: searchParams.get('id'),
                branch: searchParams.get('branch'),
                pullRequest: searchParams.get('pullRequest'),
                assignedToMe: 'false',
              }),
            ).toString(),
          },
          { replace: true },
        );
      } else {
        const filteredTypes = types
          .split(',')
          .filter((type: string) => type !== IssueType.SecurityHotspot)
          .join(',');

        if (types !== filteredTypes) {
          searchParams.set('types', filteredTypes);

          setSearchParams(searchParams, { replace: true });
        }
      }
    }
  }, [navigate, searchParams, setSearchParams]);

  return <IssuesApp />;
}
