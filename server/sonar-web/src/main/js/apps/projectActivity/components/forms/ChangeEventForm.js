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
// @flow
import React from 'react';
import Modal from '../../../../components/controls/Modal';
import { translate } from '../../../../helpers/l10n';
/*:: import type { Event } from '../../types'; */

/*::
type Props = {
  changeEvent: (event: string, name: string) => Promise<*>,
  changeEventButtonText: string,
  event: Event,
  onClose: () => void
};
*/

/*::
type State = {
  processing: boolean,
  name: string
};
*/

export default class ChangeEventForm extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      processing: false,
      name: props.event.name
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  closeForm = () => {
    if (this.mounted) {
      this.setState({ name: this.props.event.name });
    }
    this.props.onClose();
  };

  changeInput = (e /*: Object */) => {
    if (this.mounted) {
      this.setState({ name: e.target.value });
    }
  };

  stopProcessing = () => {
    if (this.mounted) {
      this.setState({ processing: false });
    }
  };

  stopProcessingAndClose = () => {
    if (this.mounted) {
      this.setState({ processing: false });
    }
    this.props.onClose();
  };

  handleSubmit = (e /*: Object */) => {
    e.preventDefault();
    this.setState({ processing: true });
    this.props
      .changeEvent(this.props.event.key, this.state.name)
      .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  render() {
    const header = translate(this.props.changeEventButtonText);
    return (
      <Modal contentLabel={header} onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <div className="modal-field">
              <label>{translate('name')}</label>
              <input
                value={this.state.name}
                autoFocus={true}
                disabled={this.state.processing}
                type="text"
                onChange={this.changeInput}
              />
            </div>
          </div>

          <footer className="modal-foot">
            {this.state.processing ? (
              <i className="spinner" />
            ) : (
              <div>
                <button type="submit">{translate('change_verb')}</button>
                <button type="reset" className="button-link" onClick={this.closeForm}>
                  {translate('cancel')}
                </button>
              </div>
            )}
          </footer>
        </form>
      </Modal>
    );
  }
}
