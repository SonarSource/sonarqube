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

import { Spinner } from '@sonarsource/echoes-react';
import { Modal } from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import { getTask } from '../../../api/ce';
import { translate } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
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
        body={
          <Spinner isLoading={!isDefined(scannerContext)}>
            <pre className="it__task-scanner-context">{scannerContext}</pre>
          </Spinner>
        }
        headerTitle={`${translate('background_tasks.scanner_context')}: ${
          task.componentName
        } [${translate('background_task.type', task.type)}]`}
        isLarge
        isScrollable
        onClose={this.props.onClose}
        secondaryButtonLabel={translate('close')}
      />
    );
  }
}
