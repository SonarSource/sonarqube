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
  'templates/api-documentation'
], ->

  $ = jQuery

  class extends Marionette.Layout
    template: Templates['api-documentation-layout']


    regions:
      resultsRegion: '.api-documentation-results'
      detailsRegion: '.search-navigator-workspace'


    events:
      'change #api-documentation-show-internals': 'toggleInternals'


    initialize: (app) ->
      @app = app.app
      @listenTo(@app.webServices, 'sync', @app.refresh)


    onRender: ->
      $('.search-navigator').addClass 'sticky'
      top = $('.search-navigator').offset().top
      @$('.search-navigator-side').css({ top: top }).isolatedScroll()


    toggleInternals: (event) ->
      @app.webServices.toggleInternals()
