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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import ExtensionContainer from './ExtensionContainer';
import ExtensionNotFound from './ExtensionNotFound';
import { getAppState } from '../../../store/rootReducer';

/*::
type Props = {
  adminPages: Array<{ key: string }>,
  params: {
    extensionKey: string,
    pluginKey: string
  }
};
*/

function GlobalAdminPageExtension(props /*: Props */) {
  const { extensionKey, pluginKey } = props.params;
  const extension = props.adminPages.find(p => p.key === `${pluginKey}/${extensionKey}`);
  return extension ? <ExtensionContainer extension={extension} /> : <ExtensionNotFound />;
}

const mapStateToProps = state => ({
  adminPages: getAppState(state).adminPages
});

export default connect(mapStateToProps)(GlobalAdminPageExtension);
