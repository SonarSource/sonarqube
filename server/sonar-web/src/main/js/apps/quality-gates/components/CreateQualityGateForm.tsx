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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import Modal from '../../../components/controls/Modal';
import { createQualityGate, QualityGate } from '../../../api/quality-gates';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';

interface Props {
  onCreate: (qualityGate: QualityGate) => void;
  onClose: () => void;
  organization?: string;
}

interface State {
  loading: boolean;
  name: string;
}

export default class CreateQualityGateForm extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object
  };

  state = { loading: false, name: '' };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { organization } = this.props;
    const { name } = this.state;
    if (name) {
      this.setState({ loading: true });
      createQualityGate({ name, organization }).then(
        qualityGate => {
          this.props.onCreate(qualityGate);
          this.context.router.push(getQualityGateUrl(String(qualityGate.id), organization));
          this.props.onClose();
        },
        () => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        }
      );
    }
  };

  render() {
    const { loading, name } = this.state;
    const header = translate('quality_gates.rename');
    const submitDisabled = loading || !name;

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="quality-gate-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            <div className="modal-field">
              <label htmlFor="quality-gate-form-name">
                {translate('name')}
                <em className="mandatory">*</em>
              </label>
              <input
                autoFocus={true}
                id="quality-gate-form-name"
                maxLength={100}
                onChange={this.handleNameChange}
                required={true}
                size={50}
                type="text"
                value={name}
              />
            </div>
          </div>
          <div className="modal-foot">
            {loading && <i className="spinner spacer-right" />}
            <button disabled={submitDisabled} className="js-confirm">
              {translate('save')}
            </button>
            <a href="#" className="js-modal-close" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </div>
        </form>
      </Modal>
    );
  }
}
