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
import * as React from 'react';
import ScannerContext from './ScannerContext';
import Stacktrace from './Stacktrace';
import { STATUSES } from './../constants';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Task } from '../types';
import ActionsDropdown, { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';

interface Props {
  component?: {};
  onCancelTask: (task: Task) => void;
  onFilterTask: (task: Task) => void;
  task: Task;
}

interface State {
  scannerContextOpen: boolean;
  stacktraceOpen: boolean;
}

export default class TaskActions extends React.PureComponent<Props, State> {
  state: State = {
    scannerContextOpen: false,
    stacktraceOpen: false
  };

  handleFilterClick = () => {
    this.props.onFilterTask(this.props.task);
  };

  handleCancelClick = () => {
    this.props.onCancelTask(this.props.task);
  };

  handleShowScannerContextClick = () => {
    this.setState({ scannerContextOpen: true });
  };

  closeScannerContext = () => this.setState({ scannerContextOpen: false });

  handleShowStacktraceClick = () => {
    this.setState({ stacktraceOpen: true });
  };

  closeStacktrace = () => this.setState({ stacktraceOpen: false });

  render() {
    const { component, task } = this.props;

    const canFilter = component === undefined;
    const canCancel = task.status === STATUSES.PENDING;
    const canShowStacktrace = task.errorMessage !== undefined;
    const hasActions = canFilter || canCancel || task.hasScannerContext || canShowStacktrace;

    if (!hasActions) {
      return <td>&nbsp;</td>;
    }

    return (
      <td className="thin nowrap">
        <ActionsDropdown className="js-task-action">
          {canFilter &&
            task.componentName && (
              <ActionsDropdownItem className="js-task-filter" onClick={this.handleFilterClick}>
                {translateWithParameters(
                  'background_tasks.filter_by_component_x',
                  task.componentName
                )}
              </ActionsDropdownItem>
            )}
          {canCancel && (
            <ActionsDropdownItem
              className="js-task-cancel"
              destructive={true}
              onClick={this.handleCancelClick}>
              {translate('background_tasks.cancel_task')}
            </ActionsDropdownItem>
          )}
          {task.hasScannerContext && (
            <ActionsDropdownItem
              className="js-task-show-scanner-context"
              onClick={this.handleShowScannerContextClick}>
              {translate('background_tasks.show_scanner_context')}
            </ActionsDropdownItem>
          )}
          {canShowStacktrace && (
            <ActionsDropdownItem
              className="js-task-show-stacktrace"
              onClick={this.handleShowStacktraceClick}>
              {translate('background_tasks.show_stacktrace')}
            </ActionsDropdownItem>
          )}
        </ActionsDropdown>

        {this.state.scannerContextOpen && (
          <ScannerContext onClose={this.closeScannerContext} task={task} />
        )}

        {this.state.stacktraceOpen && <Stacktrace onClose={this.closeStacktrace} task={task} />}
      </td>
    );
  }
}
