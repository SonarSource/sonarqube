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
/*:: import type { Analysis } from '../../types'; */

/*::
type Props = {
  addEvent: (analysis: string, name: string, category?: string) => Promise<*>,
  analysis: Analysis,
  addEventButtonText: string,
  onClose: () => void;
};
*/

/*::
type State = {
  processing: boolean,
  name: string
};
*/

export default class AddEventForm extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    processing: false,
    name: ''
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

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

  handleSubmit = (e /*: Object */) => {
    e.preventDefault();
    this.setState({ processing: true });
    this.props
      .addEvent(this.props.analysis.key, this.state.name)
      .then(this.props.onClose, this.stopProcessing);
  };

  render() {
    const header = translate(this.props.addEventButtonText);
    return (
      <Modal contentLabel={header} key="add-event-modal" onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <div className="modal-field">
              <label>{translate('name')}</label>
              <input
                autoFocus={true}
                disabled={this.state.processing}
                onChange={this.changeInput}
                type="text"
                value={this.state.name}
              />
            </div>
          </div>

          <footer className="modal-foot">
            {this.state.processing ? (
              <i className="spinner" />
            ) : (
              <div>
                <button type="submit">{translate('save')}</button>
                <button className="button-link" onClick={this.props.onClose} type="reset">
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
