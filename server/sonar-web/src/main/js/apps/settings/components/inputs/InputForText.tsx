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

import * as React from 'react';
import { InputTextArea } from '~design-system';
import { DefaultSpecializedInputProps, getPropertyName } from '../../utils';

interface Props extends DefaultSpecializedInputProps {
  innerRef: React.ForwardedRef<HTMLTextAreaElement>;
}

class InputForText extends React.PureComponent<Props> {
  handleInputChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.props.onChange(event.target.value);
  };

  render() {
    const { setting, name, innerRef, value } = this.props;
    return (
      <InputTextArea
        size="large"
        name={name}
        onChange={this.handleInputChange}
        ref={innerRef}
        rows={5}
        value={value || ''}
        aria-label={getPropertyName(setting.definition)}
      />
    );
  }
}

export default React.forwardRef(
  (props: DefaultSpecializedInputProps, ref: React.ForwardedRef<HTMLTextAreaElement>) => (
    <InputForText innerRef={ref} {...props} />
  ),
);
