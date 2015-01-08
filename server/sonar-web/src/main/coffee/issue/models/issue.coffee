define ->

  class Issue extends Backbone.Model
    idAttribute: 'key'


    url: ->
      "#{baseUrl}/api/issues/show?key=#{@get('key')}"


    parse: (r) ->
      if r.issue then r.issue else r
