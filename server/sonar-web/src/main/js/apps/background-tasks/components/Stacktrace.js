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
// @flow
import React from 'react';
import Modal from 'react-modal';
import { getTask } from '../../../api/ce';
import { translate } from '../../../helpers/l10n';

type Props = {
  onClose: () => void,
  task: { componentName: string, errorMessage: string, id: string, type: string }
};

type State = {
  loading: boolean,
  stacktrace: ?string
};

export default class Stacktrace extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = {
    loading: true,
    stacktrace: null
  };

  componentDidMount() {
    this.mounted = true;
    this.loadStacktrace();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadStacktrace() {
    getTask(this.props.task.id, ['stacktrace']).then(task => {
      if (this.mounted) {
        this.setState({ loading: false, stacktrace: task.errorStacktrace });
      }
    });
  }

  handleCloseClick = (event: Event) => {
    event.preventDefault();
    this.props.onClose();
  };

  render() {
    const { task } = this.props;
    const { loading, stacktrace } = this.state;

    return (
      <Modal
        isOpen={true}
        contentLabel="stacktrace"
        className="modal modal-large"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>

        <div className="modal-head">
          <h2>
            {translate('background_tasks.error_stacktrace')}
            {': '}
            {task.componentName}
            {' ['}
            {translate('background_task.type', task.type)}
            {']'}
          </h2>
        </div>

        <div className="modal-body modal-container">
          {loading
            ? <i className="spinner" />
            : stacktrace
                ? <div>
                    <h4 className="spacer-bottom">
                      {translate('background_tasks.error_stacktrace')}
                    </h4>
                    <pre className="js-task-stacktrace">{stacktrace}</pre>
                  </div>
                : <div>
                    <h4 className="spacer-bottom">{translate('background_tasks.error_message')}</h4>
                    <pre className="js-task-error-message">{task.errorMessage}</pre>
                  </div>}
        </div>

        <div className="modal-foot">
          <a href="#" className="js-modal-close" onClick={this.handleCloseClick}>
            {translate('close')}
          </a>
        </div>

      </Modal>
    );
  }
}
