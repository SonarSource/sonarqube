define [
  'backbone'
], (
  Backbone
) ->

  class WebService extends Backbone.Model
    idAttribute: 'path'

    initialize: (options) ->
      @set 'internal', _.every options.actions, (action) -> action.internal
