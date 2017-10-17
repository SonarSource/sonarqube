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
import * as React from 'react';
import Modal from 'react-modal';
import LicenseEditionSet from './LicenseEditionSet';
import { Edition, EditionStatus, applyLicense } from '../../../api/marketplace';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  edition: Edition;
  onClose: () => void;
  updateEditionStatus: (editionStatus: EditionStatus) => void;
}

interface State {
  license: string;
  loading: boolean;
  status?: string;
}

export default class LicenseEditionForm extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { license: '', loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleLicenseChange = (license: string, status?: string) => {
    if (this.mounted) {
      this.setState({ license, status });
    }
  };

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleConfirmClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    const { license, status } = this.state;
    if (license && status && ['AUTOMATIC_INSTALL', 'NO_INSTALL'].includes(status)) {
      this.setState({ loading: true });
      applyLicense({ license }).then(
        editionStatus => {
          this.props.updateEditionStatus(editionStatus);
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
    const { edition } = this.props;
    const { status } = this.state;
    const header = translateWithParameters('marketplace.install_x', edition.name);
    return (
      <Modal
        isOpen={true}
        contentLabel={header}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <LicenseEditionSet
          className="modal-body"
          edition={edition}
          updateLicense={this.handleLicenseChange}
        />

        <footer className="modal-foot">
          {this.state.loading && <i className="spinner spacer-right" />}
          {status &&
          ['NO_INSTALL', 'AUTOMATIC_INSTALL'].includes(status) && (
            <button className="js-confirm" onClick={this.handleConfirmClick}>
              {status === 'NO_INSTALL' ? translate('save') : translate('marketplace.install')}
            </button>
          )}
          <a className="js-modal-close" href="#" onClick={this.handleCancelClick}>
            {translate('cancel')}
          </a>
        </footer>
      </Modal>
    );
  }
}
