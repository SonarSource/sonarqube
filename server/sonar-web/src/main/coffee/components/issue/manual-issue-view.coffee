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
  './templates'
], ->

  $ = jQuery
  API_ISSUE = "#{baseUrl}/api/issues/show"
  API_ADD_MANUAL_ISSUE = "#{baseUrl}/api/issues/create"


  class extends Marionette.ItemView
    template: Templates['manual-issue']


    events:
      'submit .js-manual-issue-form': 'formSubmit'
      'click .js-cancel': 'cancel'


    initialize: ->
      @rules = []
      $.get("#{baseUrl}/api/rules/search?repositories=manual&f=name&ps=999999").done (data) =>
        @rules = data.rules
        @render()


    onRender: ->
      @delegateEvents()
      @$('[name=rule]').select2
        width: '250px'
        minimumResultsForSearch: 10
      @$('[name=rule]').select2 'open' if @rules.length > 0
      if key?
        @key = key.getScope()
        key.setScope ''


    onClose: ->
      key.setScope @key if key? && @key?


    showSpinner: ->
      @$('.js-submit').hide()
      @$('.js-spinner').show()


    hideSpinner: ->
      @$('.js-submit').show()
      @$('.js-spinner').hide()


    validateFields: ->
      message = @$('[name=message]')
      unless message.val()
        message.addClass('invalid').focus()
        return false
      return true


    formSubmit: (e) ->
      e.preventDefault()
      return unless @validateFields()
      @showSpinner()
      data = $(e.currentTarget).serialize()
      $.post API_ADD_MANUAL_ISSUE, data
        .done (r) =>
          r = JSON.parse(r) if typeof r == 'string'
          @addIssue r.issue.key
        .fail (r) =>
          @hideSpinner()
          if r.responseJSON?.errors?
            @showError _.pluck(r.responseJSON.errors, 'msg').join '. '


    addIssue: (key) ->
      $.get API_ISSUE, key: key, (r) =>
        @trigger 'add', r.issue
        @close()


    showError: (msg) ->
      @$('.code-issue-errors').removeClass('hidden').text msg


    cancel: (e) ->
      e.preventDefault()
      @close()


    serializeData: ->
      _.extend super,
        line: @options.line
        component: @options.component
        rules: _.sortBy @rules, 'name'
