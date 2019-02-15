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
import Modal, { ModalProps } from './Modal';
import ValidationForm, { ChildrenProps } from './ValidationForm';
import DeferredSpinner from '../common/DeferredSpinner';
import { SubmitButton, ResetButtonLink } from '../ui/buttons';
import { translate } from '../../helpers/l10n';

interface Props<V> extends ModalProps {
  children: (props: ChildrenProps<V>) => React.ReactNode;
  confirmButtonText: string;
  header: string;
  initialValues: V;
  isInitialValid?: boolean;
  onClose: () => void;
  onSubmit: (data: V) => Promise<void>;
  validate: (data: V) => { [P in keyof V]?: string };
}

export default class ValidationModal<V> extends React.PureComponent<Props<V>> {
  handleSubmit = (data: V) => {
    return this.props.onSubmit(data).then(() => {
      this.props.onClose();
    });
  };

  render() {
    return (
      <Modal
        contentLabel={this.props.header}
        noBackdrop={this.props.noBackdrop}
        onRequestClose={this.props.onClose}
        size={this.props.size}>
        <ValidationForm
          initialValues={this.props.initialValues}
          isInitialValid={this.props.isInitialValid}
          onSubmit={this.handleSubmit}
          validate={this.props.validate}>
          {props => (
            <>
              <header className="modal-head">
                <h2>{this.props.header}</h2>
              </header>

              <div className="modal-body">{this.props.children(props)}</div>

              <footer className="modal-foot">
                <DeferredSpinner className="spacer-right" loading={props.isSubmitting} />
                <SubmitButton disabled={props.isSubmitting || !props.isValid || !props.dirty}>
                  {this.props.confirmButtonText}
                </SubmitButton>
                <ResetButtonLink disabled={props.isSubmitting} onClick={this.props.onClose}>
                  {translate('cancel')}
                </ResetButtonLink>
              </footer>
            </>
          )}
        </ValidationForm>
      </Modal>
    );
  }
}
