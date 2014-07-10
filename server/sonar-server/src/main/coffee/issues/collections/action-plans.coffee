define [
  'backbone'
], (
  Backbone
) ->

  class ActionPlans extends Backbone.Collection

    url: ->
      "#{baseUrl}/api/action_plans/search"


    parse: (r) ->
      r.actionPlans