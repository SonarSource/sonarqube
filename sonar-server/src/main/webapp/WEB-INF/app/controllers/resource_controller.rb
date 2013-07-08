#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
require "rexml/document"

class ResourceController < ApplicationController

  include REXML, SourceHelper

  SECTION=Navigation::SECTION_RESOURCE
  helper :dashboard
  helper SourceHelper, UsersHelper, IssuesHelper

  def index
    if request.xhr?
      @resource = Project.by_key(params[:id])
      not_found("Resource not found") unless @resource
      @resource=@resource.permanent_resource
      access_denied unless has_role?(:user, @resource)

      @snapshot=@resource.last_snapshot
      @popup_mode = params[:popup] == 'true'

      if @snapshot
        load_extensions()

        if @extension
          if @extension.getId()=='issues'
            render_issues()
            render_partial_index()
          elsif (@extension.getId()=='coverage')
            render_coverage()
            render_partial_index()
          elsif (@extension.getId()=='source')
            render_source()
            render_partial_index()
          elsif (@extension.getId()=='duplications')
            render_duplications()
            render_partial_index()
          else
            render_extension()
          end
        else
          render_nothing()
        end
      else
        render_resource_deleted()
      end
    else
      # popup mode, title will always be displayed
      params[:layout] = 'false'
      render :action => 'index'
    end
  end

  def show_duplication_snippet
    resource = Project.by_key(params[:id])
    not_found("Resource not found") unless resource
    access_denied unless has_role?(:user, resource)

    original_resource = Project.by_key(params[:original_resource_id])
    render :partial => 'duplications_source_snippet',
           :locals => {:resource => resource, :original_resource => original_resource, :from_line => params[:from_line].to_i, :to_line => params[:to_line].to_i, :lines_count => params[:lines_count].to_i,
                       :group_index => params[:group_index], :external => (resource.root_id != original_resource.root_id)}
  end

  private

  def render_partial_index
    render :partial => 'index'
  end

  def load_extensions
    @extensions=[]
    java_facade.getResourceTabs(@resource.scope, @resource.qualifier, @resource.language, @snapshot.metric_keys.to_java(:string)).each do |tab|
      if tab.getUserRoles().empty?
        @extensions<<tab
      else
        tab.getUserRoles().each do |role|
          if has_role?(role, @resource)
            @extensions<<tab
            break
          end
        end
      end
    end

    if params[:tab].present?
      # Hack to manage violations as issues.
      params[:tab] = 'issues' if params[:tab] == 'violations'
      @extension=@extensions.find { |extension| extension.getId()==params[:tab] }

    elsif !params[:metric].blank?
      metric = Metric.by_key(params[:metric])
      @extension=@extensions.find { |extension| extension.getDefaultTabForMetrics().include?(metric.key) }
    end
    @extension=@extensions.find { |extension| extension.isDefaultTab() } if @extension==nil
  end

  def load_sources
    @period = params[:period].to_i unless params[:period].blank?
    @expanded=(params[:expand]=='true')
    @display_manual_violation_form=(current_user && has_role?(:user, @snapshot))

    panel = get_html_source_panel(@snapshot, {:display_scm => true})
    @lines = panel.html_lines unless panel.nil?
    @scm_available=panel.display_scm unless panel.nil?
  end

  def render_coverage
    load_sources()
    @display_coverage = true
    @display_it_coverage = (!@snapshot.measure('it_coverage').nil?)
    @display_overall_coverage = (!@snapshot.measure('overall_coverage').nil?)
    @expandable = (@lines!=nil)
    if @lines
      metric = Metric.by_key(params[:coverage_filter]||params[:metric])
      @coverage_filter = params[:coverage_filter] if params[:coverage_filter] == 'lines_covered_per_test'
      @coverage_filter = (metric ? metric.key : 'coverage') unless @coverage_filter
      @test_case_filter = params[:test_case_filter] if  !params[:test_case_filter].blank?

      it_prefix = ''
      it_prefix = 'it_' if (@coverage_filter.start_with?('it_') || @coverage_filter.start_with?('new_it_'))
      it_prefix = 'overall_' if (@coverage_filter.start_with?('overall_') || @coverage_filter.start_with?('new_overall_'))

      @hits_by_line = load_distribution(@snapshot, "#{it_prefix}coverage_line_hits_data")
      @conditions_by_line = load_distribution(@snapshot, "#{it_prefix}conditions_by_line")
      @covered_conditions_by_line = load_distribution(@snapshot, "#{it_prefix}covered_conditions_by_line")

      @testable = java_facade.testable(@snapshot.id)
      @hits_by_line.each_pair do |line_id, hits|
        line = @lines[line_id-1]
        if line
          line.index = line_id
          line.covered_lines = @testable ? @testable.countTestCasesOfLine(line_id) : 0
          line.hits = hits.to_i
          line.conditions = @conditions_by_line[line_id].to_i
          line.covered_conditions = @covered_conditions_by_line[line_id].to_i
        end
      end

      if @snapshot.measure("#{it_prefix}conditions_by_line").nil?
        # TODO remove this code when branch_coverage_hits_data is fully removed from CoreMetrics
        deprecated_branches_by_line = load_distribution(@snapshot, "#{it_prefix}branch_coverage_hits_data")
        deprecated_branches_by_line.each_pair do |line_id, label|
          line = @lines[line_id-1]
          if line
            line.deprecated_conditions_label = label
          end
        end
      end

      to = (@period && @snapshot.period_datetime(@period) ? Java::JavaUtil::Date.new(@snapshot.period_datetime(@period).to_f * 1000) : nil)
      @filtered = true
      if ('lines_to_cover'==@coverage_filter || 'coverage'==@coverage_filter || 'line_coverage'==@coverage_filter ||
          'new_lines_to_cover'==@coverage_filter || 'new_coverage'==@coverage_filter || 'new_line_coverage'==@coverage_filter ||
          'it_lines_to_cover'==@coverage_filter || 'it_coverage'==@coverage_filter || 'it_line_coverage'==@coverage_filter ||
          'new_it_lines_to_cover'==@coverage_filter || 'new_it_coverage'==@coverage_filter || 'new_it_line_coverage'==@coverage_filter ||
          'overall_lines_to_cover'==@coverage_filter || 'overall_coverage'==@coverage_filter || 'overall_line_coverage'==@coverage_filter ||
          'new_overall_lines_to_cover'==@coverage_filter || 'new_overall_coverage'==@coverage_filter || 'new_overall_line_coverage'==@coverage_filter)
        @coverage_filter = "#{it_prefix}lines_to_cover"
        filter_lines { |line| line.hits && line.after(to) }

      elsif ('uncovered_lines'==@coverage_filter || 'new_uncovered_lines'==@coverage_filter ||
          'it_uncovered_lines'==@coverage_filter || 'new_it_uncovered_lines'==@coverage_filter ||
          'overall_uncovered_lines'==@coverage_filter || 'new_overall_uncovered_lines'==@coverage_filter)
        @coverage_filter = "#{it_prefix}uncovered_lines"
        filter_lines { |line| line.hits && line.hits==0 && line.after(to) }

      elsif ('conditions_to_cover'==@coverage_filter || 'branch_coverage'==@coverage_filter ||
          'new_conditions_to_cover'==@coverage_filter || 'new_branch_coverage'==@coverage_filter ||
          'it_conditions_to_cover'==@coverage_filter || 'it_branch_coverage'==@coverage_filter ||
          'new_it_conditions_to_cover' == @coverage_filter || 'new_it_branch_coverage'==@coverage_filter ||
          'overall_conditions_to_cover'==@coverage_filter || 'overall_branch_coverage'==@coverage_filter ||
          'new_overall_conditions_to_cover' == @coverage_filter || 'new_overall_branch_coverage'==@coverage_filter)
        @coverage_filter="#{it_prefix}conditions_to_cover"
        filter_lines { |line| line.conditions && line.conditions>0 && line.after(to) }

      elsif ('uncovered_conditions' == @coverage_filter || 'new_uncovered_conditions' == @coverage_filter ||
        'it_uncovered_conditions'==@coverage_filter || 'new_it_uncovered_conditions' == @coverage_filter ||
        'overall_uncovered_conditions'==@coverage_filter || 'new_overall_uncovered_conditions' == @coverage_filter)
        @coverage_filter="#{it_prefix}uncovered_conditions"
        filter_lines { |line| line.conditions && line.covered_conditions && line.covered_conditions<line.conditions && line.after(to) }

      elsif @coverage_filter == 'lines_covered_per_test'
        @test_case_by_test_plan = {}
        @testable.testCases.each do |test_case|
          test_plan = test_case.testPlan
          test_cases = @test_case_by_test_plan[test_plan]
          test_cases = [] unless test_cases
          test_cases << test_case
          @test_case_by_test_plan[test_plan] = test_cases
        end

        if @test_case_filter
          test_case = @testable.testCaseByName(@test_case_filter)
          lines = @testable.coverage_block(test_case).lines
          filter_lines { |line| lines.include?(line.index) && line.after(to) }
        else
          filter_lines { |line| line.covered_lines && line.covered_lines > 0 && line.after(to) }
        end
      end
    end
  end

  def render_duplications
    duplications_data = @snapshot.measure('duplications_data')

    # create duplication groups
    @duplication_groups = []
    if duplications_data && duplications_data.measure_data && duplications_data.measure_data.data
      dups = Document.new duplications_data.measure_data.data.to_s
      if XPath.match(dups, "//g").size > 0
        parse_duplications(dups, @duplication_groups)
      else
        # This is the format prior to Sonar 2.12 => we display nothing but a message
        @duplication_group_warning = message('duplications.old_format_should_reanalyze')
      end
    end

    # And sort them 
    @duplication_groups.each do |group|
      group.sort! do |dup1, dup2|
        r1 = dup1[:resource]
        r2 = dup2[:resource]
        if r1 == r2
          # if duplication on same file => order by starting line
          dup1[:from_line].to_i <=> dup2[:from_line].to_i
        elsif r1 == @resource
          # the current resource must be displayed first
          -1
        elsif r2 == @resource
          # the current resource must be displayed first
          1
        elsif r1.project == @resource.project && r2.project != @resource.project
          # if resource is in the same project, this it must be displayed first
          -1
        elsif r2.project == @resource.project && r1.project != @resource.project
          # if resource is in the same project, this it must be displayed first
          1
        else
          dup1[:from_line].to_i <=> dup2[:from_line].to_i
        end
      end
    end
    @duplication_groups.sort! { |group1, group2| group1[0][:from_line].to_i <=> group2[0][:from_line].to_i }
  end

  def parse_duplications(dups, duplication_groups)
    resource_by_key = {}
    resource_by_key[@resource.key] = @resource
    dups_found_on_deleted_resource = false
    dups.elements.each("duplications/g") do |group|
      dup_group = []
      group.each_element("b") do |block|
        resource_key = block.attributes['r']
        resource = resource_by_key[resource_key]
        unless resource
          # we use the resource_by_key map for optimization
          resource = Project.by_key(resource_key)
          resource_by_key[resource_key] = resource
        end
        if resource
          dup_group << {:resource => resource, :lines_count => block.attributes['l'], :from_line => block.attributes['s']}
        else
          dups_found_on_deleted_resource = true
        end
      end
      duplication_groups << dup_group if dup_group.size > 1
    end
    @duplication_group_warning = message('duplications.dups_found_on_deleted_resource') if dups_found_on_deleted_resource
  end

  def render_issues
    load_sources()
    @display_issues = true
    @global_issues = []
    @expandable = (@lines != nil)
    @filtered = !@expanded
    rule_param = params[:rule]
    @issues_query_params = {'components' => @resource.key, 'resolved' => 'false'}

    if rule_param.blank? && params[:metric]
      metric = Metric.by_id(params[:metric])
      if metric && (metric.name=='false_positive_issues')
        rule_param = metric.name.gsub(/new_/, '')

        # hack to select the correct option in the rule filter select-box
        params[:rule] = rule_param
      end
    end

    if !rule_param.blank? && rule_param != 'all'
      if rule_param=='false_positive_issues'
        @issues_query_params['resolutions'] = 'FALSE-POSITIVE'
        @issues_query_params['resolved'] = 'true'

      elsif Sonar::RulePriority.id(rule_param)
        @issues_query_params['severities'] = rule_param

      else
        rule = Rule.by_key_or_id(rule_param)
        @issues_query_params['rules'] = rule.key
      end
    end

    if @period && @period != 0
      date = @snapshot.period_datetime(@period)
      if date
        @issues_query_params['createdAfter'] = Api::Utils.format_datetime(date)
      end
    end

    @issue_results = Api.issues.find(@issues_query_params)
    @issue_results.issues.each do |issue|
      # sorted by severity => from blocker to info
      if @lines && issue.line && issue.line>0 && issue.line<=@lines.size
        @lines[issue.line-1].add_issue(issue)
      else
        @global_issues<<issue
      end
    end

    if !@expanded && @lines
      filter_lines { |line| line.issues? }
    end

  end

  def render_source
    load_sources()
    filter_lines_by_date()
  end


  def filter_lines_by_date
    if @period && @snapshot.period_datetime(@period)
      @filtered=true
      to=Java::JavaUtil::Date.new(@snapshot.period_datetime(@period).to_f * 1000)
      if to && @lines
        @lines.each do |line|
          line.flag_as_hidden() if !line.after(to)
        end
      end
    end
  end

  def filter_lines(&block)
    @lines.each_with_index do |line, index|
      if yield(line)
        for i in index-4...index
          @lines[i].flag_as_displayed_context if i>=0
        end
        line.flag_as_displayed
        for i in index+1..index+4
          @lines[i].flag_as_displayed_context if i<@lines.size
        end
      else
        line.flag_as_hidden()
      end
    end
  end

  def render_extension()
    render :partial => 'extension'
  end

  def render_nothing()
    render :partial => 'nothing'
  end

  def render_resource_deleted()
    render :partial => 'resource_deleted'
  end

end