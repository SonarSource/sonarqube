/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Helmet from 'react-helmet';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import { translate } from 'sonar-ui-common/helpers/l10n';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { MeasureHistory, ParsedAnalysis, Query } from '../utils';
import './projectActivity.css';
import ProjectActivityAnalysesList from './ProjectActivityAnalysesList';
import ProjectActivityGraphs from './ProjectActivityGraphs';
import ProjectActivityPageHeader from './ProjectActivityPageHeader';

interface Props {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<void>;
  addVersion: (analysis: string, version: string) => Promise<void>;
  analyses: ParsedAnalysis[];
  analysesLoading: boolean;
  changeEvent: (event: string, name: string) => Promise<void>;
  deleteAnalysis: (analysis: string) => Promise<void>;
  deleteEvent: (analysis: string, event: string) => Promise<void>;
  graphLoading: boolean;
  initializing: boolean;
  project: Pick<T.Component, 'configuration' | 'key' | 'leakPeriodDate' | 'qualifier'>;
  metrics: T.Metric[];
  measuresHistory: MeasureHistory[];
  query: Query;
  updateQuery: (changes: Partial<Query>) => void;
}

export default function ProjectActivityApp(props: Props) {
  const { analyses, measuresHistory, query } = props;
  const { configuration } = props.project;
  const canAdmin =
    (props.project.qualifier === 'TRK' || props.project.qualifier === 'APP') &&
    (configuration ? configuration.showHistory : false);
  const canDeleteAnalyses = configuration ? configuration.showHistory : false;
  return (
    <div className="page page-limited" id="project-activity">
      <Suggestions suggestions="project_activity" />
      <Helmet title={translate('project_activity.page')} />

      <A11ySkipTarget anchor="activity_main" />

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
            analyses={analyses}
            analysesLoading={props.analysesLoading}
            canAdmin={canAdmin}
            canDeleteAnalyses={canDeleteAnalyses}
            changeEvent={props.changeEvent}
            className="boxed-group-inner"
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
            project={props.project.key}
            query={query}
            updateQuery={props.updateQuery}
          />
        </div>
      </div>
    </div>
  );
}
