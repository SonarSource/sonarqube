define [
  'backbone',
  'backbone.marionette',
  'coding-rules/views/coding-rules-detail-quality-profiles-view'
  'common/handlebars-extensions'
], (
  Backbone,
  Marionette,
  CodingRulesDetailQualityProfilesView
) ->

  class CodingRulesDetailView extends Marionette.Layout
    template: getTemplate '#coding-rules-detail-template'


    regions:
      qualityProfilesRegion: '#coding-rules-detail-quality-profiles'


    ui:
      tagsChange: '.coding-rules-detail-tags-change'
      tagInput: '.coding-rules-detail-tag-input'
      tagsEdit: '.coding-rules-detail-tag-edit'
      tagsEditDone: '.coding-rules-detail-tag-edit-done'
      tagsList: '.coding-rules-detail-tag-list'

      descriptionExtra: '#coding-rules-detail-description-extra'
      extendDescriptionLink: '#coding-rules-detail-extend-description'
      extendDescriptionForm: '#coding-rules-detail-extend-description-form'
      extendDescriptionSubmit: '#coding-rules-detail-extend-description-submit'
      extendDescriptionText: '#coding-rules-detail-extend-description-text'
      extendDescriptionSpinner: '#coding-rules-detail-extend-description-spinner'
      cancelExtendDescription: '#coding-rules-detail-extend-description-cancel'

      qualityProfileActivate: '#coding-rules-quality-profile-activate'


    events:
      'click @ui.tagsChange': 'changeTags'
      'click @ui.tagsEditDone': 'editDone'

      'click @ui.extendDescriptionLink': 'showExtendDescriptionForm'
      'click @ui.cancelExtendDescription': 'hideExtendDescriptionForm'
      'click @ui.extendDescriptionSubmit': 'submitExtendDescription'

      'click @ui.qualityProfileActivate': 'activateQualityProfile'


    initialize: (options) ->
      @qualityProfilesView = new CodingRulesDetailQualityProfilesView
        collection: new Backbone.Collection options.model.get 'qualityProfiles'


    onRender: ->
      @qualityProfilesRegion.show @qualityProfilesView

      @ui.tagInput.select2
        tags: _.difference @options.app.tags, @model.get 'tags'
        width: '500px'
      @ui.tagsEdit.hide()

      @ui.extendDescriptionForm.hide()
      @ui.extendDescriptionSpinner.hide()

      qp = @options.app.getActiveQualityProfile()
      @$('.coding-rules-detail-quality-profile').first().addClass 'active' if qp?


    changeTags: ->
      @ui.tagsEdit.show()
      @ui.tagsList.hide()


    editDone: ->
      @ui.tagsEdit.html '<i class="spinner"></i>'
      tags = @ui.tagInput.val()
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/codingrules/set_tags"
        data: tags: tags
      .done =>
          @model.set 'tags', tags.split ','
          @render()


    showExtendDescriptionForm: ->
      @ui.descriptionExtra.hide()
      @ui.extendDescriptionForm.show()


    hideExtendDescriptionForm: ->
      @ui.descriptionExtra.show()
      @ui.extendDescriptionForm.hide()


    submitExtendDescription: ->
      @ui.extendDescriptionForm.hide()
      @ui.extendDescriptionSpinner.show()
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/codingrules/extend_description"
        dataType: 'json'
        data: text: @ui.extendDescriptionText.val()
      .done (r) =>
        @model.set extra: r.extra, extraRaw: r.extraRaw
        @render()


    activateQualityProfile: ->
      @options.app.codingRulesQualityProfileActivationView.model = @model
      @options.app.codingRulesQualityProfileActivationView.show()