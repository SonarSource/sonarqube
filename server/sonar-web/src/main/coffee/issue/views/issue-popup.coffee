define [
  'common/popup'
], (
  Popup
) ->

  class extends Popup
    className: 'bubble-popup issue-bubble-popup'


    template: -> '<div class="bubble-popup-arrow"></div>'


    events: ->
      'click .js-issue-form-cancel': 'close'


    onRender: ->
      super
      @options.view.$el.appendTo @$el
      @options.view.render()


    onClose: ->
      @options.view.close()


    attachCloseEvents: ->
