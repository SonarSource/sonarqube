define [
  'backbone'
], (
  Backbone
) ->


  class Period extends Backbone.Model

    defaults:
      key: ''
      label: t 'none'
      sinceDate: null
