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
import Popup from '../../common/popup';
import Template from './templates/source-viewer-line-options-popup.hbs';

export default Popup.extend({
  template: Template,

  events: {
    'click .js-get-permalink': 'getPermalink'
  },

  getPermalink(e) {
    e.preventDefault();
    const { component, line } = this.options;
    const url = window.baseUrl +
      `/component/index?id=${encodeURIComponent(component.key)}&line=${line}`;
    const windowParams = 'resizable=1,scrollbars=1,status=1';
    window.open(url, component.name, windowParams);
  }
});
