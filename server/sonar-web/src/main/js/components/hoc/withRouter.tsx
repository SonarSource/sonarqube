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
import {
  Location as LocationRouter,
  Params,
  useLocation as useLocationRouter,
  useNavigate,
  useParams,
} from 'react-router-dom';
import { queryToSearch, searchParamsToQuery } from '../../helpers/urls';
import { RawQuery } from '../../types/types';
import { getWrappedDisplayName } from './utils';

export interface Location extends LocationRouter {
  query: RawQuery;
}

export interface Router {
  replace: (location: string | Partial<Location>) => void;
  push: (location: string | Partial<Location>) => void;
}

export interface WithRouterProps {
  location: Location;
  params: Params;
  router: Router;
}

export function withRouter<P extends Partial<WithRouterProps>>(
  WrappedComponent: React.ComponentType<React.PropsWithChildren<P>>,
): React.ComponentType<React.PropsWithChildren<Omit<P, keyof WithRouterProps>>> {
  function ComponentWithRouterProp(props: P) {
    const router = useRouter();
    const params = useParams();
    const location = useLocation();

    return <WrappedComponent {...props} location={location} params={params} router={router} />;
  }

  (ComponentWithRouterProp as React.FC<React.PropsWithChildren<P>>).displayName =
    getWrappedDisplayName(WrappedComponent, 'withRouter');

  return ComponentWithRouterProp;
}

export function useRouter() {
  const navigate = useNavigate();

  const router = React.useMemo(
    () => ({
      replace: (path: string | Partial<Location>) => {
        if ((path as Location).query) {
          path.search = queryToSearch((path as Location).query);
        }
        navigate(path, { replace: true });
      },
      push: (path: string | Partial<Location>) => {
        if ((path as Location).query) {
          path.search = queryToSearch((path as Location).query);
        }
        navigate(path);
      },
    }),
    [navigate],
  );

  return router;
}

export function useLocation() {
  const location = useLocationRouter();

  return { ...location, query: searchParamsToQuery(new URLSearchParams(location.search)) };
}
