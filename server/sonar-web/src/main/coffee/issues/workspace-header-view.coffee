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
  'components/navigator/workspace-header-view'
  'templates/issues'
], (
  WorkspaceHeaderView
) ->

  $ = jQuery


  class extends WorkspaceHeaderView
    template: Templates['issues-workspace-header']


    events: ->
      _.extend super,
        'click .js-back': 'returnToList'
        'click .js-new-search': 'newSearch'


    initialize: ->
      super
      @_onBulkIssues = window.onBulkIssues
      window.onBulkIssues = =>
        $('#modal').dialog 'close'
        @options.app.controller.fetchList()


    onClose: ->
      window.onBulkIssues = @_onBulkIssues


    returnToList: ->
      @options.app.controller.closeComponentViewer()


    newSearch: ->
      @options.app.controller.newSearch()


    bulkChange: ->
      query = @options.app.controller.getQuery '&', true
      url = "#{baseUrl}/issues/bulk_change_form?#{query}"
      openModalWindow url, {}
