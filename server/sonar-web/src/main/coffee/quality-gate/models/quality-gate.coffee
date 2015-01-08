define ->

  class QualityGate extends Backbone.Model

    url: ->
      "#{baseUrl}/api/qualitygates/show?id=#{@get('id')}"
