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
import { setWorkerCount } from '../../../api/ce';
import Modal from '../../../components/controls/Modal';
import Select from '../../../components/controls/Select';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import { Alert } from '../../../components/ui/Alert';

const MAX_WORKERS = 10;

interface Props {
  onClose: (newWorkerCount?: number) => void;
  workerCount: number;
}

interface State {
  newWorkerCount: number;
  submitting: boolean;
}

export default class WorkersForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      newWorkerCount: props.workerCount,
      submitting: false
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClose = () => {
    this.props.onClose();
  };

  handleWorkerCountChange = (option: { value: number }) =>
    this.setState({ newWorkerCount: option.value });

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    const { newWorkerCount } = this.state;
    setWorkerCount(newWorkerCount).then(
      () => {
        if (this.mounted) {
          this.props.onClose(newWorkerCount);
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ submitting: false });
        }
      }
    );
  };

  render() {
    const options = [];
    for (let i = 1; i <= MAX_WORKERS; i++) {
      options.push({ label: String(i), value: i });
    }

    return (
      <Modal
        contentLabel={translate('background_tasks.change_number_of_workers')}
        onRequestClose={this.handleClose}>
        <header className="modal-head">
          <h2>{translate('background_tasks.change_number_of_workers')}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <Select
              className="input-tiny spacer-top"
              clearable={false}
              onChange={this.handleWorkerCountChange}
              options={options}
              searchable={false}
              value={this.state.newWorkerCount}
            />
            <Alert className="big-spacer-top" variant="info">
              {translate('background_tasks.change_number_of_workers.hint')}
            </Alert>
          </div>
          <footer className="modal-foot">
            <div>
              {this.state.submitting && <i className="spinner spacer-right" />}
              <SubmitButton disabled={this.state.submitting}>{translate('save')}</SubmitButton>
              <ResetButtonLink onClick={this.handleClose}>{translate('cancel')}</ResetButtonLink>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }
}
