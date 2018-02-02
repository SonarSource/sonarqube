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
import { stringify } from 'querystring';
import Template from './templates/source-viewer-line-options-popup.hbs';
import Popup from '../../common/popup';
import { getBaseUrl } from '../../../helpers/urls';
import { omitNil } from '../../../helpers/request';

export default Popup.extend({
  template: Template,

  serializeData() {
    const { component, line, branchLike } = this.options;
    const query = { id: component.key, line, ...getBranchLikeQuery(branchLike) };
    return { permalink: getBaseUrl() + '/component?' + stringify(omitNil(query)) };
  }
});
