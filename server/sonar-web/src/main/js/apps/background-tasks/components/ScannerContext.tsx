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
import { Modal, Spinner } from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { getTask } from '../../../api/ce';
import { translate } from '../../../helpers/l10n';
import { Task } from '../../../types/tasks';

interface Props {
  onClose: () => void;
  task: Pick<Task, 'componentName' | 'id' | 'type'>;
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
    getTask(this.props.task.id, ['scannerContext']).then((task) => {
      if (this.mounted) {
        this.setState({ scannerContext: task.scannerContext });
      }
    }, noop);
  }

  render() {
    const { task } = this.props;
    const { scannerContext } = this.state;

    return (
      <Modal
        onClose={this.props.onClose}
        isLarge
        isScrollable
        headerTitle={
          <FormattedMessage
            id="background_tasks.error_stacktrace.title"
            values={{
              project: task.componentName,
              type: translate('background_task.type', task.type),
            }}
          />
        }
        body={
          <Spinner loading={scannerContext == null}>
            <pre className="js-task-scanner-context">{scannerContext}</pre>
          </Spinner>
        }
        secondaryButtonLabel={translate('close')}
      />
    );
  }
}
