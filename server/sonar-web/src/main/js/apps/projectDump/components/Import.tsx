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
import classNames from 'classnames';
import * as React from 'react';
import { doImport } from '../../../api/project-dump';
import Link from '../../../components/common/Link';
import { Button } from '../../../components/controls/buttons';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { Alert } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getComponentBackgroundTaskUrl } from '../../../helpers/urls';
import { DumpStatus, DumpTask } from '../../../types/project-dump';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';

interface Props {
  analysis?: DumpTask;
  componentKey: string;
  importEnabled: boolean;
  loadStatus: () => void;
  status: DumpStatus;
  task?: DumpTask;
}

export default class Import extends React.Component<Props> {
  handleImport = () => {
    doImport(this.props.componentKey).then(this.props.loadStatus, () => {
      /* no catch needed */
    });
  };

  renderWhenCanNotImport() {
    return (
      <div className="boxed-group-inner" id="import-not-possible">
        {translate('project_dump.can_not_import')}
      </div>
    );
  }

  renderWhenNoDump() {
    return (
      <div className="boxed-group-inner">
        <Alert id="import-no-file" variant="warning">
          {translate('project_dump.no_file_to_import')}
        </Alert>
      </div>
    );
  }

  renderImportForm() {
    return (
      <div>
        <div className="spacer-bottom">{translate('project_dump.import_form_description')}</div>
        <Button onClick={this.handleImport}>{translate('project_dump.do_import')}</Button>
      </div>
    );
  }

  renderWhenImportSuccess(task: DumpTask) {
    return (
      <div className="boxed-group-inner">
        {task.executedAt && (
          <DateTimeFormatter date={task.executedAt}>
            {(formatted) => (
              <Alert variant="success">
                {translateWithParameters('project_dump.import_success', formatted)}
              </Alert>
            )}
          </DateTimeFormatter>
        )}
      </div>
    );
  }

  renderWhenImportPending(task: DumpTask) {
    return (
      <div className="boxed-group-inner" id="import-pending">
        <i className="spinner spacer-right" />
        <DateTimeFormatter date={task.submittedAt}>
          {(formatted) => (
            <span>{translateWithParameters('project_dump.pending_import', formatted)}</span>
          )}
        </DateTimeFormatter>
      </div>
    );
  }

  renderWhenImportInProgress(task: DumpTask) {
    return (
      <div className="boxed-group-inner" id="import-in-progress">
        <i className="spinner spacer-right" />
        {task.startedAt && (
          <DateFromNow date={task.startedAt}>
            {(fromNow) => (
              <span>{translateWithParameters('project_dump.in_progress_import', fromNow)}</span>
            )}
          </DateFromNow>
        )}
      </div>
    );
  }

  renderWhenImportFailed() {
    const { componentKey } = this.props;
    const detailsUrl = getComponentBackgroundTaskUrl(
      componentKey,
      TaskStatuses.Failed,
      TaskTypes.ProjectImport
    );

    return (
      <div className="boxed-group-inner">
        <Alert id="export-in-progress" variant="error">
          {translate('project_dump.failed_import')}
          <Link className="spacer-left" to={detailsUrl}>
            {translate('project_dump.see_details')}
          </Link>
        </Alert>

        {this.renderImportForm()}
      </div>
    );
  }

  render() {
    const { importEnabled, status, task, analysis } = this.props;

    let content: React.ReactNode = null;
    if (task && task.status === TaskStatuses.Success && !analysis) {
      content = this.renderWhenImportSuccess(task);
    } else if (task && task.status === TaskStatuses.Pending) {
      content = this.renderWhenImportPending(task);
    } else if (task && task.status === TaskStatuses.InProgress) {
      content = this.renderWhenImportInProgress(task);
    } else if (task && task.status === TaskStatuses.Failed) {
      content = this.renderWhenImportFailed();
    } else if (!status.canBeImported) {
      content = this.renderWhenCanNotImport();
    } else if (!status.dumpToImport) {
      content = this.renderWhenNoDump();
    } else {
      content = <div className="boxed-group-inner">{this.renderImportForm()}</div>;
    }
    return (
      <div
        className={classNames('boxed-group', {
          'import-disabled text-muted': !importEnabled,
        })}
        id="project-import"
      >
        <div className="boxed-group-header">
          <h2>{translate('project_dump.import')}</h2>
        </div>
        {importEnabled ? (
          content
        ) : (
          <div className="boxed-group-inner">
            {translate('project_dump.import_form_description_disabled')}
          </div>
        )}
      </div>
    );
  }
}
