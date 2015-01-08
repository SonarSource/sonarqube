define [
  'dashboard/models/widget'
], (
  Widget
) ->

  class extends Backbone.Collection
    model: Widget


    comparator: (model) ->
      model.get('row')

