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
import AlmPRDecorationFormModalRenderer from './AlmPRDecorationFormModalRenderer';

interface Props {
  alm: string;
  data: T.GithubDefinition;
  onCancel: () => void;
  onSubmit: (data: T.GithubDefinition, originalKey: string) => void;
}

interface State {
  formData: T.GithubDefinition;
}

export default class AlmPRDecorationFormModal extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = { formData: props.data };
  }

  handleFieldChange = (fieldId: keyof T.GithubDefinition, value: string) => {
    this.setState(({ formData }) => ({
      formData: {
        ...formData,
        [fieldId]: value
      }
    }));
  };

  handleFormSubmit = () => {
    this.props.onSubmit(this.state.formData, this.props.data.key);
  };

  canSubmit = () => {
    return Object.values(this.state.formData).reduce(
      (result, value) => result && value.length > 0,
      true
    );
  };

  render() {
    const { alm, data } = this.props;
    const { formData } = this.state;

    return (
      <AlmPRDecorationFormModalRenderer
        alm={alm}
        canSubmit={this.canSubmit}
        formData={formData}
        onCancel={this.props.onCancel}
        onFieldChange={this.handleFieldChange}
        onSubmit={this.handleFormSubmit}
        originalKey={data.key}
      />
    );
  }
}
