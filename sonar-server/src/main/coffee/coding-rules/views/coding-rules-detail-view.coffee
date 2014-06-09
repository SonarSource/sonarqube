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
      customRulesRegion: '#coding-rules-detail-custom-rules'
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
      createCustomRule: '#coding-rules-custom-rules-create'


    events:
      'click @ui.tagsChange': 'changeTags'
      'click @ui.tagsEditDone': 'editDone'

      'click @ui.extendDescriptionLink': 'showExtendDescriptionForm'
      'click @ui.cancelExtendDescription': 'hideExtendDescriptionForm'
      'click @ui.extendDescriptionSubmit': 'submitExtendDescription'

      'click @ui.activateQualityProfile': 'activateQualityProfile'
      'click @ui.activateContextQualityProfile': 'activateContextQualityProfile'
      'click @ui.changeQualityProfile': 'changeQualityProfile'
      'clock @ui.createCustomRule': 'createCustomRule'


    initialize: (options) ->
      super options

      if @model.get 'params'
        @model.set 'params', _.sortBy(@model.get('params'), 'key')

      if @model.get 'isTemplate'
        customRules = new Backbone.Collection()
        jQuery.ajax
          url: "#{baseUrl}/api/rules/search"
          data:
            template_key: @model.get 'key'
            f: 'name'
        .done (r) =>
          customRules.add r.rules
        #@customRulesView = new CodingrulesDetailCustomRulesView
        #  app: @options.app
        #  collection: customRules
        #  rule: @model
      else
        qualityProfiles = new Backbone.Collection options.actives
        @qualityProfilesView = new CodingRulesDetailQualityProfilesView
          app: @options.app
          collection: qualityProfiles
          rule: @model

        qualityProfileKey = @options.app.getQualityProfile()

        if qualityProfileKey
          @contextProfile = qualityProfiles.findWhere qProfile: qualityProfileKey
          unless @contextProfile
            @contextProfile = new Backbone.Model
              key: qualityProfileKey, name: @options.app.qualityProfileFilter.view.renderValue()
          @contextQualityProfileView = new CodingRulesDetailQualityProfileView
            app: @options.app
            model: @contextProfile
            rule: @model
            qualityProfiles: qualityProfiles

          @listenTo @contextProfile, 'destroy', @hideContext

    onRender: ->
      @$el.find('.open-modal').modal();

      if @model.get 'isTemplate'
        @$(@contextRegion.el).hide()
        # @customRulesRegion.show @customRulesView
      else
        @qualityProfilesRegion.show @qualityProfilesView

        if @options.app.getQualityProfile()
          @$(@contextRegion.el).show()
          @contextRegion.show @contextQualityProfileView
        else
          @$(@contextRegion.el).hide()

      that = @
      jQuery.ajax
        url: "#{baseUrl}/api/rules/tags"
      .done (r) =>
        that.ui.tagInput.select2
          tags: _.difference (_.difference r.tags, @model.get 'tags'), @model.get 'sysTags'
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
      key.setScope 'tags'
      key 'escape', 'tags', => @cancelEdit()


    cancelEdit: ->
      key.unbind 'escape', 'tags'
      @ui.tagsList.show()
      @ui.tagInput.select2 'close'
      @ui.tagsEdit.hide()


    editDone: ->
      @ui.tagsEdit.html '<i class="spinner"></i>'
      tags = @ui.tagInput.val()
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/rules/update"
        data:
          key: @model.get 'key'
          tags: tags
      .done (r) =>
          @model.set 'tags', r.rule.tags
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
        url: "#{baseUrl}/api/rules/update"
        dataType: 'json'
        data:
          key: @model.get 'key'
          markdown_note: @ui.extendDescriptionText.val()
      .done (r) =>
        @model.set
          htmlNote: r.rule.htmlNote
          mdNote: r.rule.mdNote
        @render()


    activateQualityProfile: ->
      @options.app.codingRulesQualityProfileActivationView.model = null
      @options.app.codingRulesQualityProfileActivationView.show()


    activateContextQualityProfile: ->
      @options.app.codingRulesQualityProfileActivationView.model = @contextProfile
      @options.app.codingRulesQualityProfileActivationView.show()

    createCustomRule: ->
      #@options.app.codingRulesCustomRuleView.model = @model
      #@options.app.codingRulesCustomRuleView.show()


    serializeData: ->
      contextQualityProfile = @options.app.getQualityProfile()
      repoKey = @model.get 'repo'

      _.extend super,
        contextQualityProfile: contextQualityProfile
        contextQualityProfileName: @options.app.qualityProfileFilter.view.renderValue()
        qualityProfile: @contextProfile
        language: @options.app.languages[@model.get 'lang']
        repository: _.find(@options.app.repositories, (repo) -> repo.key == repoKey).name
        canWrite: @options.app.canWrite
        qualityProfilesVisible: not @model.get('isTemplate') and (@options.app.canWrite or not _.isEmpty(@options.actives))
        subcharacteristic: (@options.app.characteristics[@model.get 'debtSubChar'] || '').replace ': ', ' > '
        createdAt: new Date(@model.get 'createdAt')
        allTags: _.union @model.get('sysTags'), @model.get('tags')
