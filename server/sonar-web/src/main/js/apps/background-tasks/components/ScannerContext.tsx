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
import { getTask } from '../../../api/ce';
import { translate } from '../../../helpers/l10n';
import Modal from '../../../components/controls/Modal';

interface Props {
  onClose: () => void;
  task: Pick<T.Task, 'componentName' | 'id' | 'type'>;
}

interface State {
  scannerContext?: string;
}

export default class ScannerContext extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.loadScannerContext();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadScannerContext() {
    getTask(this.props.task.id, ['scannerContext']).then(task => {
      if (this.mounted) {
        this.setState({ scannerContext: task.scannerContext });
      }
    });
  }

  handleCloseClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  render() {
    const { task } = this.props;
    const { scannerContext } = this.state;

    return (
      <Modal contentLabel="scanner context" onRequestClose={this.props.onClose} size={'large'}>
        <div className="modal-head">
          <h2>
            {translate('background_tasks.scanner_context')}
            {': '}
            {task.componentName}
            {' ['}
            {translate('background_task.type', task.type)}
            {']'}
          </h2>
        </div>

        <div className="modal-body modal-container">
          {scannerContext != null ? (
            <pre className="js-task-scanner-context">{scannerContext}</pre>
          ) : (
            <i className="spinner" />
          )}
        </div>

        <div className="modal-foot">
          <a className="js-modal-close" href="#" onClick={this.handleCloseClick}>
            {translate('close')}
          </a>
        </div>
      </Modal>
    );
  }
}
