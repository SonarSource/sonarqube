define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class HeaderQualityProfilesView extends Marionette.ItemView
    className: 'coding-rules-header-quality-profiles'
    template: getTemplate '#coding-rules-header-quality-profiles-template'


    events:
      'click label': 'selectQualityProfile'


    selectQualityProfile: (e)->
      @options.app.header = jQuery(e.target).html()
      @options.headerView.render()
