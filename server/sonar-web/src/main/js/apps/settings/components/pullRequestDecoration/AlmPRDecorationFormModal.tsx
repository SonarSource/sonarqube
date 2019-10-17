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

interface ChildrenProps<AlmBindingDefinitionType> {
  formData: AlmBindingDefinitionType;
  onFieldChange: (fieldId: keyof AlmBindingDefinitionType, value: string) => void;
}

interface Props<B> {
  children: (props: ChildrenProps<B>) => React.ReactNode;
  bindingDefinition: B;
  onCancel: () => void;
  onSubmit: (data: B, originalKey: string) => void;
}

interface State<AlmBindingDefinitionType> {
  formData: AlmBindingDefinitionType;
}

export default class AlmPRDecorationFormModal<
  B extends T.AlmSettingsBinding
> extends React.PureComponent<Props<B>, State<B>> {
  constructor(props: Props<B>) {
    super(props);

    this.state = { formData: props.bindingDefinition };
  }

  handleFieldChange = (fieldId: keyof B, value: string) => {
    this.setState(({ formData }) => ({
      formData: {
        ...formData,
        [fieldId]: value
      }
    }));
  };

  handleFormSubmit = () => {
    this.props.onSubmit(this.state.formData, this.props.bindingDefinition.key);
  };

  canSubmit = () => {
    return Object.values(this.state.formData).reduce(
      (result, value) => result && value.length > 0,
      true
    );
  };

  render() {
    const { children, bindingDefinition } = this.props;
    const { formData } = this.state;

    return (
      <AlmPRDecorationFormModalRenderer
        canSubmit={this.canSubmit}
        onCancel={this.props.onCancel}
        onSubmit={this.handleFormSubmit}
        originalKey={bindingDefinition.key}>
        {children({
          formData,
          onFieldChange: this.handleFieldChange
        })}
      </AlmPRDecorationFormModalRenderer>
    );
  }
}
