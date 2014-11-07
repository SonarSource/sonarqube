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


    getOptions: ->
      @$('.issue-action-option')


    getActiveOption: ->
      @getOptions().filter('.active')


    makeActive: (option) ->
      @getOptions().removeClass 'active'
      option.addClass 'active'


    selectInitialOption: ->
      @makeActive @getOptions().first()


    selectNextOption: ->
      @makeActive @getActiveOption().next('.issue-action-option')
      false # return `false` to use with keymaster


    selectPreviousOption: ->
      @makeActive @getActiveOption().prev('.issue-action-option')
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
      @unbindShortcuts()


    selectOption: (e) ->
      e.preventDefault()
      @close()


    selectActiveOption: ->
      @getActiveOption().click()
