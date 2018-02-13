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
import LicenseEditionSet from './LicenseEditionSet';
import { Edition, EditionStatus, applyLicense } from '../../../api/marketplace';
import Modal from '../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  edition: Edition;
  editions: Edition[];
  isDowngrade: boolean;
  onClose: () => void;
  updateEditionStatus: (editionStatus: EditionStatus) => void;
}

interface State {
  license: string;
  status?: string;
  submitting: boolean;
}

export default class LicenseEditionForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { license: '', submitting: false };

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
    if (license && status) {
      this.setState({ submitting: true });
      applyLicense({ license }).then(
        editionStatus => {
          this.props.updateEditionStatus(editionStatus);
          this.props.onClose();
        },
        () => {
          if (this.mounted) {
            this.setState({ submitting: false });
          }
        }
      );
    }
  };

  render() {
    const { edition, isDowngrade } = this.props;
    const { license, submitting, status } = this.state;

    const header = isDowngrade
      ? translateWithParameters('marketplace.downgrade_to_x', edition.name)
      : translateWithParameters('marketplace.upgrade_to_x', edition.name);
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <LicenseEditionSet
          className="modal-body"
          edition={edition}
          editions={this.props.editions}
          updateLicense={this.handleLicenseChange}
        />

        <footer className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          {status && (
            <button
              className="js-confirm"
              onClick={this.handleConfirmClick}
              disabled={!license || submitting}>
              {status === 'AUTOMATIC_INSTALL'
                ? translate('marketplace.install')
                : translate('save')}
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
