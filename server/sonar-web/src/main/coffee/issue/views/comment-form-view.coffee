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
  'common/popup'
  'templates/issue'
], (
  PopupView
) ->

  $ = jQuery
  

  class extends PopupView
    className: 'bubble-popup issue-comment-bubble-popup'
    template: Templates['comment-form']


    ui:
      textarea: '.issue-comment-form-text textarea'
      cancelButton: '.js-issue-comment-cancel'
      submitButton: '.js-issue-comment-submit'


    events:
      'click': 'onClick'
      'keydown @ui.textarea': 'onKeydown'
      'keyup @ui.textarea': 'toggleSubmit'
      'click @ui.cancelButton': 'cancel'
      'click @ui.submitButton': 'submit'


    onRender: ->
      super
      setTimeout (=> @ui.textarea.focus()), 100


    toggleSubmit: ->
      @ui.submitButton.prop 'disabled', @ui.textarea.val().length == 0


    onClick: (e) ->
      # disable close by clicking inside
      e.stopPropagation()


    onKeydown: (e) ->
      @close() if e.keyCode == 27 # escape


    cancel: ->
      @options.detailView.updateAfterAction false


    submit: ->
      text = @ui.textarea.val()
      update = @model && @model.has('key')
      method = if update then 'edit_comment' else 'add_comment'
      url = "#{baseUrl}/api/issues/#{method}"
      data = text: text
      if update
        data.key = @model.get('key')
      else
        data.issue = @options.issue.id
      $.post url, data
      .done =>
        @options.detailView.updateAfterAction true
