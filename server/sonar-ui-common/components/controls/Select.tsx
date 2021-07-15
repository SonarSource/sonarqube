/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import {
  defaultFilterOptions as reactSelectDefaultFilterOptions,
  ReactAsyncSelectProps,
  ReactCreatableSelectProps,
  ReactSelectProps,
} from 'react-select';
import { lazyLoadComponent } from '../lazyLoadComponent';
import { ClearButton } from './buttons';
import './Select.css';

declare module 'react-select' {
  export function defaultFilterOptions(...args: any[]): any;
}

const ReactSelectLib = import('react-select');
const ReactSelect = lazyLoadComponent(() => ReactSelectLib);
const ReactCreatable = lazyLoadComponent(() =>
  ReactSelectLib.then((lib) => ({ default: lib.Creatable }))
);
const ReactAsync = lazyLoadComponent(() => ReactSelectLib.then((lib) => ({ default: lib.Async })));

function renderInput() {
  return <ClearButton className="button-tiny spacer-left text-middle" iconProps={{ size: 12 }} />;
}

interface WithInnerRef {
  innerRef?: (element: React.Component) => void;
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

export const defaultFilterOptions = reactSelectDefaultFilterOptions;

export function Creatable(props: ReactCreatableSelectProps) {
  // ReactSelect doesn't declare `clearRenderer` prop
  const ReactCreatableAny = ReactCreatable as any;
  return <ReactCreatableAny {...props} clearRenderer={renderInput} />;
}

// TODO figure out why `ref` prop is incompatible
export function AsyncSelect(props: ReactAsyncSelectProps & { ref?: any }) {
  return <ReactAsync {...props} />;
}
