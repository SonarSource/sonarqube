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
import ReactSelect, {
  Creatable as ReactCreatable,
  Async,
  ReactSelectProps,
  ReactCreatableSelectProps,
  ReactAsyncSelectProps
} from 'react-select';
import * as theme from '../../app/theme';
import ClearIcon from '../icons-components/ClearIcon';
import { ButtonIcon } from '../ui/buttons';
import './react-select.css';

function renderInput() {
  return (
    <ButtonIcon className="button-tiny spacer-left text-middle" color={theme.gray60}>
      <ClearIcon size={12} />
    </ButtonIcon>
  );
}

interface WithInnerRef {
  innerRef?: (element: ReactSelect) => void;
}

export default function Select({ innerRef, ...props }: WithInnerRef & ReactSelectProps) {
  // TODO try to define good defaults, if any
  // ReactSelect doesn't declare `clearRenderer` prop
  const ReactSelectAny = ReactSelect as any;
  // hide the "x" icon when select is empty
  const clearable = props.clearable ? Boolean(props.value) : false;
  return (
    <ReactSelectAny {...props} clearable={clearable} clearRenderer={renderInput} ref={innerRef} />
  );
}

export function Creatable(props: ReactCreatableSelectProps) {
  return <ReactCreatable {...props} />;
}

// TODO figure out why `ref` prop is incompatible
export function AsyncSelect(props: ReactAsyncSelectProps & { ref: any }) {
  return <Async {...props} />;
}
