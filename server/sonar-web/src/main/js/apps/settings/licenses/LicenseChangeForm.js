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
import React from 'react';
import Modal from 'react-modal';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default class LicenseChangeForm extends React.PureComponent {
  static propTypes = {
    license: React.PropTypes.object.isRequired,
    onChange: React.PropTypes.func.isRequired
  };

  state = {
    loading: false,
    modalOpen: false
  };

  onClick(e) {
    e.preventDefault();
    e.target.blur();
    this.setState({ modalOpen: true });
  }

  closeModal = () => this.setState({ modalOpen: false });

  handleSubmit = event => {
    event.preventDefault();
    if (this.textarea) {
      const { value } = this.textarea;
      this.setState({ loading: true });
      this.props
        .onChange(value)
        .then(
          () => this.setState({ loading: false, modalOpen: false }),
          () => this.setState({ loading: false })
        );
    }
  };

  handleCancelClick = event => {
    event.preventDefault();
    this.closeModal();
  };

  render() {
    const { license } = this.props;
    const productName = license.name || license.key;

    return (
      <button className="js-change" onClick={e => this.onClick(e)}>
        {translate('update_verb')}

        {this.state.modalOpen &&
          <Modal
            isOpen={true}
            contentLabel="license update"
            className="modal"
            overlayClassName="modal-overlay"
            onRequestClose={this.closeModal}>
            <form onSubmit={this.handleSubmit}>
              <div className="modal-head">
                <h2>{translateWithParameters('licenses.update_license_for_x', productName)}</h2>
              </div>
              <div className="modal-body">
                <label htmlFor="license-input">{translate('licenses.license_input_label')}</label>
                <textarea
                  autoFocus={true}
                  className="width-100 spacer-top"
                  ref={node => (this.textarea = node)}
                  rows="7"
                  id="license-input"
                  defaultValue={license.value}
                />
                <div className="spacer-top note">{translate('licenses.license_input_note')}</div>
              </div>
              <div className="modal-foot">
                {this.state.loading && <i className="js-modal-spinner spinner spacer-right" />}
                <button className="js-modal-submit" disabled={this.state.loading}>
                  {translate('save')}
                </button>
                <a href="#" className="js-modal-close" onClick={this.handleCancelClick}>
                  {translate('cancel')}
                </a>
              </div>
            </form>
          </Modal>}
      </button>
    );
  }
}
