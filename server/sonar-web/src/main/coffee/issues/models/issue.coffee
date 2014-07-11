define [
  'backbone'
], (
  Backbone
) ->

  class Issue extends Backbone.Model

    url: ->
      "#{baseUrl}/api/issues/show?key=#{@get('key')}"


    parse: (r) ->
      if r.issue then r.issue else r