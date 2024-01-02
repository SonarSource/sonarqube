/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { ButtonPrimary, ButtonSecondary, Modal } from 'design-system';
import { FormikValues } from 'formik';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import ValidationForm, { ChildrenProps } from './ValidationForm';

interface Props<V> {
  children: (props: ChildrenProps<V>) => React.ReactNode;
  confirmButtonText: string;
  header: string;
  initialValues: V;
  onClose: () => void;
  onSubmit: (data: V) => Promise<void>;
  validate: (data: V) => { [P in keyof V]?: string };
}

export default class ValidationModal<V extends FormikValues> extends React.PureComponent<Props<V>> {
  handleSubmit = (data: V) => {
    return this.props.onSubmit(data).then(() => {
      this.props.onClose();
    });
  };

  render() {
    return (
      <Modal onClose={this.props.onClose}>
        <ValidationForm
          initialValues={this.props.initialValues}
          onSubmit={this.handleSubmit}
          validate={this.props.validate}
        >
          {(formState) => (
            <>
              <Modal.Header title={this.props.header} />
              <div className="sw-py-4">{this.props.children(formState)}</div>
              <Modal.Footer
                loading={formState.isSubmitting}
                primaryButton={
                  <ButtonPrimary
                    type="submit"
                    disabled={formState.isSubmitting || !formState.isValid || !formState.dirty}
                  >
                    {this.props.confirmButtonText}
                  </ButtonPrimary>
                }
                secondaryButton={
                  <ButtonSecondary
                    className="sw-ml-2"
                    disabled={formState.isSubmitting}
                    onClick={this.props.onClose}
                  >
                    {translate('cancel')}
                  </ButtonSecondary>
                }
              />
            </>
          )}
        </ValidationForm>
      </Modal>
    );
  }
}
