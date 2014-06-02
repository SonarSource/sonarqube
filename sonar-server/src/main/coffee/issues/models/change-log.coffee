define [
  'backbone'
], (
  Backbone
) ->

  class ChangeLog extends Backbone.Collection

    url: ->
      "#{baseUrl}/api/issues/changelog"


    parse: (r) ->
      return r.changelog