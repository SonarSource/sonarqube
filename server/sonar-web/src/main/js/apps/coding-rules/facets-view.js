define([
      'components/navigator/facets-view',
      './facets/base-facet',
      './facets/query-facet',
      './facets/key-facet',
      './facets/language-facet',
      './facets/repository-facet',
      './facets/tag-facet',
      './facets/quality-profile-facet',
      './facets/characteristic-facet',
      './facets/severity-facet',
      './facets/status-facet',
      './facets/available-since-facet',
      './facets/inheritance-facet',
      './facets/active-severity-facet',
      './facets/template-facet'
    ],
    function (FacetsView,
              BaseFacet,
              QueryFacet,
              KeyFacet,
              LanguageFacet,
              RepositoryFacet,
              TagFacet,
              QualityProfileFacet,
              CharacteristicFacet,
              SeverityFacet,
              StatusFacet,
              AvailableSinceFacet,
              InheritanceFacet,
              ActiveSeverityFacet,
              TemplateFacet) {

      var viewsMapping = {
        q: QueryFacet,
        rule_key: KeyFacet,
        languages: LanguageFacet,
        repositories: RepositoryFacet,
        tags: TagFacet,
        qprofile: QualityProfileFacet,
        debt_characteristics: CharacteristicFacet,
        severities: SeverityFacet,
        statuses: StatusFacet,
        available_since: AvailableSinceFacet,
        inheritance: InheritanceFacet,
        active_severities: ActiveSeverityFacet,
        is_template: TemplateFacet
      };

      return FacetsView.extend({

        getChildView: function (model) {
          var view = viewsMapping[model.get('property')];
          return view ? view : BaseFacet;
        }

      });

    });
