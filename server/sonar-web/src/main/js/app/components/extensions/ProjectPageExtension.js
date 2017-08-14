/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Extension from './Extension';
import ExtensionNotFound from './ExtensionNotFound';
import { getComponent } from '../../../store/rootReducer';
import { addGlobalErrorMessage } from '../../../store/globalMessages/duck';

/*::
type Props = {
  component: {
    extensions: Array<{ key: string }>
  },
  location: { query: { id: string } },
  params: {
    extensionKey: string,
    pluginKey: string
  }
};
*/

function ProjectPageExtension(props /*: Props */) {
  const { extensionKey, pluginKey } = props.params;
  const { component } = props;
  const extension = component.extensions.find(p => p.key === `${pluginKey}/${extensionKey}`);
  return extension
    ? <Extension extension={extension} options={{ component }} />
    : <ExtensionNotFound />;
}

const mapStateToProps = (state, ownProps /*: Props */) => ({
  component: getComponent(state, ownProps.location.query.id)
});

const mapDispatchToProps = { onFail: addGlobalErrorMessage };

export default connect(mapStateToProps, mapDispatchToProps)(ProjectPageExtension);
