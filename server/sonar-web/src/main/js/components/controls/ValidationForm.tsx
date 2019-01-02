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
import { FormikActions, FormikProps, Formik } from 'formik';

export type ChildrenProps<V> = T.Omit<FormikProps<V>, 'handleSubmit'>;

interface Props<V> {
  children: (props: ChildrenProps<V>) => React.ReactNode;
  initialValues: V;
  isInitialValid?: boolean;
  onSubmit: (data: V) => Promise<void>;
  validate: (data: V) => { [P in keyof V]?: string } | Promise<{ [P in keyof V]?: string }>;
}

export default class ValidationForm<V> extends React.Component<Props<V>> {
  mounted = false;

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSubmit = (data: V, { setSubmitting }: FormikActions<V>) => {
    const result = this.props.onSubmit(data);
    const stopSubmitting = () => {
      if (this.mounted) {
        setSubmitting(false);
      }
    };

    if (result) {
      result.then(stopSubmitting, stopSubmitting);
    } else {
      stopSubmitting();
    }
  };

  render() {
    return (
      <Formik<V>
        initialValues={this.props.initialValues}
        isInitialValid={this.props.isInitialValid}
        onSubmit={this.handleSubmit}
        validate={this.props.validate}>
        {({ handleSubmit, ...props }) => (
          <form onSubmit={handleSubmit}>{this.props.children(props)}</form>
        )}
      </Formik>
    );
  }
}
