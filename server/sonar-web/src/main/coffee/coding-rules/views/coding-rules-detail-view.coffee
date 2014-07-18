define [
  'backbone'
  'backbone.marionette'
  'coding-rules/views/coding-rules-detail-quality-profiles-view'
  'coding-rules/views/coding-rules-detail-quality-profile-view'
  'coding-rules/views/coding-rules-detail-custom-rules-view'
  'coding-rules/views/coding-rules-detail-custom-rule-view'
  'coding-rules/views/coding-rules-parameter-popup-view'
  'templates/coding-rules'
], (
  Backbone
  Marionette
  CodingRulesDetailQualityProfilesView
  CodingRulesDetailQualityProfileView
  CodingRulesDetailCustomRulesView
  CodingRulesDetailCustomRuleView
  CodingRulesParameterPopupView
  Templates
) ->

  class CodingRulesDetailView extends Marionette.Layout
    template: Templates['coding-rules-detail']


    regions:
      qualityProfilesRegion: '#coding-rules-detail-quality-profiles'
      customRulesRegion: '.coding-rules-detail-custom-rules-section'
      customRulesListRegion: '#coding-rules-detail-custom-rules'
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
      extendDescriptionRemove: '#coding-rules-detail-extend-description-remove'
      extendDescriptionText: '#coding-rules-detail-extend-description-text'
      extendDescriptionSpinner: '#coding-rules-detail-extend-description-spinner'
      cancelExtendDescription: '#coding-rules-detail-extend-description-cancel'

      activateQualityProfile: '#coding-rules-quality-profile-activate'
      activateContextQualityProfile: '.coding-rules-detail-quality-profile-activate'
      changeQualityProfile: '.coding-rules-detail-quality-profile-update'
      createCustomRule: '#coding-rules-custom-rules-create'
      changeCustomRule: '#coding-rules-detail-custom-rule-change'
      deleteCustomRule: '#coding-rules-detail-custom-rule-delete'


    events:
      'click @ui.tagsChange': 'changeTags'
      'click @ui.tagsEditDone': 'editDone'

      'click @ui.extendDescriptionLink': 'showExtendDescriptionForm'
      'click @ui.cancelExtendDescription': 'hideExtendDescriptionForm'
      'click @ui.extendDescriptionSubmit': 'submitExtendDescription'
      'click @ui.extendDescriptionRemove': 'removeExtendedDescription'

      'click @ui.activateQualityProfile': 'activateQualityProfile'
      'click @ui.activateContextQualityProfile': 'activateContextQualityProfile'
      'click @ui.changeQualityProfile': 'changeQualityProfile'
      'click @ui.createCustomRule': 'createCustomRule'
      'click @ui.changeCustomRule': 'changeCustomRule'
      'click @ui.deleteCustomRule': 'deleteCustomRule'

      'click .coding-rules-detail-parameter-details': 'showParamPopup'

    initialize: (options) ->
      super options

      if @model.get 'params'
        origParams = @model.get('params')
        _.map origParams, (param) =>
          _.extend param, showMoreLink: (param.htmlDesc and param.htmlDesc.indexOf('<') >= 0)
        @model.set 'params', _.sortBy(origParams, 'key')

      _.map options.actives, (active) =>
        _.extend active, options.app.getQualityProfileByKey active.qProfile
      qualityProfiles = new Backbone.Collection options.actives,
        comparator: 'name'
      @qualityProfilesView = new CodingRulesDetailQualityProfilesView
        app: @options.app
        collection: qualityProfiles
        rule: @model

      unless @model.get 'isTemplate'
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
      @$el.find('.open-modal').modal()

      if @model.get 'isTemplate'
        @$(@contextRegion.el).hide()

        if _.isEmpty(@options.actives)
          @$(@qualityProfilesRegion.el).hide()
        else
          @qualityProfilesRegion.show @qualityProfilesView

        @$(@customRulesRegion.el).show()
        customRulesOriginal = @$(@customRulesRegion.el).html()

        @$(@customRulesRegion.el).html '<i class="spinner"></i>'

        customRules = new Backbone.Collection()
        jQuery.ajax
          url: "#{baseUrl}/api/rules/search"
          data:
            template_key: @model.get 'key'
            f: 'name,severity,params'
        .done (r) =>
          customRules.add r.rules

          # Protect against element disappearing due to navigation
          if @customRulesRegion
            if customRules.isEmpty() and not @options.app.canWrite
              @$(@customRulesRegion.el).hide()
            else
              @customRulesView = new CodingRulesDetailCustomRulesView
                app: @options.app
                collection: customRules
                templateRule: @model
              @$(@customRulesRegion.el).html customRulesOriginal
              @customRulesListRegion.show @customRulesView

      else
        @$(@customRulesRegion.el).hide()
        @$(@qualityProfilesRegion.el).show()
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
        if @ui.tagInput.select2
          # Prevent synchronization issue with navigation
          @ui.tagInput.select2
            tags: _.difference (_.difference r.tags, that.model.get 'tags'), that.model.get 'sysTags'
            width: '300px'

      @ui.tagsEdit.hide()

      @ui.extendDescriptionForm.hide()
      @ui.extendDescriptionSpinner.hide()


    showParamPopup: (e) ->
      e.stopPropagation()
      jQuery('body').click()
      key = jQuery(e.currentTarget).closest('.coding-rules-detail-parameter').data 'key'
      popup = new CodingRulesParameterPopupView
        model: new Backbone.Model _.findWhere(@model.get('params'), key: key)
        triggerEl: jQuery(e.currentTarget)
      popup.render()
      false


    hideContext: ->
      @contextRegion.reset()
      @$(@contextRegion.el).hide()


    changeTags: ->
      if @ui.tagsEdit.show
        @ui.tagsEdit.show()
      if @ui.tagsList.hide
        @ui.tagsList.hide()
      key.setScope 'tags'
      key 'escape', 'tags', => @cancelEdit()


    cancelEdit: ->
      key.unbind 'escape', 'tags'
      if @ui.tagsList.show
        @ui.tagsList.show()
      if @ui.tagInput.select2
        @ui.tagInput.select2 'close'
      if @ui.tagsEdit.hide
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
          @cancelEdit()
      .always =>
        @render()


    showExtendDescriptionForm: ->
      @ui.descriptionExtra.hide()
      @ui.extendDescriptionForm.show()
      key.setScope 'extraDesc'
      key 'escape', 'extraDesc', => @hideExtendDescriptionForm()
      @ui.extendDescriptionText.focus()


    hideExtendDescriptionForm: ->
      key.unbind 'escape', 'extraDesc'
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


    removeExtendedDescription: ->
      confirmDialog
        html: t 'coding_rules.remove_extended_description.confirm'
        yesHandler: =>
          @ui.extendDescriptionText.val ''
          @submitExtendDescription()


    activateQualityProfile: ->
      @options.app.codingRulesQualityProfileActivationView.model = null
      @options.app.codingRulesQualityProfileActivationView.show()


    activateContextQualityProfile: ->
      @options.app.codingRulesQualityProfileActivationView.model = @contextProfile
      @options.app.codingRulesQualityProfileActivationView.show()

    createCustomRule: ->
      @options.app.codingRulesCustomRuleCreationView.templateRule = @model
      @options.app.codingRulesCustomRuleCreationView.model = new Backbone.Model()
      @options.app.codingRulesCustomRuleCreationView.show()


    changeCustomRule: ->
      @options.app.codingRulesCustomRuleCreationView.model = @model
      @options.app.codingRulesCustomRuleCreationView.show()


    deleteCustomRule: ->
      confirmDialog
        title: t 'delete'
        html: t 'are_you_sure'
        yesHandler: =>
          jQuery.ajax
            type: 'POST'
            url: "#{baseUrl}/api/rules/delete"
            data:
              key: @model.get 'key'
          .done =>
            @options.app.fetchFirstPage()
          .fail =>
            @options.app.showRule @model.get('key')


    serializeData: ->
      contextQualityProfile = @options.app.getQualityProfile()
      repoKey = @model.get 'repo'
      isManual = (@options.app.manualRepository().key == repoKey)

      qualityProfilesVisible = not isManual
      if qualityProfilesVisible
        if @model.get 'isTemplate'
          qualityProfilesVisible = (not _.isEmpty(@options.actives))
        else
          qualityProfilesVisible = (@options.app.canWrite or not _.isEmpty(@options.actives))


      _.extend super,
        contextQualityProfile: contextQualityProfile
        contextQualityProfileName: @options.app.qualityProfileFilter.view.renderValue()
        qualityProfile: @contextProfile
        language: @options.app.languages[@model.get 'lang']
        repository: _.find(@options.app.repositories, (repo) -> repo.key == repoKey).name
        isManual: isManual
        canWrite: @options.app.canWrite
        qualityProfilesVisible: qualityProfilesVisible
        subcharacteristic: (@options.app.characteristics[@model.get 'debtSubChar'] || '').replace ': ', ' > '
        createdAt: new Date(@model.get 'createdAt')
        allTags: _.union @model.get('sysTags'), @model.get('tags')
