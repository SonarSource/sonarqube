/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define([
  './templates'
], function () {

  var $ = jQuery;

  return Marionette.Layout.extend({
    template: Templates['coding-rules-layout'],

    regions: {
      filtersRegion: '.search-navigator-filters',
      facetsRegion: '.search-navigator-facets',
      workspaceHeaderRegion: '.search-navigator-workspace-header',
      workspaceListRegion: '.search-navigator-workspace-list',
      workspaceDetailsRegion: '.search-navigator-workspace-details'
    },

    onRender: function () {
      var navigator = $('.search-navigator');
      navigator.addClass('sticky');
      var top = navigator.offset().top;
      this.$('.search-navigator-workspace-header').css({ top: top });
      this.$('.search-navigator-side').css({ top: top }).isolatedScroll();
    },

    showDetails: function () {
      this.scroll = $(window).scrollTop();
      $('.search-navigator').addClass('search-navigator-extended-view');
    },


    hideDetails: function () {
      $('.search-navigator').removeClass('search-navigator-extended-view');
      if (this.scroll != null) {
        $(window).scrollTop(this.scroll);
      }
    }

  });

});
