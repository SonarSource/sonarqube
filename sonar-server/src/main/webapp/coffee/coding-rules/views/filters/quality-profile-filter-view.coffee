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


    createRequest: (v) ->
      jQuery.ajax
        url: baseUrl + '/api/qualityprofiles/show'
        type: 'GET'
        data: key: v
      .done (r) =>
        @choices.add new Backbone.Model
          id: r.qualityprofile.id,
          text: r.qualityprofile.text,
          parent: r.qualityprofile.parent,
          checked: true

