#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

define [
  'templates/issues'
], ->

  $ = jQuery


  class extends Marionette.Layout
    template: Templates['issues-layout']


    regions:
      filtersRegion: '.search-navigator-filters'
      facetsRegion: '.search-navigator-facets'
      workspaceHeaderRegion: '.search-navigator-workspace-header'
      workspaceListRegion: '.search-navigator-workspace-list'
      workspaceComponentViewerRegion: '.issues-workspace-component-viewer'
      workspaceHomeRegion: '.issues-workspace-home'


    onRender: ->
      @$(@filtersRegion.el).addClass('hidden') if @options.app.state.get('isContext')
      $('.search-navigator').addClass 'sticky'
      top = $('.search-navigator').offset().top
      @$('.search-navigator-workspace-header').css top: top
      @$('.search-navigator-side').css({ top: top }).isolatedScroll()


    showSpinner: (region) ->
      @[region].show new Marionette.ItemView
        template: _.template('<i class="spinner"></i>')


    showComponentViewer: ->
      @scroll = $(window).scrollTop()
      $('.issues').addClass 'issues-extended-view'


    hideComponentViewer: ->
      $('.issues').removeClass 'issues-extended-view'
      $(window).scrollTop @scroll if @scroll?


    showHomePage: ->
      $('.issues').addClass 'issues-home-view'


    hideHomePage: ->
      $('.issues').removeClass 'issues-home-view'
