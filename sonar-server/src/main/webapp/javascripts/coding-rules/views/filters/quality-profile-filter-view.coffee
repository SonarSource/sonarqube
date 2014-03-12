define [
  'navigator/filters/ajax-select-filters'
], (
  AjaxSelectFilters
) ->

  class QualityProfileSuggestions extends AjaxSelectFilters.Suggestions

    url: ->
      "#{baseUrl}/api/qualityprofiles/list"



  class QualityProfileFilterView extends AjaxSelectFilters.AjaxSelectFilterView

    initialize: ->
      super
      @choices = new QualityProfileSuggestions
      @listenTo @model, 'change:value', @updateParentQualityProfile


    updateParentQualityProfile: ->
      selected = @getSelected()
      if selected.length == 1
        @model.set 'parentQualityProfile', selected[0].get('parent')
      else
        @model.unset 'parentQualityProfile'

