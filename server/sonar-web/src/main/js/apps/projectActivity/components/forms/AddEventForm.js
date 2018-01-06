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
import { ActionsDropdownItem } from '../../../../components/controls/ActionsDropdown';
import { translate } from '../../../../helpers/l10n';
/*:: import type { Analysis } from '../../types'; */

/*::
type Props = {
  addEvent: (analysis: string, name: string, category?: string) => Promise<*>,
  analysis: Analysis,
  addEventButtonText: string
};
*/

/*::
type State = {
  open: boolean,
  processing: boolean,
  name: string
};
*/

export default class AddEventForm extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    open: false,
    processing: false,
    name: ''
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  openForm = () => {
    this.setState({ open: true });
  };

  closeForm = () => {
    if (this.mounted) {
      this.setState({ open: false, name: '' });
    }
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
      this.setState({ open: false, processing: false, name: '' });
    }
  };

  handleSubmit = (e /*: Object */) => {
    e.preventDefault();
    this.setState({ processing: true });
    this.props
      .addEvent(this.props.analysis.key, this.state.name)
      .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  renderModal() {
    const header = translate(this.props.addEventButtonText);
    return (
      <Modal key="add-event-modal" contentLabel={header} onRequestClose={this.closeForm}>
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
                <button type="submit">{translate('save')}</button>
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

  render() {
    const linkComponent = (
      <ActionsDropdownItem className="js-add-event" onClick={this.openForm}>
        {translate(this.props.addEventButtonText)}
      </ActionsDropdownItem>
    );
    if (this.state.open) {
      return [linkComponent, this.renderModal()];
    }
    return linkComponent;
  }
}
