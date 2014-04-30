define [
  'backbone',
  'api-documentation/models/web-service-action'
], (
  Backbone,
  WebServiceAction
) ->

  class WebServiceActions extends Backbone.Collection
    model: WebServiceAction
    comparator: 'key'
