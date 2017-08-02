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
// @flow
import React from 'react';
import ComponentsList from './ComponentsList';
import ListFooter from '../../../../components/controls/ListFooter';
import type { Component, ComponentEnhanced, Paging } from '../../types';
import type { Metric } from '../../../../store/metrics/actions';

type Props = {
  components: Array<ComponentEnhanced>,
  fetchMore: () => void,
  handleSelect: Component => void,
  metric: Metric,
  metrics: { [string]: Metric },
  paging: ?Paging
};

export default function ListView(props: Props) {
  return (
    <div>
      <ComponentsList
        components={props.components}
        metrics={props.metrics}
        metric={props.metric}
        onClick={props.handleSelect}
      />
      {props.paging &&
        <ListFooter
          count={props.components.length}
          total={props.paging.total}
          loadMore={props.fetchMore}
        />}
    </div>
  );
}
