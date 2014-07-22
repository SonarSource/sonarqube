define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/header/base-header'
], (
  Marionette
  Templates
  BaseHeaderView
) ->


  class extends BaseHeaderView
    template: Templates['cw-basic-header']
