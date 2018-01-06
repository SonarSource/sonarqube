/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Helmet from 'react-helmet';
import ProjectActivityPageHeader from './ProjectActivityPageHeader';
import ProjectActivityAnalysesList from './ProjectActivityAnalysesList';
import ProjectActivityGraphs from './ProjectActivityGraphs';
import { parseDate } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import './projectActivity.css';
/*:: import type { Analysis, MeasureHistory, Metric, Query } from '../types'; */

/*::
type Props = {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<*>,
  addVersion: (analysis: string, version: string) => Promise<*>,
  analyses: Array<Analysis>,
  analysesLoading: boolean,
  changeEvent: (event: string, name: string) => Promise<*>,
  deleteAnalysis: (analysis: string) => Promise<*>,
  deleteEvent: (analysis: string, event: string) => Promise<*>,
  graphLoading: boolean,
  initializing: boolean,
  project: {
    configuration?: { showHistory: boolean },
    key: string,
    leakPeriodDate?: string,
    qualifier: string
  },
  metrics: Array<Metric>,
  measuresHistory: Array<MeasureHistory>,
  query: Query,
  updateQuery: (newQuery: Query) => void
};
*/

export default function ProjectActivityApp(props /*: Props */) {
  const { analyses, measuresHistory, query } = props;
  const { configuration } = props.project;
  const canAdmin =
    (props.project.qualifier === 'TRK' || props.project.qualifier === 'APP') &&
    (configuration ? configuration.showHistory : false);
  const canDeleteAnalyses = configuration ? configuration.showHistory : false;
  return (
    <div id="project-activity" className="page page-limited">
      <Helmet title={translate('project_activity.page')} />

      <ProjectActivityPageHeader
        category={query.category}
        from={query.from}
        project={props.project}
        to={query.to}
        updateQuery={props.updateQuery}
      />

      <div className="layout-page project-activity-page">
        <div className="layout-page-side-outer project-activity-page-side-outer boxed-group">
          <ProjectActivityAnalysesList
            addCustomEvent={props.addCustomEvent}
            addVersion={props.addVersion}
            analysesLoading={props.analysesLoading}
            analyses={analyses}
            canAdmin={canAdmin}
            canDeleteAnalyses={canDeleteAnalyses}
            className="boxed-group-inner"
            changeEvent={props.changeEvent}
            deleteAnalysis={props.deleteAnalysis}
            deleteEvent={props.deleteEvent}
            initializing={props.initializing}
            project={props.project}
            query={props.query}
            updateQuery={props.updateQuery}
          />
        </div>
        <div className="project-activity-layout-page-main">
          <ProjectActivityGraphs
            analyses={analyses}
            leakPeriodDate={
              props.project.leakPeriodDate ? parseDate(props.project.leakPeriodDate) : undefined
            }
            loading={props.graphLoading}
            measuresHistory={measuresHistory}
            metrics={props.metrics}
            query={query}
            updateQuery={props.updateQuery}
          />
        </div>
      </div>
    </div>
  );
}
