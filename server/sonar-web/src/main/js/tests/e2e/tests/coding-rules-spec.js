var lib = require('../lib'),
    testName = lib.testName('Coding Rules');


lib.initMessages();
lib.changeWorkingDirectory('coding-rules-spec');


casper.test.begin(testName('Readonly Tests'), function suite(test) {

  var appId = null;
  var showId = null;

  casper.start(lib.buildUrl('coding-rules'), function() {
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_initial.json');
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkInitialPageLoad() {

    casper.waitForSelector('.navigator-header', function checkHeader() {
      test.assertExists('.navigator-header h1');
      test.assertExists('button#coding-rules-new-search');
      test.assertDoesntExist('button#coding-rules-create-rule');
    });


    casper.waitForSelector('.navigator-filters', function checkDefaultFilters() {
      test.assertVisible('input[type="text"].query-filter-input');
      test.assertElementCount('.navigator-filter', 15);
      test.assertElementCount('.navigator-filter-optional', 12 /* Only query, qProfile and 'More' are visible by default */);
      test.assertVisible('button.navigator-filter-submit');


      casper.click('.navigator-filter-more-criteria');
      casper.waitUntilVisible('.navigator-filter-details.active', function checkTagsAreOrdered() {
        casper.click('.navigator-filter-details.active label[data-property="tags"]');
        test.assertSelectorHasText('.navigator-filter[data-property="tags"] option:nth-child(1)', 'brain-overload');
        test.assertSelectorHasText('.navigator-filter[data-property="tags"] option:nth-child(11)', 'unused');
        casper.click('.navigator-filter.active>.navigator-filter-disable');
      });

      // Check repositories are sorted by name, then language
      test.assertSelectorHasText('#filter-repositories li:nth-child(1) span:nth-child(1)', 'Common SonarQube');
      test.assertSelectorHasText('#filter-repositories li:nth-child(1) span:nth-child(2)', 'CoffeeScript');
      test.assertSelectorHasText('#filter-repositories li:nth-child(2) span:nth-child(1)', 'Common SonarQube');
      test.assertSelectorHasText('#filter-repositories li:nth-child(2) span:nth-child(2)', 'Java');
      test.assertSelectorHasText('#filter-repositories li:nth-child(3) span:nth-child(1)', 'Manual Rules');
      test.assertSelectorHasText('#filter-repositories li:nth-child(3) span:nth-child(2)', 'None');
      test.assertSelectorHasText('#filter-repositories li:nth-child(4) span:nth-child(1)', 'SonarQube');
      test.assertSelectorHasText('#filter-repositories li:nth-child(4) span:nth-child(2)', 'CoffeeScript');
      test.assertSelectorHasText('#filter-repositories li:nth-child(5) span:nth-child(1)', 'SonarQube');
      test.assertSelectorHasText('#filter-repositories li:nth-child(5) span:nth-child(2)', 'Java');
      test.assertSelectorHasText('#filter-repositories li:nth-child(6) span:nth-child(1)', 'SonarQube');
      test.assertSelectorHasText('#filter-repositories li:nth-child(6) span:nth-child(2)', 'Xoo');
    });


    casper.waitForSelector('li.active', function checkResultsList() {
      test.assertElementCount('ol.navigator-results-list li', 10);
      test.assertElementCount('li.active', 1);
      test.assertSelectorHasText('ol.navigator-results-list li.active', 'Xoo');
      test.assertSelectorHasText('ol.navigator-results-list li.active', 'No empty line');
      test.assertSelectorHasText('ol.navigator-results-list li.active', 'BETA');
    });


    casper.waitForSelector('h3.coding-rules-detail-header', function showFirstRule() {
      test.assertSelectorHasText('h3.coding-rules-detail-header', 'No empty line');
      test.assertSelectorHasText('.navigator-details .subtitle', 'squid-xoo:x1');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(1)', 'severity.MINOR');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(2)', 'BETA');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(3)', 'convention, pitfall');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(4)', 'Testability > Integration level testability');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(6)', 'SonarQube (Xoo)');


      casper.click('.coding-rules-subcharacteristic');
      casper.waitForSelector('.coding-rules-debt-popup', function checkDebtPopup() {
        test.assertElementCount('ul.bubble-popup-list li', 3);
        test.assertSelectorHasText('.bubble-popup-list li:nth-child(1)', 'LINEAR_OFFSET');
        test.assertSelectorHasText('.bubble-popup-list li:nth-child(2)', '1h');
        test.assertSelectorHasText('.bubble-popup-list li:nth-child(3)', '30min');
      });


      test.assertDoesntExist('button#coding-rules-detail-extend-description');


      casper.then(function checkParameters() {
        test.assertElementCount('.coding-rules-detail-parameter', 3);
        test.assertVisible('.coding-rules-detail-parameter-description[data-key=acceptWhitespace]');
        test.assertSelectorHasText('.coding-rules-detail-parameter-description[data-key=acceptWhitespace]', 'Accept whitespace');
        casper.click('.coding-rules-detail-parameter:nth-child(1) .coding-rules-detail-parameter-name');
        test.assertNotVisible('.coding-rules-detail-parameter-description[data-key=acceptWhitespace]');
        casper.click('.coding-rules-detail-parameter:nth-child(1) .coding-rules-detail-parameter-name');
        test.assertVisible('.coding-rules-detail-parameter-description[data-key=acceptWhitespace]');
      });
    });
  });

  casper.then(function showDeprecated() {
    lib.clearRequestMock(showId);
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_deprecated.json');

    casper.click('div[name="squid:S0001"]');

    casper.waitWhileSelector('.navigator-details i.spinner');
    casper.waitForSelector('h3.coding-rules-detail-header', function() {
      test.assertSelectorHasText('h3.coding-rules-detail-header', 'Deprecated rule');
    });

  });

  casper.run(function() {
    test.done();
  });
});


casper.test.begin(testName('Admin Tests'), function suite(test) {

  var showId = null;
  var updateId = null;

  casper.start(lib.buildUrl('coding-rules'), function() {
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app_admin.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_initial.json');
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkInitialPageLoad() {

    casper.waitForSelector('.navigator-header', function checkAdminHeader() {
      test.assertExist('button#coding-rules-create-rule');
    });


    casper.waitForSelector('h3.coding-rules-detail-header', function showFirstRule() {
      test.assertSelectorHasText('.coding-rules-detail-description-extra', 'Xoo shall not pass');

      test.assertExists('button#coding-rules-detail-extend-description');
    });


    casper.then(function editNote() {
      casper.click('button#coding-rules-detail-extend-description');
      test.assertSelectorHasText('textarea#coding-rules-detail-extend-description-text', 'As per the [Book of Xoo](http://xoo.sonarsource.com/book):\n> Xoo shall not pass!');

      casper.sendKeys('textarea#coding-rules-detail-extend-description-text', 'Xoo must pass');

      updateId = lib.mockRequestFromFile('/api/rules/update', 'edit_note_x1.json');
      casper.click('button#coding-rules-detail-extend-description-submit');

      casper.wait(500, function showUpdatedRule() {
        test.assertSelectorHasText('.coding-rules-detail-description-extra', 'Xoo must pass');
      });

    });
  });

  casper.run(function() {
    test.done();
  });
});


casper.test.begin(testName('Activation Tests'), function suite(test) {

  var showId = null;
  var activateId = null;

  casper.start(lib.buildUrl('coding-rules#rule_key=squid-xoo:x1'), function() {
    lib.clearRequestMocks();
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app_admin.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_x1.json');
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkRuleActivation() {

    casper.waitForSelector('div.coding-rules-detail-quality-profiles-section', function showRuleActivation() {
      test.assertExists('#coding-rules-detail-quality-profiles');

      test.assertElementCount('.coding-rules-detail-quality-profile', 1);

      test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'A nice');
      test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'true');
      test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', '0');
    });


    casper.then(function editActiveRule() {
      casper.click('button.coding-rules-detail-quality-profile-change');

      casper.waitForSelector('button#coding-rules-quality-profile-activation-activate', function checkActivationPopup() {
        test.assertElementCount('.modal-body .property', 5 /* quality profile, severity, 3 parameters */);

        test.assertSelectorHasText('.modal-body .property textarea[name="textParameter"]', 'A nice\ntext parameter\nwith newlines.');
        test.assertSelectorHasText('.modal-body .property input[name="skipLines"]', '');
        test.assertEval(function checkPlaceHolder() {
          return $j('.modal-body .property input[name="skipLines"]').attr('placeholder') === '0'
        });
        test.assertEval(function checkTrueIsSelected() {
          return $j('.modal-body .property select[name="acceptWhitespace"]').val() === 'true'
        });
      });
    });

    casper.then(function updateParameters() {
      casper.sendKeys('textarea[name="textParameter"]', '\nUpdated');
      casper.sendKeys('input[name="skipLines"]', '5');
      casper.evaluate(function selectDefault() {
          $j('select[name="acceptWhitespace"]').val('false').change();
      });

      lib.clearRequestMock(showId);
      showId = lib.mockRequestFromFile('/api/rules/show', 'update_parameters_x1.json');
      activateId = lib.mockRequest('/api/qualityprofiles/activate_rule', '');

      casper.click('button#coding-rules-quality-profile-activation-activate');

      casper.wait(500, function showUpdatedParameters() {
        test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'A nice');
        test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'false');
        test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', '5');
      });

    });

    casper.then(function deactivateRule() {
      casper.click('button.coding-rules-detail-quality-profile-deactivate');

      casper.waitForSelector('button[data-confirm="yes"]', function checkConfirmPopup() {
        lib.clearRequestMock(showId);
        showId = lib.mockRequestFromFile('/api/rules/show', 'deactivate_x1.json');
        lib.mockRequest('/api/qualityprofiles/deactivate_rule', '');

        casper.click('button[data-confirm="yes"]');
      });

      casper.wait(500, function showUpdatedParameters() {
        test.assertElementCount('.coding-rules-detail-quality-profile', 0);
      });
    });

    casper.then(function activateRule() {
      casper.click('button#coding-rules-quality-profile-activate');

      casper.waitForSelector('button#coding-rules-quality-profile-activation-activate', function checkActivationPopup() {

        lib.clearRequestMock(showId);
        showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
        lib.clearRequestMock(activateId);
        activateId = lib.mockRequest('/api/qualityprofiles/activate_rule', '');

        casper.click('button#coding-rules-quality-profile-activation-activate');

        casper.wait(500, function showUpdatedParameters() {
          test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'A nice');
          test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', 'true');
          test.assertSelectorHasText('.coding-rules-detail-quality-profile-parameter', '0');
        });

      });

    });

  });


  casper.run(function() {
    test.done();
  });
});


casper.test.begin(testName('Tag Navigation Test'), function suite(test) {

  casper.start(lib.buildUrl('coding-rules#tags=polop,bug,pilip,unused,palap'), function() {
    lib.clearRequestMocks();
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app_admin.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_x1.json');
    lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkTagFilterRestored() {
    casper.waitForSelector('.navigator-filters', function checkDefaultFilters() {
      test.assertElementCount('.navigator-filter-disabled', 11 /* Tag is enabled */);
      test.assertSelectorHasText('.navigator-filter[data-property="tags"] .navigator-filter-value', 'bug, unused');
    });
  });

  casper.run(function() {
    test.done();
  });
});
