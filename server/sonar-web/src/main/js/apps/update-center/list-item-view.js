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
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import PluginChangelogView from './plugin-changelog-view';
import Template from './templates/update-center-plugin.hbs';
import SystemTemplate from './templates/update-center-system-update.hbs';

export default Marionette.ItemView.extend({
  tagName: 'li',
  className: 'panel panel-vertical',
  template: Template,
  systemTemplate: SystemTemplate,

  modelEvents: {
    'change:_hidden': 'toggleDisplay',
    'change': 'onModelChange',
    'request': 'onRequest'
  },

  events: {
    'click .js-changelog': 'onChangelogClick',
    'click .js-install': 'install',
    'click .js-update': 'update',
    'click .js-uninstall': 'uninstall',
    'change .js-terms': 'onTermsChange',
    'click .js-plugin-category': 'onCategoryClick'
  },

  getTemplate() {
    return this.model.get('_system') ? this.systemTemplate : this.template;
  },

  onRender() {
    this.$el.attr('data-id', this.model.id);
    if (this.model.get('_system')) {
      this.$el.attr('data-system', '');
    }
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onModelChange() {
    if (!this.model.hasChanged('_hidden')) {
      this.render();
    }
  },

  onChangelogClick(e) {
    e.preventDefault();
    e.stopPropagation();
    $('body').click();
    const index = $(e.currentTarget).data('idx');

    // if show changelog of update, show details of this update
    // otherwise show changelog of the available release
    const update = this.model.has('release')
      ? this.model.toJSON()
      : this.model.get('updates')[index];
    const popup = new PluginChangelogView({
      triggerEl: $(e.currentTarget),
      model: new Backbone.Model(update)
    });
    popup.render();
  },

  onRequest() {
    this.$('.js-actions').addClass('hidden');
    this.$('.js-spinner').removeClass('hidden');
  },

  toggleDisplay() {
    this.$el.toggleClass('hidden', this.model.get('_hidden'));
  },

  install() {
    this.model.install();
  },

  update() {
    this.model.update();
  },

  uninstall() {
    this.model.uninstall();
  },

  onTermsChange() {
    const isAccepted = this.$('.js-terms').is(':checked');
    this.$('.js-install').prop('disabled', !isAccepted);
  },

  onCategoryClick(e) {
    e.preventDefault();
    this.model.trigger('filter', this.model);
  }
});
