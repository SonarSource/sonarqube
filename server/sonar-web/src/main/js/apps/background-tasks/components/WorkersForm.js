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
import Select from 'react-select';
import { times } from 'lodash';
import { setWorkerCount } from '../../../api/ce';
import { translate } from '../../../helpers/l10n';

const MAX_WORKERS = 10;

/*::
type Props = {
  onClose: (newWorkerCount?: number) => void,
  workerCount: number
};
*/

/*::
type State = {
  newWorkerCount: number,
  submitting: boolean
};
*/

export default class WorkersForm extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
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

  handleClose = () => this.props.onClose();

  handleWorkerCountChange = (option /*: { value: number } */) =>
    this.setState({ newWorkerCount: option.value });

  handleSubmit = (event /*: Event */) => {
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
    const options = times(MAX_WORKERS).map((_, i) => ({ label: i + 1, value: i + 1 }));

    return (
      <Modal
        isOpen={true}
        contentLabel={translate('background_tasks.change_number_of_workers')}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.handleClose}>
        <header className="modal-head">
          <h2>
            {translate('background_tasks.change_number_of_workers')}
          </h2>
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
            <div className="big-spacer-top alert alert-success markdown">
              {translate('background_tasks.change_number_of_workers.hint')}
            </div>
          </div>
          <footer className="modal-foot">
            <div>
              {this.state.submitting && <i className="spinner spacer-right" />}
              <button disabled={this.state.submitting} type="submit">
                {translate('save')}
              </button>
              <button type="reset" className="button-link" onClick={this.handleClose}>
                {translate('cancel')}
              </button>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }
}
