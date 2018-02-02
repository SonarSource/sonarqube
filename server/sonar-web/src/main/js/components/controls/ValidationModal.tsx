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
import { withFormik, Form, FormikActions, FormikProps } from 'formik';
import Modal from './Modal';
import DeferredSpinner from '../common/DeferredSpinner';
import { translate } from '../../helpers/l10n';

interface InnerFormProps<Values> {
  children: (props: FormikProps<Values>) => React.ReactNode;
  confirmButtonText: string;
  header: string;
  initialValues: Values;
}

interface Props<Values> extends InnerFormProps<Values> {
  isInitialValid?: boolean;
  onClose: () => void;
  validate: (data: Values) => void | object | Promise<object>;
  onSubmit: (data: Values) => void | Promise<void>;
}

export default class ValidationModal<Values> extends React.PureComponent<Props<Values>> {
  handleCancelClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClose();
  };

  handleSubmit = (data: Values, { setSubmitting }: FormikActions<Values>) => {
    const result = this.props.onSubmit(data);
    if (result) {
      result.then(
        () => {
          setSubmitting(false);
          this.props.onClose();
        },
        () => {
          setSubmitting(false);
        }
      );
    } else {
      setSubmitting(false);
      this.props.onClose();
    }
  };

  render() {
    const { header } = this.props;

    const InnerForm = withFormik<InnerFormProps<Values>, Values>({
      handleSubmit: this.handleSubmit,
      isInitialValid: this.props.isInitialValid,
      mapPropsToValues: props => props.initialValues,
      validate: this.props.validate
    })(props => (
      <Form>
        <div className="modal-head">
          <h2>{props.header}</h2>
        </div>

        <div className="modal-body">{props.children(props)}</div>

        <footer className="modal-foot">
          <DeferredSpinner className="spacer-right" loading={props.isSubmitting} />
          <button disabled={props.isSubmitting || !props.isValid || !props.dirty} type="submit">
            {props.confirmButtonText}
          </button>
          <button
            className="button-link"
            disabled={props.isSubmitting}
            onClick={this.handleCancelClick}
            type="reset">
            {translate('cancel')}
          </button>
        </footer>
      </Form>
    ));

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <InnerForm
          confirmButtonText={this.props.confirmButtonText}
          header={header}
          initialValues={this.props.initialValues}>
          {this.props.children}
        </InnerForm>
      </Modal>
    );
  }
}
