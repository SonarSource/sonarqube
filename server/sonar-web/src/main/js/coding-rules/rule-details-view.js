define([
  'backbone',
  'backbone.marionette',
  'templates/coding-rules',
  'coding-rules/rule/rule-meta-view',
  'coding-rules/rule/rule-description-view',
  'coding-rules/rule/rule-parameters-view',
  'coding-rules/rule/rule-profiles-view'
], function (Backbone, Marionette, Templates, MetaView, DescView, ParamView, ProfilesView) {

  return Marionette.Layout.extend({
    template: Templates['coding-rules-rule-details'],

    regions: {
      metaRegion: '.js-rule-meta',
      descRegion: '.js-rule-description',
      paramRegion: '.js-rule-parameters',
      profilesRegion: '.js-rule-profiles'
    },

    initialize: function () {
      this.bindShortcuts();
    },

    onRender: function () {
      this.metaRegion.show(new MetaView({
        app: this.options.app,
        model: this.model
      }));
      this.descRegion.show(new DescView({
        app: this.options.app,
        model: this.model
      }));
      this.paramRegion.show(new ParamView({
        app: this.options.app,
        model: this.model
      }));
      this.profilesRegion.show(new ProfilesView({
        app: this.options.app,
        model: this.model,
        collection: new Backbone.Collection(this.getQualityProfiles())
      }));
    },

    onClose: function () {
      this.unbindShortcuts();
    },

    getQualityProfiles: function () {
      var that = this;
      return this.options.actives.map(function (profile) {
        var profileBase = _.findWhere(that.options.app.qualityProfiles, { key: profile.qProfile });
        if (profileBase != null) {
          _.extend(profile, profileBase);
        }
        return profile;
      });
    },

    bindShortcuts: function () {
      var that = this;
      key('up', 'details', function () {
        that.options.app.controller.selectPrev();
        that.options.app.controller.showDetailsForSelected();
        return false;
      });
      key('down', 'details', function () {
        that.options.app.controller.selectNext();
        that.options.app.controller.showDetailsForSelected();
        return false;
      });
      key('left', 'details', function () {
        that.options.app.controller.hideDetails();
        return false;
      });
    },

    unbindShortcuts: function () {
      key.deleteScope('details');
    },

    serializeData: function () {
      var isManual = (this.options.app.manualRepository().key === this.model.get('repo')),
          isCustom = this.model.has('templateKey'),
          isEditable = this.options.app.canWrite && (isManual || isCustom),
          qualityProfilesVisible = !isManual;

      if (qualityProfilesVisible) {
        if (this.model.get('isTemplate')) {
          qualityProfilesVisible = !_.isEmpty(this.options.actives);
        }
        else {
          qualityProfilesVisible = (this.options.app.canWrite || !_.isEmpty(this.options.actives));
        }
      }

      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        isManual: isManual,
        isEditable: isEditable,
        canWrite: this.options.app.canWrite,
        qualityProfilesVisible: qualityProfilesVisible,
        subCharacteristic: this.options.app.getSubCharacteristicName(this.model.get('debtSubChar')),
        allTags: _.union(this.model.get('sysTags'), this.model.get('tags'))
      });
    }
  });

});
