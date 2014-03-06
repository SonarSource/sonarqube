define [
  'backbone'
], (
  Backbone
) ->

  class CodingRule extends Backbone.Model

    url: ->
      "#{baseUrl}/api/codingrules/show"


    parse: (r) ->
      model = if r.codingrule? then r.codingrule else r
      _.extend model, qualityProfiles: r.qualityprofiles if r.qualityprofiles?
      model
