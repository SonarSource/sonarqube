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
import React from 'react';
import { Navigate, Route, useParams, useSearchParams } from 'react-router-dom';
import NavigateWithParams from '../../app/utils/NavigateWithParams';
import { omitNil } from '../../helpers/request';
import { searchParamsToQuery } from '../../helpers/urls';
import ComponentMeasuresApp from './components/ComponentMeasuresApp';

const routes = () => (
  <Route path="component_measures">
    <Route index element={<ComponentMeasuresApp />} />
    <Route
      path="domain/:domainName"
      element={
        <NavigateWithParams
          pathname="/component_measures"
          transformParams={(params) =>
            omitNil({
              metric: params['domainName'],
            })
          }
        />
      }
    />

    <Route path="metric/:metricKey" element={<MetricRedirect />} />
    <Route path="metric/:metricKey/:view" element={<MetricRedirect />} />
  </Route>
);

function MetricRedirect() {
  const params = useParams();
  const [searchParams] = useSearchParams();

  if (params.view === 'history') {
    const to = {
      pathname: '/project/activity',
      search: new URLSearchParams(
        omitNil({
          id: searchParams.get('id') ?? undefined,
          graph: 'custom',
          custom_metrics: params.metricKey,
        }),
      ).toString(),
    };
    return <Navigate to={to} replace />;
  }
  const to = {
    pathname: '/component_measures',
    search: new URLSearchParams(
      omitNil({
        ...searchParamsToQuery(searchParams),
        metric: params.metricKey,
        view: params.view,
      }),
    ).toString(),
  };
  return <Navigate to={to} replace />;
}

export default routes;
