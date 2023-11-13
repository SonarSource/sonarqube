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
import { doImport } from '../../../api/project-dump';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
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

export default function Import(props: Readonly<Props>) {
  const handleImport = () => {
    doImport(props.componentKey).then(props.loadStatus, () => {
      /* no catch needed */
    });
  };

  function renderWhenCanNotImport() {
    return <span>{translate('project_dump.can_not_import')}</span>;
  }

  function renderWhenNoDump() {
    return (
      <FlagMessage variant="warning">{translate('project_dump.no_file_to_import')}</FlagMessage>
    );
  }

  function renderImportForm() {
    return (
      <>
        <div className="sw-mt-4">{translate('project_dump.import_form_description')}</div>
        <ButtonSecondary
          aria-label={translate('project_dump.do_import')}
          className="sw-mt-4"
          onClick={handleImport}
        >
          {translate('project_dump.do_import')}
        </ButtonSecondary>
      </>
    );
  }

  function renderWhenImportSuccess(task: DumpTask) {
    return (
      <>
        {task.executedAt && (
          <DateTimeFormatter date={task.executedAt}>
            {(formatted) => (
              <FlagMessage variant="success">
                {translateWithParameters('project_dump.import_success', formatted)}
              </FlagMessage>
            )}
          </DateTimeFormatter>
        )}
      </>
    );
  }

  function renderWhenImportPending(task: DumpTask) {
    return (
      <>
        <Spinner />
        <DateTimeFormatter date={task.submittedAt}>
          {(formatted) => (
            <span>{translateWithParameters('project_dump.pending_import', formatted)}</span>
          )}
        </DateTimeFormatter>
      </>
    );
  }

  function renderWhenImportInProgress(task: DumpTask) {
    return (
      <>
        <Spinner />
        {task.startedAt && (
          <DateFromNow date={task.startedAt}>
            {(fromNow) => (
              <span>{translateWithParameters('project_dump.in_progress_import', fromNow)}</span>
            )}
          </DateFromNow>
        )}
      </>
    );
  }

  function renderWhenImportFailed() {
    const { componentKey } = props;
    const detailsUrl = getComponentBackgroundTaskUrl(
      componentKey,
      TaskStatuses.Failed,
      TaskTypes.ProjectImport,
    );

    return (
      <div>
        <FlagMessage variant="error">
          {translate('project_dump.failed_import')}
          <Link className="sw-ml-1" to={detailsUrl}>
            {translate('project_dump.see_details')}
          </Link>
        </FlagMessage>

        {renderImportForm()}
      </div>
    );
  }

  const { importEnabled, status, task, analysis } = props;

  function renderContent(): React.ReactNode {
    switch (task?.status) {
      case TaskStatuses.Success:
        if (!analysis) {
          return renderWhenImportSuccess(task);
        }
        break;
      case TaskStatuses.Pending:
        return renderWhenImportPending(task);
      case TaskStatuses.InProgress:
        return renderWhenImportInProgress(task);
      case TaskStatuses.Failed:
        return renderWhenImportFailed();
      default:
        if (!status.canBeImported) {
          return renderWhenCanNotImport();
        } else if (!status.dumpToImport) {
          return renderWhenNoDump();
        }
        return <div>{renderImportForm()}</div>;
    }
  }

  return (
    <>
      <div className="sw-my-4">
        <span className="sw-heading-md">{translate('project_dump.import')}</span>
      </div>

      {importEnabled ? (
        renderContent()
      ) : (
        <div>{translate('project_dump.import_form_description_disabled')}</div>
      )}
    </>
  );
}
