/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import SimpleModal, { ChildrenProps } from './SimpleModal';
import DeferredSpinner from '../common/DeferredSpinner';
import { translate } from '../../helpers/l10n';
import { SubmitButton, ResetButtonLink } from '../ui/buttons';

interface Props<T> {
  children: React.ReactNode;
  cancelButtonText?: string;
  confirmButtonText: string;
  confirmData?: T;
  confirmDisable?: boolean;
  header: string;
  isDestructive?: boolean;
  onClose: () => void;
  onConfirm: (data?: T) => void | Promise<void>;
}

export default class ConfirmModal<T = string> extends React.PureComponent<Props<T>> {
  mounted = false;

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSubmit = () => {
    const result = this.props.onConfirm(this.props.confirmData);
    if (result) {
      return result.then(this.props.onClose, () => {});
    } else {
      this.props.onClose();
      return undefined;
    }
  };

  renderModalContent = ({ onCloseClick, onFormSubmit, submitting }: ChildrenProps) => {
    const { children, confirmButtonText, confirmDisable, header, isDestructive } = this.props;
    const { cancelButtonText = translate('cancel') } = this.props;
    return (
      <form onSubmit={onFormSubmit}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <div className="modal-body">{children}</div>
        <footer className="modal-foot">
          <DeferredSpinner className="spacer-right" loading={submitting} />
          <SubmitButton
            autoFocus={true}
            className={isDestructive ? 'button-red' : undefined}
            disabled={submitting || confirmDisable}>
            {confirmButtonText}
          </SubmitButton>
          <ResetButtonLink disabled={submitting} onClick={onCloseClick}>
            {cancelButtonText}
          </ResetButtonLink>
        </footer>
      </form>
    );
  };

  render() {
    const { header } = this.props;
    return (
      <SimpleModal header={header} onClose={this.props.onClose} onSubmit={this.handleSubmit}>
        {this.renderModalContent}
      </SimpleModal>
    );
  }
}
