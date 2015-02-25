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
  'issue/views/action-options-view'
  'templates/issue'
], (
  ActionOptionsView
) ->

  $ = jQuery
  

  class extends ActionOptionsView
    template: Templates['issue-set-severity-form']


    getTransition: ->
      @model.get 'severity'


    selectInitialOption: ->
      @makeActive @getOptions().filter("[data-value=#{@getTransition()}]")


    selectOption: (e) ->
      severity = $(e.currentTarget).data 'value'
      @submit severity
      super


    submit: (severity) ->
      _severity = @getTransition()
      return if severity == _severity
      @model.set severity: severity
      $.ajax
        type: 'POST'
        url: "#{baseUrl}/api/issues/set_severity"
        data:
          issue: @model.id
          severity: severity
      .fail =>
        @model.set severity: _severity


    serializeData: ->
      _.extend super,
        items: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
