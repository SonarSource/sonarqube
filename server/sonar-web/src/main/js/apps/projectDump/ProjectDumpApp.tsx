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
import {
  BasicSeparator,
  LargeCenteredLayout,
  PageContentFontWrapper,
  Spinner,
  Title,
} from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { getActivity } from '../../api/ce';
import { getStatus } from '../../api/project-dump';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../app/components/available-features/withAvailableFeatures';
import withComponentContext from '../../app/components/componentContext/withComponentContext';
import { throwGlobalError } from '../../helpers/error';
import { translate } from '../../helpers/l10n';
import { Feature } from '../../types/features';
import { DumpStatus, DumpTask } from '../../types/project-dump';
import { ActivityRequestParameters, TaskStatuses, TaskTypes } from '../../types/tasks';
import { Component } from '../../types/types';
import Export from './components/Export';
import Import from './components/Import';
import './styles.css';

const POLL_INTERNAL = 5000;

interface Props extends WithAvailableFeaturesProps {
  component: Component;
}

interface State {
  lastAnalysisTask?: DumpTask;
  lastExportTask?: DumpTask;
  lastImportTask?: DumpTask;
  status?: DumpStatus;
}

export class ProjectDumpApp extends React.Component<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.loadStatus();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component.key !== this.props.component.key) {
      this.loadStatus();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getLastTask(component: string, type: TaskTypes) {
    const data: ActivityRequestParameters = {
      type,
      component,
      ps: 1,
      status: [
        TaskStatuses.Pending,
        TaskStatuses.InProgress,
        TaskStatuses.Success,
        TaskStatuses.Failed,
        TaskStatuses.Canceled,
      ].join(','),
    };
    return getActivity(data)
      .then(({ tasks }) => (tasks.length > 0 ? tasks[0] : undefined), throwGlobalError)
      .catch(() => undefined);
  }

  getLastTaskOfEachType(componentKey: string) {
    const projectImportFeatureEnabled = this.props.hasFeature(Feature.ProjectImport);
    const all = projectImportFeatureEnabled
      ? [
          this.getLastTask(componentKey, TaskTypes.ProjectExport),
          this.getLastTask(componentKey, TaskTypes.ProjectImport),
          this.getLastTask(componentKey, TaskTypes.Report),
        ]
      : [
          this.getLastTask(componentKey, TaskTypes.ProjectExport),
          Promise.resolve(),
          this.getLastTask(componentKey, TaskTypes.Report),
        ];
    return Promise.all(all).then(([lastExportTask, lastImportTask, lastAnalysisTask]) => ({
      lastExportTask,
      lastImportTask,
      lastAnalysisTask,
    }));
  }

  loadStatus = () => {
    const { component } = this.props;
    return Promise.all([getStatus(component.key), this.getLastTaskOfEachType(component.key)]).then(
      ([status, { lastExportTask, lastImportTask, lastAnalysisTask }]) => {
        if (this.mounted) {
          this.setState({
            status,
            lastExportTask,
            lastImportTask,
            lastAnalysisTask,
          });
        }
        return {
          status,
          lastExportTask,
          lastImportTask,
          lastAnalysisTask,
        };
      },
    );
  };

  poll = () => {
    this.loadStatus().then(
      ({ lastExportTask, lastImportTask }) => {
        if (this.mounted) {
          const progressStatus = [TaskStatuses.Pending, TaskStatuses.InProgress];
          const exportNotFinished =
            lastExportTask === undefined || progressStatus.includes(lastExportTask.status);
          const importNotFinished =
            lastImportTask === undefined || progressStatus.includes(lastImportTask.status);
          if (exportNotFinished || importNotFinished) {
            setTimeout(this.poll, POLL_INTERNAL);
          } else {
            // Since we fetch status separate from task we could not get an up to date status.
            // even if we detect that export / import is finish.
            // Doing a last call will make sur we get the latest status.
            this.loadStatus();
          }
        }
      },
      () => {
        /* no catch needed */
      },
    );
  };

  render() {
    const { component } = this.props;
    const projectImportFeatureEnabled = this.props.hasFeature(Feature.ProjectImport);
    const { lastAnalysisTask, lastExportTask, lastImportTask, status } = this.state;

    return (
      <LargeCenteredLayout id="project-dump">
        <PageContentFontWrapper className="sw-my-8 sw-body-sm">
          <header className="sw-mb-5">
            <Helmet defer={false} title={translate('project_dump.page')} />
            <Title className="sw-mb-4">{translate('project_dump.page')}</Title>
            <p>
              {projectImportFeatureEnabled
                ? translate('project_dump.page.description')
                : translate('project_dump.page.description_without_import')}
            </p>
          </header>

          <Spinner loading={status === undefined}>
            {status && (
              <>
                <Export
                  componentKey={component.key}
                  loadStatus={this.poll}
                  status={status}
                  task={lastExportTask}
                />
                <BasicSeparator className="sw-my-8" />
                <Import
                  importEnabled={!!projectImportFeatureEnabled}
                  analysis={lastAnalysisTask}
                  componentKey={component.key}
                  loadStatus={this.poll}
                  status={status}
                  task={lastImportTask}
                />
              </>
            )}
          </Spinner>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

export default withComponentContext(withAvailableFeatures(ProjectDumpApp));
