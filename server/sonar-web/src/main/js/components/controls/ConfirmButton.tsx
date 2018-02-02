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
import SimpleModal from './SimpleModal';
import DeferredSpinner from '../common/DeferredSpinner';
import { translate } from '../../helpers/l10n';
import { SubmitButton, ResetButtonLink } from '../ui/buttons';

interface Props {
  children: (props: { onClick: () => void }) => React.ReactNode;
  confirmButtonText: string;
  confirmData?: string;
  isDestructive?: boolean;
  modalBody: React.ReactNode;
  modalHeader: string;
  onConfirm: (data?: string) => void | Promise<void>;
}

interface State {
  modal: boolean;
}

export default class ConfirmButton extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { modal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleButtonClick = () => {
    this.setState({ modal: true });
  };

  handleSubmit = () => {
    const result = this.props.onConfirm(this.props.confirmData);
    if (result) {
      return result.then(this.handleCloseModal, () => {});
    } else {
      this.handleCloseModal();
      return undefined;
    }
  };

  handleCloseModal = () => {
    if (this.mounted) {
      this.setState({ modal: false });
    }
  };

  render() {
    const { confirmButtonText, isDestructive, modalBody, modalHeader } = this.props;

    return (
      <>
        {this.props.children({ onClick: this.handleButtonClick })}
        {this.state.modal && (
          <SimpleModal
            header={modalHeader}
            onClose={this.handleCloseModal}
            onSubmit={this.handleSubmit}>
            {({ onCloseClick, onFormSubmit, submitting }) => (
              <form onSubmit={onFormSubmit}>
                <header className="modal-head">
                  <h2>{modalHeader}</h2>
                </header>

                <div className="modal-body">{modalBody}</div>

                <footer className="modal-foot">
                  <DeferredSpinner className="spacer-right" loading={submitting} />
                  <SubmitButton
                    className={isDestructive ? 'button-red' : undefined}
                    disabled={submitting}>
                    {confirmButtonText}
                  </SubmitButton>
                  <ResetButtonLink disabled={submitting} onClick={onCloseClick}>
                    {translate('cancel')}
                  </ResetButtonLink>
                </footer>
              </form>
            )}
          </SimpleModal>
        )}
      </>
    );
  }
}
