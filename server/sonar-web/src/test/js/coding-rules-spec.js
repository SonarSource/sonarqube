/* global describe:false, it:false */
var lib = require('../lib');

describe('Coding Rules App', function () {

  it('should show alert when there is no available profiles for activation', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app-no-available-profiles.json');
          lib.fmock('/api/rules/search', 'search-no-available-profiles.json');
          lib.fmock('/api/rules/show', 'show-no-available-profiles.json');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rules-detail-header');
        })

        .then(function () {
          test.assertExist('#coding-rules-quality-profile-activate');
          casper.click('#coding-rules-quality-profile-activate');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          test.assertExists('.modal .alert');
        });
  });

  it('should show profile facet', 6, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-profile-facet-qprofile-active.json', { data: { activation: true } });
          lib.fmock('/api/rules/search', 'search-profile-facet-qprofile-inactive.json',
              { data: { activation: 'false' } });
          lib.fmock('/api/rules/search', 'search-profile-facet.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        })

        .then(function () {
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '407');
          test.assertExists('.js-facet[data-value="java-default-with-mojo-conventions-49307"] .js-active.facet-toggle-active');
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"] .js-inactive');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '408');
          test.assertExists('.js-facet[data-value="java-default-with-mojo-conventions-49307"] .js-inactive.facet-toggle-active');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
        });
  });

  it('should show query facet', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-query.json', { data: { q: 'query' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
          casper.evaluate(function () {
            jQuery('[data-property="q"] input').val('query');
            jQuery('[data-property="q"] form').submit();
          });
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '4');
          casper.evaluate(function () {
            jQuery('[data-property="q"] input').val('');
            jQuery('[data-property="q"] form').submit();
          });
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
        });
  });

  it('should show rule permalink', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
          lib.fmock('/api/rules/show', 'show.json');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected');
        })

        .then(function () {
          casper.click('.coding-rule.selected .js-rule');
          casper.waitForSelector('.coding-rules-detail-header');
        })

        .then(function () {
          lib.capture();
          test.assertExists('a[href="/coding_rules#rule_key=squid%3AS2204"]');
        });
  });

  it('should activate profile', 5, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
          this.showMock = lib.fmock('/api/rules/show', 'show-activate-profile.json');
          lib.smock('/api/qualityprofiles/activate_rule', '{}');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rules-detail-header');
        })

        .then(function () {
          test.assertDoesntExist('.coding-rules-detail-quality-profile-name');
          test.assertExist('#coding-rules-quality-profile-activate');
          casper.click('#coding-rules-quality-profile-activate');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          lib.clearRequestMock(this.showMock);
          lib.fmock('/api/rules/show', 'show-activate-profile-with-profile.json');
          casper.click('#coding-rules-quality-profile-activation-activate');
          casper.waitForSelector('.coding-rules-detail-quality-profile-name');
        })

        .then(function () {
          test.assertExists('.coding-rules-detail-quality-profile-name');
          test.assertExists('.coding-rules-detail-quality-profile-severity');
          test.assertExists('.coding-rules-detail-quality-profile-deactivate');
        });
  });

  it('should create custom rule', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          this.customRulesSearchMock = lib.fmock('/api/rules/search', 'search-custom-rules.json',
              { data: { template_key: 'squid:ArchitecturalConstraint' } });
          this.searchMock = lib.fmock('/api/rules/search', 'search-create-custom-rules.json');
          lib.fmock('/api/rules/show', 'show-create-custom-rules.json');
          lib.smock('/api/rules/create', '{}');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected');
        })

        .then(function () {
          casper.click('.coding-rule.selected .js-rule');
          casper.waitForSelector('#coding-rules-detail-custom-rules .coding-rules-detail-list-name');
        })

        .then(function () {
          lib.clearRequestMock(this.customRulesSearchMock);
          lib.clearRequestMock(this.searchMock);
          lib.fmock('/api/rules/search', 'search-custom-rules2.json');
        })

        .then(function () {
          test.assertElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 1);
          casper.click('.js-create-custom-rule');
          casper.evaluate(function () {
            jQuery('.modal form [name="name"]').val('test').keyup();
            jQuery('.modal form [name="markdown_description"]').val('test');
          });
          casper.click('#coding-rules-custom-rule-creation-create');
          lib.waitForElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 2);
        });
  });

  it('should reactivate custom rule', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          this.customRulesSearchMock = lib.fmock('/api/rules/search', 'search-custom-rules.json',
              { data: { template_key: 'squid:ArchitecturalConstraint' } });
          this.searchMock = lib.fmock('/api/rules/search', 'search-create-custom-rules.json');
          lib.fmock('/api/rules/show', 'show-create-custom-rules.json');
          this.createMock = lib.fmock('/api/rules/create', 'create-create-custom-rules.json', { status: 409 });
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('.js-create-custom-rule');
        })

        .then(function () {
          casper.click('.js-create-custom-rule');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('.modal form [name="name"]').val('My Custom Rule').keyup();
            jQuery('.modal form [name="markdown_description"]').val('My Description');
          });
          casper.click('#coding-rules-custom-rule-creation-create');
          casper.waitForSelector('.modal .alert-warning');
        })

        .then(function () {
          test.assertVisible('.modal #coding-rules-custom-rule-creation-reactivate');
          test.assertNotVisible('.modal #coding-rules-custom-rule-creation-create');
          lib.clearRequestMock(this.createMock);
          lib.clearRequestMock(this.customRulesSearchMock);
          lib.clearRequestMock(this.searchMock);
          lib.fmock('/api/rules/create', 'create-create-custom-rules.json');
          this.customRulesSearchMock = lib.fmock('/api/rules/search', 'search-custom-rules2.json',
              { data: { template_key: 'squid:ArchitecturalConstraint' } });
          this.searchMock = lib.fmock('/api/rules/search', 'search-create-custom-rules.json');
          casper.click('.modal #coding-rules-custom-rule-creation-reactivate');
          lib.waitForElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 2);
        });
  });

  it('should create manual rule', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-create-manual-rule.json');
          lib.fmock('/api/rules/create', 'show-create-manual-rule.json');
          lib.fmock('/api/rules/show', 'show-create-manual-rule.json');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-create-manual-rule');
        })

        .then(function () {
          casper.click('.js-create-manual-rule');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('.modal [name="name"]').val('Manual Rule').keyup();
            jQuery('.modal [name="markdown_description"]').val('Manual Rule Description');
            jQuery('.modal #coding-rules-manual-rule-creation-create').click();
          });
          casper.waitForSelector('.coding-rules-detail-header');
        })

        .then(function () {
          test.assertSelectorContains('.coding-rules-detail-header', 'Manual Rule');
          test.assertSelectorContains('.coding-rule-details', 'manual:Manual_Rule');
          test.assertSelectorContains('.coding-rules-detail-description', 'Manual Rule Description');
        });
  });

  it('should reactivate manual rule', 7, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-create-manual-rule.json');
          this.createMock = lib.fmock('/api/rules/create', 'show-create-manual-rule.json', { status: 409 });
          lib.fmock('/api/rules/show', 'show-create-manual-rule.json');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-create-manual-rule');
        })

        .then(function () {
          casper.click('.js-create-manual-rule');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          test.assertNotVisible('.modal #coding-rules-manual-rule-creation-reactivate');
          test.assertVisible('.modal #coding-rules-manual-rule-creation-create');
          casper.evaluate(function () {
            jQuery('.modal [name="name"]').val('Manual Rule').keyup();
            jQuery('.modal [name="markdown_description"]').val('Manual Rule Description');
            jQuery('.modal #coding-rules-manual-rule-creation-create').click();
          });
          casper.waitForSelector('.modal .alert-warning');
        })

        .then(function () {
          test.assertVisible('.modal #coding-rules-manual-rule-creation-reactivate');
          test.assertNotVisible('.modal #coding-rules-manual-rule-creation-create');
          lib.clearRequestMock(this.createMock);
          lib.fmock('/api/rules/create', 'show.json');
          casper.click('.modal #coding-rules-manual-rule-creation-reactivate');
          casper.waitForSelector('.coding-rules-detail-header');
        })

        .then(function () {
          test.assertSelectorContains('.coding-rules-detail-header', 'Manual Rule');
          test.assertSelectorContains('.coding-rule-details', 'manual:Manual_Rule');
          test.assertSelectorContains('.coding-rules-detail-description', 'Manual Rule Description');
        });
  });

  it('should delete custom rules', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-delete-custom-rule-custom-rules.json',
              { data: { template_key: 'squid:ArchitecturalConstraint' } });
          lib.fmock('/api/rules/search', 'search-delete-custom-rule.json');
          lib.fmock('/api/rules/show', 'show-delete-custom-rule.json');
          lib.smock('/api/rules/delete', '{}');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('#coding-rules-detail-custom-rules .coding-rules-detail-list-name');
        })

        .then(function () {
          test.assertElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 2);
          casper.click('.js-delete-custom-rule');
          casper.click('[data-confirm="yes"]');
          lib.waitForElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 1);
        });
  });

  it('should delete manual rules', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          this.searchMock = lib.fmock('/api/rules/search', 'search-delete-manual-rule-before.json');
          lib.fmock('/api/rules/show', 'show-delete-manual-rule.json');
          lib.smock('/api/rules/delete', '{}');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('.js-delete');
        })

        .then(function () {
          casper.click('.js-delete');
          casper.waitForSelector('[data-confirm="yes"]');
        })

        .then(function () {
          lib.clearRequestMock(this.searchMock);
          lib.fmock('/api/rules/search', 'search-delete-manual-rule-after.json');
          casper.click('[data-confirm="yes"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', 0);
        });
  });

  it('should show custom rules', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-show-cutsom-rule-custom-rules.json',
              { data: { template_key: 'squid:ArchitecturalConstraint' } });
          lib.fmock('/api/rules/search', 'search-show-cutsom-rule.json');
          lib.fmock('/api/rules/show', 'show-show-cutsom-rule.json');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('#coding-rules-detail-custom-rules .coding-rules-detail-list-name');
        })

        .then(function () {
          test.assertExists('#coding-rules-detail-custom-rules');
          test.assertElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 2);
          test.assertSelectorContains('#coding-rules-detail-custom-rules .coding-rules-detail-list-name',
              'Do not use org.h2.util.StringUtils');
        });
  });

  it('should show deprecated label', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-deprecated.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected');
        })

        .then(function () {
          test.assertSelectorContains('.coding-rule.selected', 'DEPRECATED');
        });
  });

  it('should show rule details', 20, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-show-details.json');
          lib.fmock('/api/rules/show', 'show-show-details.json');
          lib.smock('/api/issues/search', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rules-detail-header');
        })

        .then(function () {
          test.assertSelectorContains('.search-navigator-workspace-details',
              'Throwable and Error classes should not be caught');

          test.assertSelectorContains('.search-navigator-workspace-details', 'squid:S1181');
          test.assertExists('.coding-rules-detail-properties .icon-severity-blocker');
          test.assertSelectorContains('.coding-rules-detail-properties', 'error-handling');
          test.assertSelectorContains('.coding-rules-detail-properties', '2013');
          test.assertSelectorContains('.coding-rules-detail-properties', 'SonarQube (Java)');
          test.assertSelectorContains('.coding-rules-detail-properties', 'Reliability > Exception handling');
          test.assertSelectorContains('.coding-rules-detail-properties', 'LINEAR');
          test.assertSelectorContains('.coding-rules-detail-properties', '20min');

          test.assertSelectorContains('.coding-rules-detail-description', 'is the superclass of all errors and');
          test.assertSelectorContains('.coding-rules-detail-description', 'its subclasses should be caught.');
          test.assertSelectorContains('.coding-rules-detail-description', 'Noncompliant Code Example');
          test.assertSelectorContains('.coding-rules-detail-description', 'Compliant Solution');

          test.assertSelectorContains('.coding-rules-detail-parameters', 'max');
          test.assertSelectorContains('.coding-rules-detail-parameters', 'Maximum authorized number of parameters');
          test.assertSelectorContains('.coding-rules-detail-parameters', '7');

          test.assertElementCount('.coding-rules-detail-quality-profile-name', 6);
          test.assertSelectorContains('.coding-rules-detail-quality-profile-name', 'Default - Top');
          test.assertElementCount('.coding-rules-detail-quality-profile-inheritance', 4);
          test.assertSelectorContains('.coding-rules-detail-quality-profile-inheritance', 'Default - Top');
        });
  });

  it('should show empty list', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-empty.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.search-navigator-facet-box');
        })

        .then(function () {
          test.assertDoesntExist('.coding-rule');
          test.assertSelectorContains('#coding-rules-total', 0);
          test.assertExists('.search-navigator-no-results');
        });
  });

  it('should show facets', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.search-navigator-facet-box');
        })

        .then(function () {
          test.assertElementCount('.search-navigator-facet-box', 13);
        });
  });

  it('should show rule', 7, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected');
        })

        .then(function () {
          test.assertSelectorContains('.coding-rule.selected', 'Values passed to SQL commands should be sanitized');
          test.assertSelectorContains('.coding-rule.selected', 'Java');
          test.assertSelectorContains('.coding-rule.selected', 'cwe');
          test.assertSelectorContains('.coding-rule.selected', 'owasp-top10');
          test.assertSelectorContains('.coding-rule.selected', 'security');
          test.assertSelectorContains('.coding-rule.selected', 'sql');
          test.assertSelectorContains('.coding-rule.selected', 'custom-tag');
        });
  });

  it('should show rule issues', 5, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
          lib.fmock('/api/rules/show', 'show.json');
          lib.fmock('/api/issues/search', 'issues-search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rules-most-violated-projects');
        })

        .then(function () {
          test.assertSelectorContains('.js-rule-issues', '7');
          test.assertSelectorContains('.coding-rules-most-violated-projects', 'SonarQube');
          test.assertSelectorContains('.coding-rules-most-violated-projects', '2');
          test.assertSelectorContains('.coding-rules-most-violated-projects', 'SonarQube Runner');
          test.assertSelectorContains('.coding-rules-most-violated-projects', '1');
        });
  });

  it('should show rules', 4, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertElementCount('.coding-rule', 25);
          test.assertSelectorContains('.coding-rule', 'Values passed to SQL commands should be sanitized');
          test.assertSelectorContains('.coding-rule',
              'An open curly brace should be located at the beginning of a line');
          test.assertSelectorContains('#coding-rules-total', '609');
        });
  });

  it('should move between rules from detailed view', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
          this.showMock = lib.fmock('/api/rules/show', 'show.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rules-detail-header');
        })

        .then(function () {
          test.assertSelectorContains('.coding-rules-detail-header',
              '".equals()" should not be used to test the values');

          lib.clearRequestMock(this.showMock);
          this.showMock = lib.fmock('/api/rules/show', 'show2.json');

          casper.click('.js-next');
          casper.waitForSelectorTextChange('.coding-rules-detail-header');
        })

        .then(function () {
          test.assertSelectorContains('.coding-rules-detail-header', '"@Override" annotation should be used on any');

          lib.clearRequestMock(this.showMock);
          this.showMock = lib.fmock('/api/rules/show', 'show.json');

          casper.click('.js-prev');
          casper.waitForSelectorTextChange('.coding-rules-detail-header');
        })

        .then(function () {
          test.assertSelectorContains('.coding-rules-detail-header',
              '".equals()" should not be used to test the values');
        });
  });

  it('should filter similar rules', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-sql-tag.json', { data: { tags: 'sql' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected .js-rule-filter');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');

          casper.click('.js-rule-filter');
          casper.waitForSelector('.bubble-popup');
        })

        .then(function () {
          test.assertExists('.bubble-popup [data-property="languages"][data-value="java"]');

          casper.click('.bubble-popup [data-property="tags"][data-value="sql"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '2');
        });
  });

  it('should show active severity facet', 7, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-BLOCKER.json', { data: { active_severities: 'BLOCKER' } });
          lib.fmock('/api/rules/search', 'active-severities-facet.json',
              { data: { facets: 'active_severities', ps: '1' } });
          lib.fmock('/api/rules/search', 'search-qprofile.json',
              { data: { qprofile: 'java-default-with-mojo-conventions-49307' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
          test.assertExists('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        })

        .then(function () {
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '407');
          test.assertDoesntExist('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
          casper.click('[data-property="active_severities"] .js-facet-toggle');
          casper.waitForSelector('[data-property="active_severities"] [data-value="BLOCKER"]');
        })

        .then(function () {
          casper.click('[data-property="active_severities"] [data-value="BLOCKER"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '4');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
          test.assertExists('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
        });
  });

  it('should show available since facet', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-limited.json', { data: { available_since: '2014-12-01' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
          casper.click('[data-property="available_since"] .js-facet-toggle');
          casper.evaluate(function () {
            jQuery('[data-property="available_since"] input').val('2014-12-01').change();
          });
        })

        .then(function () {
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '101');
        });
  });

  it('should bulk activate', 7, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
          lib.smock('/api/qualityprofiles/activate_rules', '{ "succeeded": 225 }');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertExists('.js-bulk-change');
          casper.click('.js-bulk-change');
          casper.waitForSelector('.bubble-popup');
        })

        .then(function () {
          test.assertExists('.bubble-popup .js-bulk-change[data-action="activate"]');
          casper.click('.js-bulk-change[data-action="activate"]');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          test.assertExists('.modal #coding-rules-bulk-change-profile');
          test.assertExists('.modal #coding-rules-submit-bulk-change');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('#coding-rules-bulk-change-profile').val('java-default-with-mojo-conventions-49307');
          });
        })

        .then(function () {
          casper.click('.modal #coding-rules-submit-bulk-change');
          casper.waitForSelector('.modal .alert-success');
        })

        .then(function () {
          test.assertSelectorContains('.modal', 'Default - Maven Conventions');
          test.assertSelectorContains('.modal', 'Java');
          test.assertSelectorContains('.modal', '225');
        });
  });

  it('should fail to bulk activate', 6, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
          lib.smock('/api/qualityprofiles/activate_rules', '{ "succeeded": 225, "failed": 395 }');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertExists('.js-bulk-change');
          casper.click('.js-bulk-change');
          casper.waitForSelector('.bubble-popup');
        })

        .then(function () {
          test.assertExists('.bubble-popup .js-bulk-change[data-action="activate"]');
          casper.click('.js-bulk-change[data-action="activate"]');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          test.assertExists('.modal #coding-rules-bulk-change-profile');
          test.assertExists('.modal #coding-rules-submit-bulk-change');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('#coding-rules-bulk-change-profile').val('java-default-with-mojo-conventions-49307');
          });
        })

        .then(function () {
          casper.click('.modal #coding-rules-submit-bulk-change');
          casper.waitForSelector('.modal .alert-warning');
        })

        .then(function () {
          test.assertSelectorContains('.modal', '225');
          test.assertSelectorContains('.modal', '395');
        });
  });

  it('should filter profiles by language during bulk change', 4, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          casper.click('.js-facet[data-value="java"]');
          test.assertExists('.js-bulk-change');
          casper.click('.js-bulk-change');
          casper.waitForSelector('.bubble-popup');
        })

        .then(function () {
          test.assertExists('.bubble-popup .js-bulk-change[data-action="activate"]');
          casper.click('.js-bulk-change[data-action="activate"]');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          test.assertExists('.modal #coding-rules-bulk-change-profile');
          test.assertEqual(8, casper.evaluate(function () {
            return jQuery('.modal').find('#coding-rules-bulk-change-profile').find('option').length;
          }));
        });
  });

  it('should change selected profile during bulk change', 4, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-qprofile-active.json',
              { data: { activation: true } });
          lib.fmock('/api/rules/search', 'search.json');
          lib.smock('/api/qualityprofiles/deactivate_rules', '{ "succeeded": 7 }');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        })

        .then(function () {
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertExists('.js-bulk-change');
          casper.click('.js-bulk-change');
          casper.waitForSelector('.bubble-popup');
        })

        .then(function () {
          test.assertExists('.bubble-popup .js-bulk-change[data-param="java-default-with-mojo-conventions-49307"]');
          casper.click('.js-bulk-change[data-param="java-default-with-mojo-conventions-49307"]');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          test.assertDoesntExist('.modal #coding-rules-bulk-change-profile');
          casper.click('.modal #coding-rules-submit-bulk-change');
          casper.waitForSelector('.modal .alert-success');
        })

        .then(function () {
          test.assertSelectorContains('.modal', '7');
        });
  });

  it('should show characteristic facet', 6, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();

          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-with-portability-characteristic.json',
              { data: { debt_characteristics: 'PORTABILITY' } });
          lib.fmock('/api/rules/search', 'search-with-memory-efficiency-characteristic.json',
              { data: { debt_characteristics: 'MEMORY_EFFICIENCY' } });
          lib.fmock('/api/rules/search', 'search-without-characteristic.json',
              { data: { has_debt_characteristic: 'false' } });
          lib.fmock('/api/rules/search', 'search-characteristic.json',
              { data: { facets: 'debt_characteristics' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          // enable facet
          test.assertExists('.search-navigator-facet-box-collapsed[data-property="debt_characteristics"]');
          casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="PORTABILITY"]');
        })

        .then(function () {
          // select characteristic
          test.assertElementCount('[data-property="debt_characteristics"] .js-facet', 32);
          test.assertElementCount('[data-property="debt_characteristics"] .js-facet.search-navigator-facet-indent', 24);
          casper.click('.js-facet[data-value="PORTABILITY"]');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 21);
          });
        })

        .then(function () {
          // select uncharacterized
          casper.click('.js-facet[data-empty-characteristic]');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 208);
          });
        })

        .then(function () {
          // select sub-characteristic
          casper.click('.js-facet[data-value="MEMORY_EFFICIENCY"]');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 3);
          });
        });
  });

  it('should disable characteristic facet', 6, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-with-portability-characteristic.json',
              { data: { debt_characteristics: 'PORTABILITY' } });
          lib.fmock('/api/rules/search', 'search-with-memory-efficiency-characteristic.json',
              { data: { debt_characteristics: 'MEMORY_EFFICIENCY' } });
          lib.fmock('/api/rules/search', 'search-without-characteristic.json',
              { data: { has_debt_characteristic: 'false' } });
          lib.fmock('/api/rules/search', 'search-characteristic.json',
              { data: { facets: 'debt_characteristics' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          // enable facet
          casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="PORTABILITY"]');
        })

        .then(function () {
          // select characteristic
          casper.click('.js-facet[data-value="PORTABILITY"]');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 21);
          });
        })

        .then(function () {
          // disable facet
          casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 609);
          });
        })

        .then(function () {
          // enable facet
          casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="MEMORY_EFFICIENCY"]');
        })

        .then(function () {
          // select sub-characteristic
          casper.click('.js-facet[data-value="MEMORY_EFFICIENCY"]');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 3);
          });
        })

        .then(function () {
          // disable facet
          casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 609);
          });
        })

        .then(function () {
          // enable facet
          casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-empty-characteristic]');
        })

        .then(function () {
          // select uncharacterized
          casper.click('.js-facet[data-empty-characteristic]');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 208);
          });
        })

        .then(function () {
          // disable facet
          casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 609);
          });
        });
  });

  it('should show template facet', 4, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-only-templates.json', { data: { 'is_template': 'true' } });
          lib.fmock('/api/rules/search', 'search-hide-templates.json', { data: { 'is_template': 'false' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          // enable facet
          test.assertExists('.search-navigator-facet-box-collapsed[data-property="is_template"]');
          casper.click('[data-property="is_template"] .js-facet-toggle');
          casper.waitForSelector('[data-property="is_template"] .js-facet[data-value="true"]');
        })

        .then(function () {
          // show only templates
          casper.click('[data-property="is_template"] .js-facet[data-value="true"]');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 8);
          });
        })

        .then(function () {
          // hide templates
          casper.click('[data-property="is_template"] .js-facet[data-value="false"]');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 7);
          });
        })

        .then(function () {
          // disable facet
          casper.click('[data-property="is_template"] .js-facet-toggle');
          casper.waitForSelectorTextChange('#coding-rules-total', function () {
            test.assertSelectorContains('#coding-rules-total', 609);
          });
        });
  });

  it('should show language facet', 5, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();

          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-with-custom-language.json', { data: { languages: 'custom' } });
          lib.fmock('/api/rules/search', 'search.json');
          lib.smock('/api/languages/list', '{"languages":[{"key":"custom","name":"Custom"}]}',
              { data: { q: 'custom' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          casper.click('[data-property="languages"] .select2-choice');
          casper.waitForSelector('.select2-search', function () {
            casper.evaluate(function () {
              jQuery('.select2-input').val('custom').trigger('keyup-change');
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.select2-result');
        })

        .then(function () {
          test.assertSelectorContains('.select2-result', 'Custom');
          casper.evaluate(function () {
            jQuery('.select2-result').mouseup();
          });
        })

        .then(function () {
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', 13);
          test.assertExists('[data-property="languages"] .js-facet.active');
          test.assertSelectorContains('[data-property="languages"] .js-facet.active', 'custom');
          test.assertSelectorContains('[data-property="languages"] .js-facet.active', '13');
        });
  });

  it('should reload results', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();

          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          this.searchMock = lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', 609);
          lib.clearRequestMock(this.searchMock);
          lib.fmock('/api/rules/search', 'search2.json');
          casper.click('.js-reload');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', 413);
        });
  });

  it('should do a new search', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search2.json', { data: { languages: 'java' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', 609);
          casper.click('.js-facet[data-value="java"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', 413);
          casper.click('.js-new-search');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', 609);
        });
  });

  it('should go back', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search.json');
          lib.fmock('/api/rules/show', 'show.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule.selected', function () {
            casper.click('.coding-rule.selected .js-rule');
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rules-detail-header');
        })

        .then(function () {
          casper.click('.js-back');
          test.assertDoesntExist('.js-back');
          test.assertDoesntExist('.coding-rules-detail-header');
        });
  });

  it('should show inheritance facet', 11, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-not-inherited.json', { data: { inheritance: 'NONE' } });
          lib.fmock('/api/rules/search', 'search-inherited.json', { data: { inheritance: 'INHERITED' } });
          lib.fmock('/api/rules/search', 'search-overriden.json', { data: { inheritance: 'OVERRIDES' } });
          lib.fmock('/api/rules/search', 'search-qprofile.json',
              { data: { qprofile: 'java-default-with-mojo-conventions-49307' } });
          lib.fmock('/api/rules/search', 'search-qprofile2.json',
              { data: { qprofile: 'java-top-profile-without-formatting-conventions-50037' } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
          test.assertExists('.search-navigator-facet-box-forbidden[data-property="inheritance"]');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        })

        .then(function () {
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '407');
          test.assertDoesntExist('.search-navigator-facet-box-forbidden[data-property="inheritance"]');
          casper.click('[data-property="inheritance"] .js-facet-toggle');
          casper.waitForSelector('[data-property="inheritance"] [data-value="NONE"]');
        })

        .then(function () {
          casper.click('[data-property="inheritance"] [data-value="NONE"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '103');
          casper.click('[data-property="inheritance"] [data-value="INHERITED"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '101');
          casper.click('[data-property="inheritance"] [data-value="OVERRIDES"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '102');
          casper.click('.js-facet[data-value="java-top-profile-without-formatting-conventions-50037"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '408');
          test.assertExists('.search-navigator-facet-box-forbidden[data-property="inheritance"]');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertSelectorContains('#coding-rules-total', '609');
          test.assertExists('.search-navigator-facet-box-forbidden[data-property="inheritance"]');
        });
  });

  it('should show activation details', 5, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-actives.json', { data: { activation: true } });
          lib.fmock('/api/rules/search', 'search.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertDoesntExist('.coding-rule-activation');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        })

        .then(function () {
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertElementCount('.coding-rule-activation', 2);
          test.assertElementCount('.coding-rule-activation .icon-severity-major', 2);
          test.assertElementCount('.coding-rule-activation .icon-inheritance', 1);
          test.assertDoesntExist('.coding-rules-detail-quality-profile-activate');
        });
  });

  it('should activate rule', 9, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-inactive.json', { data: { activation: 'false' } });
          lib.fmock('/api/rules/search', 'search.json');
          lib.smock('/api/qualityprofiles/activate_rule', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertDoesntExist('.coding-rule-activation');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        })

        .then(function () {
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"] .js-inactive');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertDoesntExist('.coding-rule-activation .icon-severity-major');
          test.assertExists('.coding-rules-detail-quality-profile-activate');
          casper.click('.coding-rules-detail-quality-profile-activate');
          casper.waitForSelector('.modal');
        })

        .then(function () {
          test.assertExists('#coding-rules-quality-profile-activation-select');
          test.assertElementCount('#coding-rules-quality-profile-activation-select option', 1);
          test.assertExists('#coding-rules-quality-profile-activation-severity');
          casper.click('#coding-rules-quality-profile-activation-activate');
          casper.waitForSelector('.coding-rule-activation .icon-severity-major');
        })

        .then(function () {
          test.assertExist('.coding-rule-activation .icon-severity-major');
          test.assertDoesntExist('.coding-rules-detail-quality-profile-activate');
          test.assertExist('.coding-rules-detail-quality-profile-deactivate');
        });
  });

  it('should deactivate rule', 6, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.smock('/api/l10n/index', '{}');
          lib.fmock('/api/rules/app', 'app.json');
          lib.fmock('/api/rules/search', 'search-active.json', { data: { activation: true } });
          lib.fmock('/api/rules/search', 'search.json');
          lib.smock('/api/qualityprofiles/deactivate_rule', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/coding-rules/app'], function (App) {
              App.start({ el: '#content' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.coding-rule');
        })

        .then(function () {
          test.assertDoesntExist('.coding-rule-activation');
          casper.click('[data-property="qprofile"] .js-facet-toggle');
          casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        })

        .then(function () {
          casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
          casper.waitForSelectorTextChange('#coding-rules-total');
        })

        .then(function () {
          test.assertExists('.coding-rule-activation .icon-severity-major');
          test.assertDoesntExist('.coding-rules-detail-quality-profile-activate');
          casper.click('.coding-rules-detail-quality-profile-deactivate');
          casper.waitForSelector('button[data-confirm="yes"]');
        })

        .then(function () {
          casper.click('button[data-confirm="yes"]');
          casper.waitWhileSelector('.coding-rule-activation .icon-severity-major');
        })

        .then(function () {
          test.assertDoesntExist('.coding-rule-activation .icon-severity-major');
          test.assertExist('.coding-rules-detail-quality-profile-activate');
          test.assertDoesntExist('.coding-rules-detail-quality-profile-deactivate');
        });
  });

});
