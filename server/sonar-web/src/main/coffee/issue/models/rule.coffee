define [
  'backbone'
], (
  Backbone
) ->

  class Rule extends Backbone.Model

    url: ->
      "#{baseUrl}/api/rules/show/?key=#{@get('key')}"


    parse: (r) ->
      if r.rule then r.rule else r