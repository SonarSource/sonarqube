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
  analysis: string,
  deleteEvent: (analysis: string, event: string) => Promise<*>,
  event: Event,
  removeEventButtonText: string,
  removeEventQuestion: string,
  onClose: () => void
};
*/

/*::
type State = {
  processing: boolean
};
*/

export default class RemoveEventForm extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    processing: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  closeForm = () => {
    this.props.onClose();
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
      .deleteEvent(this.props.analysis, this.props.event.key)
      .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  render() {
    const header = translate(this.props.removeEventButtonText);
    return (
      <Modal contentLabel={header} onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">{translate(this.props.removeEventQuestion)}</div>

          <footer className="modal-foot">
            {this.state.processing ? (
              <i className="spinner" />
            ) : (
              <div>
                <button type="submit" className="button-red" autoFocus={true}>
                  {translate('delete')}
                </button>
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
