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
import Modal from 'sonar-ui-common/components/controls/Modal';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getTask } from '../../../api/ce';

interface Props {
  onClose: () => void;
  task: Pick<T.Task, 'componentName' | 'errorMessage' | 'id' | 'type'>;
}

interface State {
  loading: boolean;
  stacktrace?: string;
}

export default class Stacktrace extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.loadStacktrace();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadStacktrace() {
    getTask(this.props.task.id, ['stacktrace']).then(
      task => {
        if (this.mounted) {
          this.setState({ loading: false, stacktrace: task.errorStacktrace });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  handleCloseClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  render() {
    const { task } = this.props;
    const { loading, stacktrace } = this.state;

    return (
      <Modal contentLabel="stacktrace" onRequestClose={this.props.onClose} size="large">
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
          {loading ? (
            <i className="spinner" />
          ) : stacktrace ? (
            <div>
              <h4 className="spacer-bottom">{translate('background_tasks.error_stacktrace')}</h4>
              <pre className="js-task-stacktrace">{stacktrace}</pre>
            </div>
          ) : (
            <div>
              <h4 className="spacer-bottom">{translate('background_tasks.error_message')}</h4>
              <pre className="js-task-error-message">{task.errorMessage}</pre>
            </div>
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
