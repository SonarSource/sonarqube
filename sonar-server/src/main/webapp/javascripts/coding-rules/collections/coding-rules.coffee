define [
  'backbone',
  'coding-rules/models/coding-rule'
], (
  Backbone,
  CodingRule
) ->

  class CodingRules extends Backbone.Collection
    model: CodingRule


    url: ->
      "#{baseUrl}/api/codingrules/search"


    parse: (r) ->
      @paging = r.paging
      r.codingrules
