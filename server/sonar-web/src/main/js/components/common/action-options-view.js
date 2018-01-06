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
import key from 'keymaster';
import PopupView from './popup';

export default PopupView.extend({
  className: 'bubble-popup bubble-popup-menu',
  keyScope: 'action-options',

  ui: {
    options: '.menu > li > a'
  },

  events() {
    return {
      'click @ui.options': 'selectOption',
      'mouseenter @ui.options': 'activateOptionByPointer'
    };
  },

  initialize() {
    this.bindShortcuts();
  },

  onRender() {
    PopupView.prototype.onRender.apply(this, arguments);
    this.selectInitialOption();
  },

  getOptions() {
    return this.$('.menu > li > a');
  },

  getActiveOption() {
    return this.getOptions().filter('.active');
  },

  makeActive(option) {
    if (option.length > 0) {
      this.getOptions()
        .removeClass('active')
        .tooltip('hide');
      option.addClass('active').tooltip('show');
    }
  },

  selectInitialOption() {
    this.makeActive(this.getOptions().first());
  },

  selectNextOption() {
    this.makeActive(
      this.getActiveOption()
        .parent()
        .nextAll('li:not(.divider)')
        .first()
        .children('a')
    );
    return false;
  },

  selectPreviousOption() {
    this.makeActive(
      this.getActiveOption()
        .parent()
        .prevAll('li:not(.divider)')
        .first()
        .children('a')
    );
    return false;
  },

  activateOptionByPointer(e) {
    this.makeActive($(e.currentTarget));
  },

  bindShortcuts() {
    const that = this;
    this.currentKeyScope = key.getScope();
    key.setScope(this.keyScope);
    key('down', this.keyScope, () => that.selectNextOption());
    key('up', this.keyScope, () => that.selectPreviousOption());
    key('return', this.keyScope, () => that.selectActiveOption());
    key('escape', this.keyScope, () => that.destroy());
    key('backspace', this.keyScope, () => false);
    key('shift+tab', this.keyScope, () => false);
  },

  unbindShortcuts() {
    key.unbind('down', this.keyScope);
    key.unbind('up', this.keyScope);
    key.unbind('return', this.keyScope);
    key.unbind('escape', this.keyScope);
    key.unbind('backspace', this.keyScope);
    key.unbind('tab', this.keyScope);
    key.unbind('shift+tab', this.keyScope);
    key.setScope(this.currentKeyScope);
  },

  onDestroy() {
    PopupView.prototype.onDestroy.apply(this, arguments);
    this.unbindShortcuts();
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
    $('.tooltip').remove();
  },

  selectOption(e) {
    e.preventDefault();
    this.destroy();
  },

  selectActiveOption() {
    this.getActiveOption().click();
  }
});
