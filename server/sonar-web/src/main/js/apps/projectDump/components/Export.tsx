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
import { Button, Spinner } from '@sonarsource/echoes-react';
import { FlagMessage, Link } from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useLastActivityQuery } from '../../../queries/ce';
import { useProjectDumpStatusQuery, useProjectExportMutation } from '../../../queries/project-dump';
import { DumpTask } from '../../../types/project-dump';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';
import { getImportExportActivityParams } from '../utils';

interface Props {
  componentKey: string;
}
const PROGRESS_STATUS = [TaskStatuses.Pending, TaskStatuses.InProgress];
const REFRESH_INTERVAL = 5000;

export default function Export({ componentKey }: Readonly<Props>) {
  const { data: task, refetch: refetchLastActivity } = useLastActivityQuery(
    getImportExportActivityParams(componentKey, TaskTypes.ProjectExport),
    {
      refetchInterval: ({ state: { data } }) => {
        return data && PROGRESS_STATUS.includes(data.status) ? REFRESH_INTERVAL : undefined;
      },
    },
  );
  const { data: status, refetch: refetchDumpStatus } = useProjectDumpStatusQuery(componentKey, {
    refetchInterval: () => {
      return task && PROGRESS_STATUS.includes(task.status) ? REFRESH_INTERVAL : undefined;
    },
  });
  const { mutateAsync: doExport } = useProjectExportMutation();

  const isDumpAvailable = Boolean(status?.exportedDump);

  const handleExport = async () => {
    try {
      await doExport(componentKey);
      refetchLastActivity();
      refetchDumpStatus();
    } catch (_) {
      noop();
    }
  };

  function renderWhenCanNotExport() {
    return (
      <FlagMessage className="sw-mb-4" variant="warning">
        {translate('project_dump.can_not_export')}
      </FlagMessage>
    );
  }

  function renderWhenExportPending(task: DumpTask) {
    return (
      <div className="sw-flex sw-gap-2">
        <Spinner />
        <DateTimeFormatter date={task.submittedAt}>
          {(formatted) => (
            <output>{translateWithParameters('project_dump.pending_export', formatted)}</output>
          )}
        </DateTimeFormatter>
      </div>
    );
  }

  function renderWhenExportInProgress(task: DumpTask) {
    return (
      <div className="sw-flex sw-gap-2">
        <Spinner />
        {task.startedAt && (
          <DateFromNow date={task.startedAt}>
            {(fromNow) => (
              <output>{translateWithParameters('project_dump.in_progress_export', fromNow)}</output>
            )}
          </DateFromNow>
        )}
      </div>
    );
  }

  function renderWhenExportFailed() {
    const detailsUrl = `/project/background_tasks?id=${encodeURIComponent(
      componentKey,
    )}&status=FAILED&taskType=PROJECT_EXPORT`;

    return (
      <div>
        <FlagMessage className="sw-mb-4" variant="error">
          {translate('project_dump.failed_export')}
          <Link className="sw-ml-1" to={detailsUrl}>
            {translate('project_dump.see_details')}
          </Link>
        </FlagMessage>

        {renderExport()}
      </div>
    );
  }

  function renderDump(task?: DumpTask | null) {
    return (
      <FlagMessage className="sw-mb-4" variant="success">
        <div>
          {task?.executedAt && (
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

            <p className="sw-mt-2">{status?.exportedDump}</p>
          </div>
        </div>
      </FlagMessage>
    );
  }

  function renderExport() {
    return (
      <>
        <div>{translate('project_dump.export_form_description')}</div>
        <Button
          aria-label={translate('project_dump.do_export')}
          className="sw-mt-4"
          onClick={handleExport}
        >
          {translate('project_dump.do_export')}
        </Button>
      </>
    );
  }

  if (status === undefined || task === undefined) {
    return <Spinner />;
  }

  if (!status.canBeExported) {
    return renderWhenCanNotExport();
  }

  if (task?.status === TaskStatuses.Pending) {
    return renderWhenExportPending(task);
  }

  if (task?.status === TaskStatuses.InProgress) {
    return renderWhenExportInProgress(task);
  }

  if (task?.status === TaskStatuses.Failed) {
    return renderWhenExportFailed();
  }

  return (
    <div>
      {isDumpAvailable && renderDump(task)}
      {renderExport()}
    </div>
  );
}
