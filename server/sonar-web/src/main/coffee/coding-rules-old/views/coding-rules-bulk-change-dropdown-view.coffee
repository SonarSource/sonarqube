define [
  'backbone.marionette',
  'templates/coding-rules-old'
], (
  Marionette,
  Templates
) ->

  class CodingRulesBulkChangeDropdownView extends Marionette.ItemView
    className: 'coding-rules-bulk-change-dropdown'
    template: Templates['coding-rules-bulk-change-dropdown']


    events:
      'click .coding-rules-bulk-change-dropdown-link': 'doAction'


    doAction: (e) ->
      action = jQuery(e.currentTarget).data 'action'
      param = jQuery(e.currentTarget).data 'param'
      @options.app.codingRulesBulkChangeView.show action, param


    onRender: ->
      jQuery('body').append @el
      jQuery('body').off('click.bulk-change').on 'click.bulk-change', => @hide()
      @$el.css
        top: jQuery('.navigator-actions').offset().top + jQuery('.navigator-actions').height() + 1
        left: jQuery('.navigator-actions').offset().left + jQuery('.navigator-actions').outerWidth() - @$el.outerWidth()


    toggle: ->
      if @$el.is(':visible') then @hide() else @show()


    show: ->
      @render()
      @$el.show()


    hide: ->
      @$el.hide()


    serializeData: ->
      languages = @options.app.languageFilter.get('value')
      activationValues = @options.app.activationFilter.get('value') or []
      qualityProfile = @options.app.getQualityProfile()

      qualityProfile: qualityProfile
      qualityProfileName: @options.app.qualityProfileFilter.view.renderValue()
      singleLanguage: _.isArray(languages) and languages.length == 1
      language: @options.app.languageFilter.view.renderValue()
      allowActivateOnProfile: qualityProfile and (activationValues.length == 0 or activationValues[0] == 'false')
      allowDeactivateOnProfile: qualityProfile and (activationValues.length == 0 or activationValues[0] == 'true')
