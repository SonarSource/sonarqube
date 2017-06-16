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
import ProjectActivityGraphsHeader from './ProjectActivityGraphsHeader';
import StaticGraphs from './StaticGraphs';
import type { RawQuery } from '../../../helpers/query';
import type { Analysis, MeasureHistory, Query } from '../types';

type Props = {
  analyses: Array<Analysis>,
  leakPeriodDate: Date,
  loading: boolean,
  measuresHistory: Array<MeasureHistory>,
  metricsType: string,
  project: string,
  query: Query,
  updateQuery: RawQuery => void
};

export default function ProjectActivityGraphs(props: Props) {
  const { graph } = props.query;
  return (
    <div className="project-activity-layout-page-main-inner boxed-group boxed-group-inner">
      <ProjectActivityGraphsHeader graph={graph} updateQuery={props.updateQuery} />
      <StaticGraphs
        analyses={props.analyses}
        leakPeriodDate={props.leakPeriodDate}
        loading={props.loading}
        measuresHistory={props.measuresHistory}
        metricsType={props.metricsType}
        project={props.project}
        showAreas={['coverage', 'duplications'].includes(graph)}
      />
    </div>
  );
}
