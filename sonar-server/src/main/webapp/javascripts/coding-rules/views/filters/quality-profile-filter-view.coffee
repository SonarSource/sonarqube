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

