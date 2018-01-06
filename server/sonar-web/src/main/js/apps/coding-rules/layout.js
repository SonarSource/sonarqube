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
import Marionette from 'backbone.marionette';
import Template from './templates/coding-rules-layout.hbs';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    facetsRegion: '.layout-page-filters',
    workspaceHeaderRegion: '.coding-rules-header',
    workspaceListRegion: '.coding-rules-list',
    workspaceDetailsRegion: '.coding-rules-details'
  },

  onRender() {
    const navigator = this.$('.layout-page');
    const top = navigator.offset().top;
    this.$('.layout-page-side').css({ top });
  },

  showDetails() {
    this.scroll = $(window).scrollTop();
    this.$('.coding-rules').addClass('coding-rules-extended-view');
  },

  hideDetails() {
    this.$('.coding-rules').removeClass('coding-rules-extended-view');
    if (this.scroll != null) {
      $(window).scrollTop(this.scroll);
    }
  },

  detailsShow() {
    return this.$('.coding-rules').is('.coding-rules-extended-view');
  }
});
