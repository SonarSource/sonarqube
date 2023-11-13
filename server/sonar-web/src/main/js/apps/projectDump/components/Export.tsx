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
import { ButtonSecondary, FlagMessage, Link, Spinner } from 'design-system';
import * as React from 'react';
import { doExport } from '../../../api/project-dump';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { DumpStatus, DumpTask } from '../../../types/project-dump';

interface Props {
  componentKey: string;
  loadStatus: () => void;
  status: DumpStatus;
  task?: DumpTask;
}

export default function Export(props: Readonly<Props>) {
  const handleExport = async () => {
    try {
      await doExport(props.componentKey);
      props.loadStatus();
    } catch (error) {
      /* no catch needed */
    }
  };

  function renderHeader() {
    return (
      <div className="sw-mb-4">
        <span className="sw-heading-md">{translate('project_dump.export')}</span>
      </div>
    );
  }

  function renderWhenCanNotExport() {
    return (
      <>
        {renderHeader()}
        <FlagMessage className="sw-mb-4" variant="warning">
          {translate('project_dump.can_not_export')}
        </FlagMessage>
      </>
    );
  }

  function renderWhenExportPending(task: DumpTask) {
    return (
      <>
        {renderHeader()}
        <div>
          <Spinner />
          <DateTimeFormatter date={task.submittedAt}>
            {(formatted) => (
              <span>{translateWithParameters('project_dump.pending_export', formatted)}</span>
            )}
          </DateTimeFormatter>
        </div>
      </>
    );
  }

  function renderWhenExportInProgress(task: DumpTask) {
    return (
      <>
        {renderHeader()}
        <div>
          <Spinner />
          {task.startedAt && (
            <DateFromNow date={task.startedAt}>
              {(fromNow) => (
                <span>{translateWithParameters('project_dump.in_progress_export', fromNow)}</span>
              )}
            </DateFromNow>
          )}
        </div>
      </>
    );
  }

  function renderWhenExportFailed() {
    const { componentKey } = props;
    const detailsUrl = `/project/background_tasks?id=${encodeURIComponent(
      componentKey,
    )}&status=FAILED&taskType=PROJECT_EXPORT`;

    return (
      <>
        {renderHeader()}
        <div>
          <FlagMessage className="sw-mb-4" variant="error">
            {translate('project_dump.failed_export')}
            <Link className="sw-ml-1" to={detailsUrl}>
              {translate('project_dump.see_details')}
            </Link>
          </FlagMessage>

          {renderExport()}
        </div>
      </>
    );
  }

  function renderDump(task?: DumpTask) {
    const { status } = props;

    return (
      <FlagMessage className="sw-mb-4" variant="success">
        <div>
          {task && task.executedAt && (
            <DateTimeFormatter date={task.executedAt}>
              {(formatted) => (
                <div>
                  {translateWithParameters('project_dump.latest_export_available', formatted)}
                </div>
              )}
            </DateTimeFormatter>
          )}
          <div>
            {!task && <div>{translate('project_dump.export_available')}</div>}

            <code tabIndex={0}>{status.exportedDump}</code>
          </div>
        </div>
      </FlagMessage>
    );
  }

  function renderExport() {
    return (
      <>
        <div>{translate('project_dump.export_form_description')}</div>
        <ButtonSecondary
          aria-label={translate('project_dump.do_export')}
          className="sw-mt-4"
          onClick={handleExport}
        >
          {translate('project_dump.do_export')}
        </ButtonSecondary>
      </>
    );
  }

  const { task, status } = props;

  if (!status.canBeExported) {
    return renderWhenCanNotExport();
  }

  if (task && task.status === 'PENDING') {
    return renderWhenExportPending(task);
  }

  if (task && task.status === 'IN_PROGRESS') {
    return renderWhenExportInProgress(task);
  }

  if (task && task.status === 'FAILED') {
    return renderWhenExportFailed();
  }

  const isDumpAvailable = Boolean(status.exportedDump);

  return (
    <>
      {renderHeader()}
      <div>
        {isDumpAvailable && renderDump(task)}
        {renderExport()}
      </div>
    </>
  );
}
