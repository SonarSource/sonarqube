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
import Marionette from 'backbone.marionette';
import Template from './templates/issues-layout.hbs';
import './styles.css';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    filtersRegion: '.issues-header',
    facetsRegion: '.search-navigator-facets',
    workspaceHeaderRegion: '.search-navigator-workspace-header',
    workspaceListRegion: '.search-navigator-workspace-list',
    workspaceComponentViewerRegion: '.issues-workspace-component-viewer'
  },

  onRender() {
    this.$('.search-navigator').addClass('sticky');
    const top = this.$('.search-navigator').offset().top;
    this.$('.search-navigator-workspace-header').css({ top });
    this.$('.search-navigator-side').css({ top }).isolatedScroll();
  },

  showSpinner(region) {
    return this[region].show(
      new Marionette.ItemView({
        template: () => '<i class="spinner"></i>'
      })
    );
  },

  showComponentViewer() {
    this.scroll = $(window).scrollTop();
    this.$('.issues').addClass('issues-extended-view');
  },

  hideComponentViewer() {
    this.$('.issues').removeClass('issues-extended-view');
    if (this.scroll != null) {
      $(window).scrollTop(this.scroll);
    }
  }
});
