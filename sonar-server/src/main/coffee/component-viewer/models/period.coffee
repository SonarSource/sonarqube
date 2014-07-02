define [
  'backbone'
], (
  Backbone
) ->


  class Period extends Backbone.Model

    defaults:
      key: ''
      sinceDate: null
