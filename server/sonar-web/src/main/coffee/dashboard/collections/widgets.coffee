define [
  'backbone'
  'dashboard/models/widget'
], (
  Backbone
  Widget
) ->

  class extends Backbone.Collection
    model: Widget


    comparator: (model) ->
      model.get('row')

