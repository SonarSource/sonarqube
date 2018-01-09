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
import ExtensionContainer from './ExtensionContainer';
import ExtensionNotFound from './ExtensionNotFound';
import { Component } from '../../types';

interface Props {
  component: Component;
  location: { query: { id: string } };
  params: {
    extensionKey: string;
    pluginKey: string;
  };
}

export default function ProjectPageExtension(props: Props) {
  const { extensionKey, pluginKey } = props.params;
  const { component } = props;
  const extension =
    component.extensions &&
    component.extensions.find(p => p.key === `${pluginKey}/${extensionKey}`);
  return extension ? (
    <ExtensionContainer extension={extension} options={{ component }} />
  ) : (
    <ExtensionNotFound />
  );
}
