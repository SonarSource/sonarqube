define [
  'backbone'
  'backbone.marionette'
  'coding-rules/views/coding-rules-detail-quality-profiles-view'
  'coding-rules/views/coding-rules-detail-quality-profile-view'
  'templates/coding-rules'
], (
  Backbone
  Marionette
  CodingRulesDetailQualityProfilesView
  CodingRulesDetailQualityProfileView
  Templates
) ->

  class CodingRulesDetailView extends Marionette.Layout
    template: Templates['coding-rules-detail']


    regions:
      qualityProfilesRegion: '#coding-rules-detail-quality-profiles'
      contextRegion: '.coding-rules-detail-context'


    ui:
      tagsChange: '.coding-rules-detail-tags-change'
      tagInput: '.coding-rules-detail-tag-input'
      tagsEdit: '.coding-rules-detail-tag-edit'
      tagsEditDone: '.coding-rules-detail-tag-edit-done'
      tagsList: '.coding-rules-detail-tag-list'

      descriptionExtra: '#coding-rules-detail-description-extra'
      extendDescriptionLink: '#coding-rules-detail-extend-description'
      extendDescriptionForm: '.coding-rules-detail-extend-description-form'
      extendDescriptionSubmit: '#coding-rules-detail-extend-description-submit'
      extendDescriptionText: '#coding-rules-detail-extend-description-text'
      extendDescriptionSpinner: '#coding-rules-detail-extend-description-spinner'
      cancelExtendDescription: '#coding-rules-detail-extend-description-cancel'

      activateQualityProfile: '#coding-rules-quality-profile-activate'
      activateContextQualityProfile: '.coding-rules-detail-quality-profile-activate'
      changeQualityProfile: '.coding-rules-detail-quality-profile-update'


    events:
      'click @ui.tagsChange': 'changeTags'
      'click @ui.tagsEditDone': 'editDone'

      'click @ui.extendDescriptionLink': 'showExtendDescriptionForm'
      'click @ui.cancelExtendDescription': 'hideExtendDescriptionForm'
      'click @ui.extendDescriptionSubmit': 'submitExtendDescription'

      'click @ui.activateQualityProfile': 'activateQualityProfile'
      'click @ui.activateContextQualityProfile': 'activateContextQualityProfile'
      'click @ui.changeQualityProfile': 'changeQualityProfile'


    initialize: (options) ->
      qualityProfiles = new Backbone.Collection options.model.get 'qualityProfiles'
      @qualityProfilesView = new CodingRulesDetailQualityProfilesView
        app: @options.app
        collection: qualityProfiles

      qualityProfile = @options.app.getQualityProfile()
      if qualityProfile
        @contextProfile = qualityProfiles.findWhere key: qualityProfile
        unless @contextProfile
          @contextProfile = new Backbone.Model
            key: qualityProfile, name: @options.app.qualityProfileFilter.view.renderValue()
        @contextQualityProfileView = new CodingRulesDetailQualityProfileView
          app: @options.app
          model: @contextProfile
        @listenTo @contextProfile, 'destroy', @hideContext


    onRender: ->
      @qualityProfilesRegion.show @qualityProfilesView

      if @options.app.getQualityProfile()
        @$(@contextRegion.el).show()
        @contextRegion.show @contextQualityProfileView
      else
        @$(@contextRegion.el).hide()

      @ui.tagInput.select2
        tags: _.difference @options.app.tags, @model.get 'tags'
        width: '300px'
      @ui.tagsEdit.hide()

      @ui.extendDescriptionForm.hide()
      @ui.extendDescriptionSpinner.hide()


    hideContext: ->
      @contextRegion.reset()
      @$(@contextRegion.el).hide()


    changeTags: ->
      @ui.tagsEdit.show()
      @ui.tagsList.hide()
      @ui.tagInput.select2 'open'


    editDone: ->
      @ui.tagsEdit.html '<i class="spinner"></i>'
      tags = @ui.tagInput.val()
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/codingrules/set_tags"
        data: tags: tags
      .done =>
          if tags.length > 0
            @model.set 'tags', tags.split ','
          else
            @model.unset 'tags'
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
      @options.app.codingRulesQualityProfileActivationView.model = null
      @options.app.codingRulesQualityProfileActivationView.show()


    activateContextQualityProfile: ->
      @options.app.codingRulesQualityProfileActivationView.model = @contextProfile
      @options.app.codingRulesQualityProfileActivationView.show()


    serializeData: ->
      contextQualityProfile = @options.app.getQualityProfile()

      _.extend super,
        contextQualityProfile: contextQualityProfile
        contextQualityProfileName: @options.app.qualityProfileFilter.view.renderValue()
        qualityProfile: @contextProfile