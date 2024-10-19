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
import { ActionCell, ActionsDropdown, ItemButton, ItemDangerButton } from 'design-system';
import * as React from 'react';
import ConfirmModal from '../../../components/controls/ConfirmModal';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Task, TaskStatuses } from '../../../types/tasks';
import AnalysisWarningsModal from './AnalysisWarningsModal';
import ScannerContext from './ScannerContext';
import Stacktrace from './Stacktrace';

interface Props {
  component?: unknown;
  onCancelTask: (task: Task) => Promise<void>;
  onFilterTask: (task: Task) => void;
  task: Task;
  taskIndex: number;
}

interface State {
  cancelTaskOpen: boolean;
  scannerContextOpen: boolean;
  stacktraceOpen: boolean;
  warningsOpen: boolean;
}

export default class TaskActions extends React.PureComponent<Props, State> {
  state: State = {
    cancelTaskOpen: false,
    scannerContextOpen: false,
    stacktraceOpen: false,
    warningsOpen: false,
  };

  handleFilterClick = () => {
    this.props.onFilterTask(this.props.task);
  };

  handleCancelTask = () => {
    return this.props.onCancelTask(this.props.task);
  };

  handleCancelClick = () => {
    this.setState({ cancelTaskOpen: true });
  };

  handleShowScannerContextClick = () => {
    this.setState({ scannerContextOpen: true });
  };

  closeCancelTask = () => {
    this.setState({ cancelTaskOpen: false });
  };

  closeScannerContext = () => {
    this.setState({ scannerContextOpen: false });
  };

  handleShowStacktraceClick = () => {
    this.setState({ stacktraceOpen: true });
  };

  closeStacktrace = () => {
    this.setState({ stacktraceOpen: false });
  };

  handleShowWarningsClick = () => {
    this.setState({ warningsOpen: true });
  };

  closeWarnings = () => {
    this.setState({ warningsOpen: false });
  };

  render() {
    const { component, task, taskIndex } = this.props;

    const canFilter = component === undefined && task.componentName;
    const canCancel = task.status === TaskStatuses.Pending || (task.status === TaskStatuses.InProgress && task.type === TaskTypes.Report);
    const canShowStacktrace = task.errorMessage !== undefined;
    const canShowWarnings = task.warningCount !== undefined && task.warningCount > 0;
    const hasActions =
      canFilter || canCancel || task.hasScannerContext || canShowStacktrace || canShowWarnings;

    if (!hasActions) {
      return <td>&nbsp;</td>;
    }

    return (
      <ActionCell>
        <ActionsDropdown
          id={`task-${task.id}-actions`}
          ariaLabel={translateWithParameters(
            'background_tasks.show_actions_for_task_x_in_list',
            taskIndex,
          )}
          className="js-task-action"
        >
          {canFilter && task.componentName && (
            <ItemButton className="js-task-filter" onClick={this.handleFilterClick}>
              {translateWithParameters(
                'background_tasks.filter_by_component_x',
                task.componentName,
              )}
            </ItemButton>
          )}
          {canCancel && (
            <ItemDangerButton className="js-task-cancel" onClick={this.handleCancelClick}>
              {translate('background_tasks.cancel_task')}
            </ItemDangerButton>
          )}
          {task.hasScannerContext && (
            <ItemButton
              className="js-task-show-scanner-context"
              onClick={this.handleShowScannerContextClick}
            >
              {translate('background_tasks.show_scanner_context')}
            </ItemButton>
          )}
          {canShowStacktrace && (
            <ItemButton
              className="js-task-show-stacktrace"
              onClick={this.handleShowStacktraceClick}
            >
              {translate('background_tasks.show_stacktrace')}
            </ItemButton>
          )}
          {canShowWarnings && (
            <ItemButton className="js-task-show-warnings" onClick={this.handleShowWarningsClick}>
              {translate('background_tasks.show_warnings')}
            </ItemButton>
          )}
        </ActionsDropdown>

        <ConfirmModal
          cancelButtonText={translate('close')}
          confirmButtonText={translate('background_tasks.cancel_task')}
          header={translate('background_tasks.cancel_task')}
          isDestructive
          isOpen={this.state.cancelTaskOpen}
          onClose={this.closeCancelTask}
          onConfirm={this.handleCancelTask}
        >
          {translate('background_tasks.cancel_task.text')}
        </ConfirmModal>

        {this.state.scannerContextOpen && (
          <ScannerContext onClose={this.closeScannerContext} task={task} />
        )}

        {this.state.stacktraceOpen && <Stacktrace onClose={this.closeStacktrace} task={task} />}

        {this.state.warningsOpen && (
          <AnalysisWarningsModal
            componentKey={task.componentKey}
            onClose={this.closeWarnings}
            taskId={task.id}
          />
        )}
      </ActionCell>
    );
  }
}
