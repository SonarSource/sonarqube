/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import ReactSelectClass, {
  ReactAsyncSelectProps,
  ReactCreatableSelectProps,
  ReactSelectProps
} from 'react-select-legacy';
import { lazyLoadComponent } from '../lazyLoadComponent';
import { ClearButton } from './buttons';
import './SelectLegacy.css';

const ReactSelectLib = import('react-select-legacy');
const ReactSelect = lazyLoadComponent(() => ReactSelectLib);
const ReactCreatable = lazyLoadComponent(() =>
  ReactSelectLib.then(lib => ({ default: lib.Creatable }))
);
const ReactAsync = lazyLoadComponent(() => ReactSelectLib.then(lib => ({ default: lib.Async })));

function renderInput() {
  return <ClearButton className="button-tiny spacer-left text-middle" iconProps={{ size: 12 }} />;
}

export interface WithInnerRef {
  innerRef?: React.Ref<ReactSelectClass<unknown>>;
}

export default function SelectLegacy({ innerRef, ...props }: WithInnerRef & ReactSelectProps) {
  // hide the "x" icon when select is empty
  const clearable = props.clearable ? Boolean(props.value) : false;
  return (
    <ReactSelect {...props} clearable={clearable} clearRenderer={renderInput} ref={innerRef} />
  );
}

export function CreatableLegacy(props: ReactCreatableSelectProps) {
  return <ReactCreatable {...props} clearRenderer={renderInput} />;
}

export function AsyncSelectLegacy(props: ReactAsyncSelectProps & WithInnerRef) {
  return <ReactAsync {...props} />;
}
