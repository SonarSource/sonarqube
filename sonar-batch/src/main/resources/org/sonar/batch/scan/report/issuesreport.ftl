<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <title>Issues report of ${report.getTitle()?html}</title>
  <link href="issuesreport_files/sonar.css" media="all" rel="stylesheet" type="text/css">
  <link rel="shortcut icon" type="image/x-icon" href="issuesreport_files/favicon.ico">
  <script type="text/javascript" src="issuesreport_files/jquery.min.js"></script>
  <script type="text/javascript">
    var issuesPerResource = [
    <#list report.getResourceReports() as resourceReport>
      [
        <#assign issues=resourceReport.getIssues()>
        <#list issues as issue>
          <#if complete || issue.isNew()>
          {'k': '${issue.key()}', 'r': 'R${issue.getRuleKey()}', 'l': ${(issue.startLine()!0)?c}, 'new': ${issue.isNew()?string}, 's': '${issue.severity()?lower_case}'}<#if issue_has_next>,</#if>
          </#if>
        </#list>
      ]
      <#if resourceReport_has_next>,</#if>
    </#list>
    ];
    var nbResources = ${report.getResourcesWithReport()?size?c};
    var separators = new Array();

    function showLine(fileIndex, lineId) {
      var elt = $('#' + fileIndex + 'L' + lineId);
      if (elt != null) {
        elt.show();
      }
      elt = $('#' + fileIndex + 'LV' + lineId);
      if (elt != null) {
        elt.show();
      }
    }

    /* lineIds must be sorted */
    function showLines(fileIndex, lineIds) {
      var lastSeparatorId = 9999999;
      for (var lineIndex = 0; lineIndex < lineIds.length; lineIndex++) {
        var lineId = lineIds[lineIndex];
        if (lineId > 0) {
          if (lineId > lastSeparatorId) {
            var separator = $('#' + fileIndex + 'S' + lastSeparatorId);
            if (separator != null) {
              separator.addClass('visible');
              separators.push(separator);
            }
          }

          for (var i = -2; i < 3; ++i) {
            showLine(fileIndex, lineId + i);
          }
          lastSeparatorId = lineId + 2;
        }
      }
    }
     function hideAll() {
       $('tr.row').hide();
       $('div.issue').hide();
       for (var separatorIndex = 0; separatorIndex < separators.length; separatorIndex++) {
         separators[separatorIndex].removeClass('visible');
       }
       separators.length = 0;
       $('.sources td.ko').removeClass('ko');
     }

     function showIssues(fileIndex, issues) {
       $.each(issues, function(index, issue) {
         $('#' + issue['k']).show();
         $('#' + fileIndex + 'L' + issue['l'] + ' td.line').addClass('ko');
       });
       var showResource = issues.length > 0;
       if (showResource) {
         $('#resource-' + fileIndex).show();
       } else {
         $('#resource-' + fileIndex).hide();
       }
     }


    function refreshFilters(updateSelect) {
      <#if complete>
      var onlyNewIssues = $('#new_filter').is(':checked');
      <#else>
      var onlyNewIssues = true;
      </#if>

      if (updateSelect) {
        populateSelectFilter(onlyNewIssues);
      }
      var ruleFilter = $('#rule_filter').val();

      hideAll();
      if (onlyNewIssues) {
        $('.all').addClass('all-masked');
      } else {
        $('.all').removeClass('all-masked');
      }
      for (var resourceIndex = 0; resourceIndex < nbResources; resourceIndex++) {
        var filteredIssues = $.grep(issuesPerResource[resourceIndex], function(v) {
              return (!onlyNewIssues || v['new']) && (ruleFilter == '' || v['r'] == ruleFilter || v['s'] == ruleFilter);
            }
        );

        var linesToDisplay = $.map(filteredIssues, function(v, i) {
          return v['l'];
        });

        linesToDisplay.sort();// the showLines() requires sorted ids
        showLines(resourceIndex, linesToDisplay);
        showIssues(resourceIndex, filteredIssues);
      }
    }


    var severityFilter = [
    <#assign severities = report.getSummary().getTotalBySeverity()>
       <#list severities?keys as severity>
       { "key": "${severity?lower_case}",
         "label": "${severity?lower_case?cap_first}",
         "total": ${severities[severity].getCountInCurrentAnalysis()?c},
         "newtotal": ${severities[severity].getNewIssuesCount()?c}
       }<#if severity_has_next>,</#if>
       </#list>
    ];

    var ruleFilter = [
    <#assign rules = report.getSummary().getTotalByRuleKey()>
       <#list rules?keys as ruleKey>
       { "key": "${ruleKey}",
         "label": "${ruleNameProvider.nameForJS(ruleKey)}",
         "total": ${rules[ruleKey].getCountInCurrentAnalysis()?c},
         "newtotal": ${rules[ruleKey].getNewIssuesCount()?c}
       }<#if ruleKey_has_next>,</#if>
       </#list>
    ].sort(function(a, b) {
        var x = a.label; var y = b.label;
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    });

    function populateSelectFilter(onlyNewIssues) {
       var ruleFilterSelect = $('#rule_filter');
       ruleFilterSelect.empty().append(function() {
         var output = '';
         output += '<option value="" selected>Filter by:</option>';
         output += '<optgroup label="Severity">';
         $.each(severityFilter, function(key, value) {
           if ((!onlyNewIssues && value.total > 0) || value.newtotal > 0) {
             output += '<option value="' + value.key + '">' + value.label + ' (' + (onlyNewIssues ? value.newtotal : value.total) + ')</option>';
           }
         });
         output += '<optgroup label="Rule">';
         $.each(ruleFilter, function(key, value) {
           if ((!onlyNewIssues && value.total > 0) || value.newtotal > 0) {
             output += '<option value="R' + value.key + '">' + value.label + ' (' + (onlyNewIssues ? value.newtotal : value.total) + ')</option>';
           }
         });
         return output;
       });
    }
  </script>
</head>
<body>
<div id="reportHeader">
  <div id="logo"><img src="issuesreport_files/sonarqube-24x100.png" alt="SonarQube"/></div>
  <div class="title">Issues Report</div>
  <div class="subtitle">${report.getTitle()?html} - ${report.getDate()?datetime}</div>
</div>

<#if report.isNoFile()>
<div id="content">
  <div class="banner">No file analyzed</div>
</div>
<#else>
<div id="content">

  <#if !complete>
  <div class="banner">Light report: only new issues are displayed</div>
  </#if>

  <div id="summary">
  <table width="100%">
    <tbody>
    <tr>
    <#if complete>
      <#assign size = '33'>
    <#else>
      <#assign size = '50'>
    </#if>
      <td align="center" width="${size}%">
        <h3>New issues</h3>
      <#if report.getSummary().getTotal().getNewIssuesCount() gt 0>
        <span class="big worst">${report.getSummary().getTotal().getNewIssuesCount()?c}</span>
      <#else>
        <span class="big">0</span>
      </#if>
      </td>
      <td align="center" width="${size}%">
        <h3>Resolved issues</h3>
      <#if report.getSummary().getTotal().getResolvedIssuesCount() gt 0>
        <span class="big better">${report.getSummary().getTotal().getResolvedIssuesCount()?c}</span>
      <#else>
        <span class="big">0</span>
      </#if>
      </td>
    <#if complete>
      <td align="center" width="${size}%" class="all">
        <h3>Issues</h3>
        <span class="big">${report.getSummary().getTotal().getCountInCurrentAnalysis()?c}</span>
      </td>
    </#if>
    </tr>
    </tbody>
  </table>
  <#if complete>
  <br/>
  <table width="100%" class="data">
    <thead>
    <tr class="total">
      <th colspan="2" align="left">
          Issues per Rule
      </th>
      <th align="right" width="1%" nowrap>New issues</th>
      <th align="right" width="1%" nowrap>Resolved issues</th>
      <th align="right" width="1%" nowrap class="all">Issues</th>
    </tr>
    </thead>
    <tbody>
      <#list report.getSummary().getRuleReports() as ruleReport>
        <#if complete || (ruleReport.getTotal().getNewIssuesCount() > 0)>
          <#if ruleReport.getTotal().getNewIssuesCount() = 0>
          <#assign trCss = 'all'>
          <#else>
          <#assign trCss = ''>
          </#if>
      <tr class="hoverable ${trCss}">
        <td width="20">
          <i class="icon-severity-${ruleReport.getSeverity()?lower_case}"></i>
        </td>
        <td align="left">
          ${ruleNameProvider.nameForHTML(ruleReport.getRule())}
        </td>
        <td align="right">
          <#if ruleReport.getTotal().getNewIssuesCount() gt 0>
            <span class="worst">${ruleReport.getTotal().getNewIssuesCount()?c}</span>
          <#else>
            <span>0</span>
          </#if>
        </td>
        <td align="right">
          <#if ruleReport.getTotal().getResolvedIssuesCount() gt 0>
            <span class="better">${ruleReport.getTotal().getResolvedIssuesCount()?c}</span>
          <#else>
            <span>0</span>
          </#if>
        </td>
        <td align="right" class="all">
          ${ruleReport.getTotal().getCountInCurrentAnalysis()?c}
        </td>
      </tr>
        </#if>
      </#list>
    </tbody>
  </table>
  </#if>
  </div>

  <br/>

  <div class="banner">
  <#if complete>
    <input type="checkbox" id="new_filter" onclick="refreshFilters(true)" checked="checked" /> <label for="new_filter">Only NEW
    issues</label>
    &nbsp;&nbsp;&nbsp;&nbsp;
  </#if>

    <select id="rule_filter" onchange="refreshFilters(false)">
    </select>
  </div>

  <div id="summary-per-file">
  <#list report.getResourceReports() as resourceReport>
    <#if complete || (resourceReport.getTotal().getNewIssuesCount() > 0)>
      <#assign issueId=0>
      <#if resourceReport.getTotal().getNewIssuesCount() = 0>
      <#assign tableCss = 'all'>
      <#else>
      <#assign tableCss = ''>
      </#if>
  <table width="100%" class="data ${tableCss}" id="resource-${resourceReport_index?c}">
    <thead>
    <tr class="total">
      <th align="left" colspan="2" nowrap>
        <div class="file_title">
          <img src="issuesreport_files/${resourceReport.getType()}.png" title="Resource icon"/>
          <a href="#" onclick="$('.resource-details-${resourceReport_index?c}').toggleClass('masked'); return false;" style="color: black">${resourceReport.getName()}</a>
        </div>
      </th>
      <th align="right" width="1%" nowrap class="resource-details-${resourceReport_index?c}">
        <#if resourceReport.getTotal().getNewIssuesCount() gt 0>
          <span class="worst" id="new-total">${resourceReport.getTotal().getNewIssuesCount()?c}</span>
        <#else>
          <span id="new-total">0</span>
        </#if>
        <br/>New issues
      </th>
      <#if complete>
      <th align="right" width="1%" nowrap class="resource-details-${resourceReport_index?c}">
        <#if resourceReport.getTotal().getResolvedIssuesCount() gt 0>
          <span class="better" id="resolved-total">${resourceReport.getTotal().getResolvedIssuesCount()?c}</span>
        <#else>
          <span id="resolved-total">0</span>
        </#if>
        <br/>Resolved issues
      </th>
      <th align="right" width="1%" nowrap class="resource-details-${resourceReport_index?c} all">
        <span id="current-total">${resourceReport.getTotal().getCountInCurrentAnalysis()?c}</span><br/>Issues
      </th>
      </#if>
    </tr>
    </thead>
    <tbody class="resource-details-${resourceReport_index?c}">
    <#if complete>
    <#list resourceReport.getRuleReports() as ruleReport>
      <tr class="hoverable all">
        <td width="20">
          <i class="icon-severity-${ruleReport.getSeverity()?lower_case}"></i>
        </td>
        <td align="left">
          ${ruleNameProvider.nameForHTML(ruleReport.getRule())}
        </td>
        <td align="right">
          <#if ruleReport.getTotal().getNewIssuesCount() gt 0>
            <span class="worst">${ruleReport.getTotal().getNewIssuesCount()?c}</span>
          <#else>
            <span>0</span>
          </#if>
        </td>
        <#if complete>
        <td align="right">
          <#if ruleReport.getTotal().getResolvedIssuesCount() gt 0>
            <span class="better">${ruleReport.getTotal().getResolvedIssuesCount()?c}</span>
          <#else>
            <span>0</span>
          </#if>
        </td>
        <td align="right" class="all">
          ${ruleReport.getTotal().getCountInCurrentAnalysis()?c}
        </td>
        </#if>
      </tr>
    </#list>
    </#if>
    <#if complete>
      <#assign colspan = '5'>
    <#else>
      <#assign colspan = '3'>
    </#if>
    <#assign issues=resourceReport.getIssuesAtLine(0, complete)>
      <#if issues?has_content>
      <tr class="globalIssues">
        <td colspan="${colspan}">
          <#list issues as issue>
            <div class="issue" id="${issue.key()}">
              <div class="vtitle">
                <i class="icon-severity-${issue.severity()?lower_case}"></i>
                <#if issue.getMessage()?has_content>
                <span class="rulename">${issue.getMessage()?html}</span>
                <#else>
                <span class="rulename">${ruleNameProvider.nameForHTML(issue.getRuleKey())}</span>
                </#if>
                &nbsp;
                <img src="issuesreport_files/sep12.png">&nbsp;

                <span class="issue_date">
                  <#if issue.isNew()>
                    NEW
                  <#else>
                    ${issue.creationDate()?date}
                  </#if>
                </span>
              </div>
              <div class="discussionComment">
              ${ruleNameProvider.nameForHTML(issue.getRuleKey())}
              </div>
            </div>
            <#assign issueId = issueId + 1>
          </#list>
        </td>
      </tr>
      </#if>
      <tr>
        <td colspan="${colspan}">
          <table class="sources" border="0" cellpadding="0" cellspacing="0">
            <#list sourceProvider.getEscapedSource(resourceReport.getResourceNode()) as line>
              <#assign lineIndex=line_index+1>
              <#if resourceReport.isDisplayableLine(lineIndex, complete)>
                <tr id="${resourceReport_index?c}L${lineIndex?c}" class="row">
                  <td class="lid ">${lineIndex?c}</td>
                  <td class="line ">
                    <pre>${line}</pre>
                  </td>
                </tr>
                <tr id="${resourceReport_index}S${lineIndex?c}" class="blockSep">
                  <td colspan="2"></td>
                </tr>
                <#assign issues=resourceReport.getIssuesAtLine(lineIndex, complete)>
                <#if issues?has_content>
                  <tr id="${resourceReport_index?c}LV${lineIndex?c}" class="row">
                    <td class="lid"></td>
                    <td class="issues">
                      <#list issues as issue>
                        <div class="issue" id="${issue.key()}">
                          <div class="vtitle">
                            <i class="icon-severity-${issue.severity()?lower_case}"></i>
                            <#if issue.getMessage()?has_content>
                            <span class="rulename">${issue.getMessage()?html}</span>
                            <#else>
                            <span class="rulename">${ruleNameProvider.nameForHTML(issue.getRuleKey())}</span>
                            </#if>
                            &nbsp;
                            <img src="issuesreport_files/sep12.png">&nbsp;

                            <span class="issue_date">
                              <#if issue.isNew()>
                                NEW
                              <#else>
                                ${issue.creationDate()?date}
                              </#if>
                            </span>
                            &nbsp;

                          </div>
                          <div class="discussionComment">
                            ${ruleNameProvider.nameForHTML(issue.getRuleKey())}
                          </div>
                        </div>
                        <#assign issueId = issueId + 1>
                      </#list>
                    </td>
                  </tr>
                </#if>
              </#if>
            </#list>
          </table>
        </td>
      </tr>
    </tbody>
  </table>
    </#if>
  </#list>
  </div>
</div>
<script type="text/javascript">
  $(function() {
    refreshFilters(true);
  });
</script>
</#if>
</body>
</html>
