define [
  'api-documentation/models/web-service-action'
], (
  WebServiceAction
) ->

  class WebServiceActions extends Backbone.Collection
    model: WebServiceAction
    comparator: 'key'

    initialize: (models, options) ->
      _.each models, (model) ->
        model.path = options.path + '/' + model.key
