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
    template: Templates['issue-plan-form']


    getActionPlan: ->
      @model.get('actionPlan') || ''


    getActionPlanName: ->
      @model.get 'actionPlanName'


    selectInitialOption: ->
      @makeActive @getOptions().filter("[data-value=#{@getActionPlan()}]")


    selectOption: (e) ->
      actionPlan = $(e.currentTarget).data 'value'
      actionPlanName = $(e.currentTarget).data 'text'
      @submit actionPlan, actionPlanName
      super


    submit: (actionPlan, actionPlanName) ->
      _actionPlan = @getActionPlan()
      _actionPlanName = @getActionPlanName()
      return if actionPlan == _actionPlan
      if actionPlan == ''
        @model.set actionPlan: null, actionPlanName: null
      else
        @model.set actionPlan: actionPlan, actionPlanName: actionPlanName
      $.ajax
        type: 'POST'
        url: "#{baseUrl}/api/issues/plan"
        data:
          issue: @model.id
          plan: actionPlan
      .fail =>
        @model.set assignee: _actionPlan, assigneeName: _actionPlanName


    getActionPlans: ->
      [{ key: '', name: t 'issue.unplanned' }].concat @collection.toJSON()


    serializeData: ->
      _.extend super,
        items: @getActionPlans()
