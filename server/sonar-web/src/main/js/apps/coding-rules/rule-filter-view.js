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
import $ from 'jquery';
import { union } from 'lodash';
import ActionOptionsView from '../../components/common/action-options-view';
import Template from './templates/coding-rules-rule-filter-form.hbs';

export default ActionOptionsView.extend({
  template: Template,

  selectOption(e) {
    const property = $(e.currentTarget).data('property');
    const value = $(e.currentTarget).data('value');
    this.trigger('select', property, value);
    return ActionOptionsView.prototype.selectOption.apply(this, arguments);
  },

  serializeData() {
    return {
      ...ActionOptionsView.prototype.serializeData.apply(this, arguments),
      tags: union(this.model.get('sysTags'), this.model.get('tags'))
    };
  }
});
