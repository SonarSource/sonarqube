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
import $ from 'jquery';
import ActionOptionsView from '../../common/action-options-view';
import Template from '../templates/issue-transitions-form.hbs';

export default ActionOptionsView.extend({
  template: Template,

  selectInitialOption() {
    this.makeActive(this.getOptions().first());
  },

  selectOption(e) {
    const transition = $(e.currentTarget).data('value');
    this.submit(transition);
    return ActionOptionsView.prototype.selectOption.apply(this, arguments);
  },

  submit(transition) {
    return this.model.transition(transition);
  }
});
