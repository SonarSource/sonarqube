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
import { InputField } from 'design-system';
import * as React from 'react';
import { KeyboardKeys } from '../../../../helpers/keycodes';
import { DefaultSpecializedInputProps, getPropertyName } from '../../utils';

interface SimpleInputProps extends DefaultSpecializedInputProps {
  value: string | number;
}

type InternalProps = SimpleInputProps & {
  innerRef: React.ForwardedRef<HTMLInputElement>;
};

class SimpleInput extends React.PureComponent<InternalProps> {
  handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.props.onChange(event.currentTarget.value);
  };

  handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.nativeEvent.key === KeyboardKeys.Enter && this.props.onSave) {
      this.props.onSave();
    } else if (event.nativeEvent.key === KeyboardKeys.Escape && this.props.onCancel) {
      this.props.onCancel();
    }
  };

  render() {
    const {
      ariaDescribedBy,
      autoComplete,
      autoFocus,
      className,
      index,
      innerRef,
      isInvalid,
      name,
      value = '',
      setting,
      size,
      type,
    } = this.props;

    let label = getPropertyName(setting.definition);
    if (typeof index === 'number') {
      label = label.concat(` - ${index + 1}`);
    }

    return (
      <InputField
        aria-describedby={ariaDescribedBy}
        id={`input-${name}-${index}`}
        isInvalid={isInvalid}
        autoComplete={autoComplete}
        autoFocus={autoFocus}
        className={className}
        name={name}
        onChange={this.handleInputChange}
        onKeyDown={this.handleKeyDown}
        ref={innerRef}
        type={type}
        value={value}
        size={size}
        aria-label={label}
      />
    );
  }
}

export default React.forwardRef(
  (props: SimpleInputProps, ref: React.ForwardedRef<HTMLInputElement>) => (
    <SimpleInput innerRef={ref} {...props} />
  ),
);
