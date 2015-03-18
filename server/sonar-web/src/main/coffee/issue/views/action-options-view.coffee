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
], (
  PopupView
) ->

  $ = jQuery


  class extends PopupView
    keyScope: 'issue-action-options'


    ui:
      options: '.issue-action-option'


    events: ->
      'click .issue-action-option': 'selectOption'
      'mouseenter .issue-action-option': 'activateOptionByPointer'


    initialize: ->
      @bindShortcuts()


    onRender: ->
      super
      @selectInitialOption()
      @$('[data-toggle="tooltip"]').tooltip container: 'body'


    getOptions: ->
      @$('.issue-action-option')


    getActiveOption: ->
      @getOptions().filter('.active')


    makeActive: (option) ->
      if option.length > 0
        @getOptions().removeClass 'active'
        option.addClass 'active'


    selectInitialOption: ->
      @makeActive @getOptions().first()


    selectNextOption: ->
      @makeActive @getActiveOption().nextAll('.issue-action-option').first()
      false # return `false` to use with keymaster


    selectPreviousOption: ->
      @makeActive @getActiveOption().prevAll('.issue-action-option').first()
      false # return `false` to use with keymaster


    activateOptionByPointer: (e) ->
      @makeActive $(e.currentTarget)


    bindShortcuts: ->
      @currentKeyScope = key.getScope()
      key.setScope @keyScope
      key 'down', @keyScope, => @selectNextOption()
      key 'up', @keyScope, => @selectPreviousOption()
      key 'return', @keyScope, => @selectActiveOption()
      key 'escape', @keyScope, => @close()
      key 'backspace', @keyScope, => false # disable go back through the history
      key 'shift+tab', @keyScope, => false


    unbindShortcuts: ->
      key.unbind 'down', @keyScope
      key.unbind 'up', @keyScope
      key.unbind 'return', @keyScope
      key.unbind 'escape', @keyScope
      key.unbind 'backspace', @keyScope
      key.unbind 'tab', @keyScope
      key.unbind 'shift+tab', @keyScope
      key.setScope @currentKeyScope


    onClose: ->
      super
      @unbindShortcuts()
      @$('[data-toggle="tooltip"]').tooltip 'destroy'
      $('.tooltip').remove()


    selectOption: (e) ->
      e.preventDefault()
      @close()


    selectActiveOption: ->
      @getActiveOption().click()
