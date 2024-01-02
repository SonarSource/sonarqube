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
import * as React from 'react';
import { doExport } from '../../../api/project-dump';
import Link from '../../../components/common/Link';
import { Button } from '../../../components/controls/buttons';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { Alert } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { DumpStatus, DumpTask } from '../../../types/project-dump';

interface Props {
  componentKey: string;
  loadStatus: () => void;
  status: DumpStatus;
  task?: DumpTask;
}

export default class Export extends React.Component<Props> {
  handleExport = () => {
    doExport(this.props.componentKey).then(this.props.loadStatus, () => {
      /* no catch needed */
    });
  };

  renderHeader() {
    return (
      <div className="boxed-group-header">
        <h2>{translate('project_dump.export')}</h2>
      </div>
    );
  }

  renderWhenCanNotExport() {
    return (
      <div className="boxed-group" id="project-export">
        {this.renderHeader()}
        <div className="boxed-group-inner">
          <Alert id="export-not-possible" variant="warning">
            {translate('project_dump.can_not_export')}
          </Alert>
        </div>
      </div>
    );
  }

  renderWhenExportPending(task: DumpTask) {
    return (
      <div className="boxed-group" id="project-export">
        {this.renderHeader()}
        <div className="boxed-group-inner" id="export-pending">
          <i className="spinner spacer-right" />
          <DateTimeFormatter date={task.submittedAt}>
            {(formatted) => (
              <span>{translateWithParameters('project_dump.pending_export', formatted)}</span>
            )}
          </DateTimeFormatter>
        </div>
      </div>
    );
  }

  renderWhenExportInProgress(task: DumpTask) {
    return (
      <div className="boxed-group" id="project-export">
        {this.renderHeader()}

        <div className="boxed-group-inner" id="export-in-progress">
          <i className="spinner spacer-right" />
          {task.startedAt && (
            <DateFromNow date={task.startedAt}>
              {(fromNow) => (
                <span>{translateWithParameters('project_dump.in_progress_export', fromNow)}</span>
              )}
            </DateFromNow>
          )}
        </div>
      </div>
    );
  }

  renderWhenExportFailed() {
    const { componentKey } = this.props;
    const detailsUrl = `${getBaseUrl()}/project/background_tasks?id=${encodeURIComponent(
      componentKey
    )}&status=FAILED&taskType=PROJECT_EXPORT`;

    return (
      <div className="boxed-group" id="project-export">
        {this.renderHeader()}

        <div className="boxed-group-inner">
          <Alert id="export-in-progress" variant="error">
            {translate('project_dump.failed_export')}
            <Link className="spacer-left" to={detailsUrl}>
              {translate('project_dump.see_details')}
            </Link>
          </Alert>

          {this.renderExport()}
        </div>
      </div>
    );
  }

  renderDump(task?: DumpTask) {
    const { status } = this.props;

    return (
      <Alert className="export-dump" variant="success">
        {task && task.executedAt && (
          <DateTimeFormatter date={task.executedAt}>
            {(formatted) => (
              <div className="export-dump-message">
                {translateWithParameters('project_dump.latest_export_available', formatted)}
              </div>
            )}
          </DateTimeFormatter>
        )}
        {!task && (
          <div className="export-dump-message">{translate('project_dump.export_available')}</div>
        )}
        <div className="export-dump-path">
          <code tabIndex={0}>{status.exportedDump}</code>
        </div>
      </Alert>
    );
  }

  renderExport() {
    return (
      <div>
        <div className="spacer-bottom">{translate('project_dump.export_form_description')}</div>
        <Button onClick={this.handleExport}>{translate('project_dump.do_export')}</Button>
      </div>
    );
  }

  render() {
    const { status, task } = this.props;

    if (!status.canBeExported) {
      return this.renderWhenCanNotExport();
    }

    if (task && task.status === 'PENDING') {
      return this.renderWhenExportPending(task);
    }

    if (task && task.status === 'IN_PROGRESS') {
      return this.renderWhenExportInProgress(task);
    }

    if (task && task.status === 'FAILED') {
      return this.renderWhenExportFailed();
    }

    const isDumpAvailable = Boolean(status.exportedDump);

    return (
      <div className="boxed-group" id="project-export">
        {this.renderHeader()}
        <div className="boxed-group-inner">
          {isDumpAvailable && this.renderDump(task)}
          {this.renderExport()}
        </div>
      </div>
    );
  }
}
