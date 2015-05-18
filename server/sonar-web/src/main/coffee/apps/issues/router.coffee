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
  'components/navigator/router'
], (
  Router
) ->

  class extends Router
    routes:
      '': 'home'
      ':query': 'index'


    initialize: (options) ->
      super
      @listenTo options.app.state, 'change:filter', @updateRoute


    home: ->
      if @options.app.state.get 'isContext'
        @navigate 'resolved=false', { trigger: true, replace: true }
      else
        @options.app.controller.showHomePage()


    index: (query) ->
      query = @options.app.controller.parseQuery query
      if query.id?
        filter = @options.app.filters.get query.id
        delete query.id
        filter.fetch().done =>
          if Object.keys(query).length > 0
            @options.app.controller.applyFilter filter, true
            @options.app.state.setQuery query
            @options.app.state.set changed: true
          else
            @options.app.controller.applyFilter filter
      else
        @options.app.state.setQuery query
