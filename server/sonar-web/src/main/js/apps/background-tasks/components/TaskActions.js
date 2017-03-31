/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';
import ScannerContextView from '../views/ScannerContextView';
import StacktraceView from '../views/StacktraceView';
import { STATUSES } from './../constants';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default class TaskActions extends React.Component {
  shouldComponentUpdate(nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  handleFilterClick(e) {
    e.preventDefault();
    this.props.onFilterTask(this.props.task);
  }

  handleCancelClick(e) {
    e.preventDefault();
    this.props.onCancelTask(this.props.task);
  }

  handleShowScannerContextClick(e) {
    e.preventDefault();
    new ScannerContextView({ task: this.props.task }).render();
  }

  handleShowStacktraceClick(e) {
    e.preventDefault();
    new StacktraceView({ task: this.props.task }).render();
  }

  render() {
    const { component, task } = this.props;

    const canFilter = component == null;
    const canCancel = task.status === STATUSES.PENDING;
    const canShowStacktrace = task.errorMessage != null;
    const hasActions = canFilter || canCancel || task.hasScannerContext || canShowStacktrace;

    if (!hasActions) {
      return <td>&nbsp;</td>;
    }

    return (
      <td className="thin nowrap">
        <div className="dropdown js-task-action">
          <button className="dropdown-toggle" data-toggle="dropdown">
            <i className="icon-dropdown" />
          </button>
          <ul className="dropdown-menu dropdown-menu-right">
            {canFilter &&
              <li>
                <a className="js-task-filter" href="#" onClick={this.handleFilterClick.bind(this)}>
                  <i className="spacer-right icon-filter icon-gray" />
                  {translateWithParameters(
                    'background_tasks.filter_by_component_x',
                    task.componentName
                  )}
                </a>
              </li>}
            {canCancel &&
              <li>
                <a className="js-task-cancel" href="#" onClick={this.handleCancelClick.bind(this)}>
                  <i className="spacer-right icon-delete" />
                  {translate('background_tasks.cancel_task')}
                </a>
              </li>}
            {task.hasScannerContext &&
              <li>
                <a
                  className="js-task-show-scanner-context"
                  href="#"
                  onClick={this.handleShowScannerContextClick.bind(this)}>
                  <i className="spacer-right icon-list icon-gray" />
                  {translate('background_tasks.show_scanner_context')}
                </a>
              </li>}
            {canShowStacktrace &&
              <li>
                <a
                  className="js-task-show-stacktrace"
                  href="#"
                  onClick={this.handleShowStacktraceClick.bind(this)}>
                  <i className="spacer-right icon-list icon-red" />
                  {translate('background_tasks.show_stacktrace')}
                </a>
              </li>}
          </ul>
        </div>
      </td>
    );
  }
}
