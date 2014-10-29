define [
  'backbone'
], (
  Backbone
) ->

  class extends Backbone.Model

    url: ->
      "/api/issue_filters/show/#{@id}"


    parse: (r) ->
      if r.filter? then r.filter else r
