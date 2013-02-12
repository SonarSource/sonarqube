#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
require "rexml/document"

class ResourceController < ApplicationController

  include REXML

  SECTION=Navigation::SECTION_RESOURCE
  helper :dashboard
  helper SourceHelper, UsersHelper

  verify :method => :post, :only => [:create_violation]

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
          if @extension.getId()=='violations'
            render_violations()
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

  # Ajax request to display a form to create a review anywhere in source code
  def show_create_violation_form
    @line = params[:line].to_i
    @rules = Rule.manual_rules
    @html_id="#{params[:resource]}_#{@line}"
    render :partial => 'resource/create_violation_form'
  end

  def create_violation
    resource = Project.by_key(params[:resource])
    access_denied unless resource && current_user

    rule_id_or_name = params[:rule]
    if rule_id_or_name.blank?
      access_denied if params[:new_rule].present? && !has_role?(:admin)
      rule_id_or_name = params[:new_rule]
    end
    bad_request(message('code_viewer.create_violation.missing_rule')) if rule_id_or_name.blank?
    bad_request(message('code_viewer.create_violation.missing_message')) if params[:message].blank?
    bad_request(message('code_viewer.create_violation.missing_severity')) if params[:severity].blank?

    assignee=nil
    if params[:assignee_login].present?
      assignee = User.find(:first, :conditions => ["login = ?", params[:assignee_login]])
      bad_request(message('code_viewer.create_violation.bad_assignee')) unless assignee
    end
    violation = nil
    Review.transaction do
      rule = Rule.find_or_create_manual_rule(rule_id_or_name, true)
      violation = rule.create_violation!(resource, params)
      violation.create_review!(
          :assignee => assignee,
          :user => current_user,
          :status => Review::STATUS_OPEN,
          :manual_violation => true)
    end

    render :partial => 'resource/violation', :locals => {:violation => violation}
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
      @extension=@extensions.find { |extension| extension.getId()==params[:tab] }

    elsif !params[:metric].blank?
      metric=Metric.by_key(params[:metric])
      @extension=@extensions.find { |extension| extension.getDefaultTabForMetrics().include?(metric.key) }
    end
    @extension=@extensions.find { |extension| extension.isDefaultTab() } if @extension==nil
  end

  def load_sources
    @period = params[:period].to_i unless params[:period].blank?
    @expanded=(params[:expand]=='true')
    @display_manual_violation_form=(current_user && has_role?(:user, @snapshot))
    if @snapshot.source
      source_lines=@snapshot.source.syntax_highlighted_lines()
      init_scm()

      @lines=[]
      source_lines.each_with_index do |source, index|
        line=Line.new(source)
        @lines<<line

        line.revision=@revisions_by_line[index+1]
        line.author=@authors_by_line[index+1]

        date_string=@dates_by_line[index+1]
        line.datetime=(date_string ? Java::OrgSonarApiUtils::DateUtils.parseDateTime(date_string) : nil)
      end
    end
  end

  def init_scm
    @scm_available=(@snapshot.measure('last_commit_datetimes_by_line')!=nil)
    @authors_by_line=load_distribution('authors_by_line')
    @revisions_by_line=load_distribution('revisions_by_line')
    @dates_by_line=load_distribution('last_commit_datetimes_by_line')
  end

  def load_distribution(metric_key)
    m=@snapshot.measure(metric_key)
    m ? m.data_as_line_distribution() : {}
  end

  def render_coverage
    load_sources()
    @display_coverage = true
    @display_it_coverage = (!@snapshot.measure('it_coverage').nil?)
    @display_system_coverage = (!@snapshot.measure('system_coverage').nil?)
    @display_overall_coverage = (!@snapshot.measure('overall_coverage').nil?)
    @expandable = (@lines!=nil)
    if @lines
      metric = Metric.by_key(params[:coverage_filter]||params[:metric])
      @coverage_filter = params[:coverage_filter] if params[:coverage_filter] == 'lines_covered_per_test'
      @coverage_filter = (metric ? metric.key : 'coverage') unless @coverage_filter
      @test_case_filter = params[:test_case_filter] if  !params[:test_case_filter].blank?

      it_prefix = ''
      it_prefix = 'it_' if (@coverage_filter.start_with?('it_') || @coverage_filter.start_with?('new_it_'))
      it_prefix = 'system_' if (@coverage_filter.start_with?('system_') || @coverage_filter.start_with?('new_system_'))
      it_prefix = 'overall_' if (@coverage_filter.start_with?('overall_') || @coverage_filter.start_with?('new_overall_'))

      @hits_by_line = load_distribution("#{it_prefix}coverage_line_hits_data")
      @conditions_by_line = load_distribution("#{it_prefix}conditions_by_line")
      @covered_conditions_by_line = load_distribution("#{it_prefix}covered_conditions_by_line")

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
        deprecated_branches_by_line = load_distribution("#{it_prefix}branch_coverage_hits_data")
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
          'system_lines_to_cover'==@coverage_filter || 'system_coverage'==@coverage_filter || 'system_line_coverage'==@coverage_filter ||
          'new_system_lines_to_cover'==@coverage_filter || 'new_system_coverage'==@coverage_filter || 'new_system_line_coverage'==@coverage_filter ||
          'overall_lines_to_cover'==@coverage_filter || 'overall_coverage'==@coverage_filter || 'overall_line_coverage'==@coverage_filter ||
          'new_overall_lines_to_cover'==@coverage_filter || 'new_overall_coverage'==@coverage_filter || 'new_overall_line_coverage'==@coverage_filter)
        @coverage_filter = "#{it_prefix}lines_to_cover"
        filter_lines { |line| line.hits && line.after(to) }

      elsif ('uncovered_lines'==@coverage_filter || 'new_uncovered_lines'==@coverage_filter ||
          'it_uncovered_lines'==@coverage_filter || 'new_it_uncovered_lines'==@coverage_filter ||
          'system_uncovered_lines'==@coverage_filter || 'new_system_uncovered_lines'==@coverage_filter ||
          'overall_uncovered_lines'==@coverage_filter || 'new_overall_uncovered_lines'==@coverage_filter)
        @coverage_filter = "#{it_prefix}uncovered_lines"
        filter_lines { |line| line.hits && line.hits==0 && line.after(to) }

      elsif ('conditions_to_cover'==@coverage_filter || 'branch_coverage'==@coverage_filter ||
          'new_conditions_to_cover'==@coverage_filter || 'new_branch_coverage'==@coverage_filter ||
          'it_conditions_to_cover'==@coverage_filter || 'it_branch_coverage'==@coverage_filter ||
          'new_it_conditions_to_cover' == @coverage_filter || 'new_it_branch_coverage'==@coverage_filter ||
          'system_conditions_to_cover'==@coverage_filter || 'system_branch_coverage'==@coverage_filter ||
          'new_system_conditions_to_cover' == @coverage_filter || 'new_system_branch_coverage'==@coverage_filter ||
          'overall_conditions_to_cover'==@coverage_filter || 'overall_branch_coverage'==@coverage_filter ||
          'new_overall_conditions_to_cover' == @coverage_filter || 'new_overall_branch_coverage'==@coverage_filter)
        @coverage_filter="#{it_prefix}conditions_to_cover"
        filter_lines { |line| line.conditions && line.conditions>0 && line.after(to) }

      elsif ('uncovered_conditions' == @coverage_filter || 'new_uncovered_conditions' == @coverage_filter ||
        'it_uncovered_conditions'==@coverage_filter || 'new_it_uncovered_conditions' == @coverage_filter ||
        'system_uncovered_conditions'==@coverage_filter || 'new_system_uncovered_conditions' == @coverage_filter ||
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


  def render_violations
    load_sources()
    @display_violations=true
    @global_violations=[]
    @expandable=(@lines!=nil)
    @filtered=!@expanded
    rule_param=params[:rule]

    options={:snapshot_id => @snapshot.id}

    if rule_param.blank? && params[:metric]
      metric = Metric.by_id(params[:metric])
      if metric && (metric.name=='active_reviews' || metric.name=='unassigned_reviews' || metric.name=='unplanned_reviews' || metric.name=='false_positive_reviews'|| metric.name=='unreviewed_violations' || metric.name=='new_unreviewed_violations')
        rule_param = metric.name.gsub(/new_/, '')

        # hack to select the correct option in the rule filter select-box
        params[:rule] = rule_param
      end
    end

    if !rule_param.blank? && rule_param!='all'
      if rule_param=='false_positive_reviews'
        options[:switched_off]=true

      elsif rule_param=='active_reviews'
        options[:review_statuses]=[Review::STATUS_OPEN, Review::STATUS_REOPENED, nil]

      elsif rule_param=='unassigned_reviews'
        options[:review_statuses]=[Review::STATUS_OPEN, Review::STATUS_REOPENED, nil]
        options[:review_assignee_id]=nil

      elsif rule_param=='unplanned_reviews'
        options[:review_statuses]=[Review::STATUS_OPEN, Review::STATUS_REOPENED, nil]
        options[:planned]=false

      elsif rule_param=='unreviewed_violations'
        options[:review_statuses]=[nil]

      elsif Sonar::RulePriority.id(rule_param)
        options[:severity]=rule_param

      else
        options[:rule_id]=rule_param
      end
    end


    if @period
      date=@snapshot.period_datetime(@period)
      if date
        options[:created_after]=date.advance(:minutes => 1)
      end
    end

    violations = RuleFailure.search(options)
    violations.each do |violation|
      # sorted by severity => from blocker to info
      if @lines && violation.line && violation.line>0 && violation.line<=@lines.size
        @lines[violation.line-1].add_violation(violation)
      else
        @global_violations<<violation
      end
    end

    if !@expanded && @lines
      filter_lines { |line| line.violations? }
    end

    @review_screens_by_vid=nil
    if current_user && has_role?(:user, @resource)
      @review_screens_by_vid = RuleFailure.available_java_screens_for_violations(violations, @resource, current_user)
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
          @lines[i].flag_as_highlight_context() if i>=0
        end
        line.flag_as_highlighted()
        for i in index+1..index+4
          @lines[i].flag_as_highlight_context() if i<@lines.size
        end
      else
        line.flag_as_hidden()
      end
    end
  end

  class Line
    attr_accessor :index, :source, :revision, :author, :datetime, :violations, :hits, :conditions, :covered_conditions, :hidden, :highlighted, :deprecated_conditions_label, :covered_lines

    def initialize(source)
      @source=source
    end

    def add_violation(violation)
      @violations||=[]
      @violations<<violation
      @visible=true
    end

    def violations?
      @violations && @violations.size>0
    end

    def violation_severity
      if @violations && @violations.size>0
        @violations[0].failure_level
      else
        nil
      end
    end

    def after(date)
      if date && @datetime
        @datetime.after(date)
      else
        true
      end
    end

    def flag_as_highlighted
      @highlighted=true
      @hidden=false
    end

    def flag_as_highlight_context
      # do not force if highlighted has already been set to true
      @highlighted=false if @highlighted.nil?
      @hidden=false
    end

    def flag_as_hidden
      # do not force if it has already been flagged as visible
      if @hidden.nil?
        @hidden=true
        @highlighted=false
      end
    end

    def hidden?
      @hidden==true
    end

    def highlighted?
      # highlighted if the @highlighted has not been set or has been set to true
      !hidden? && @highlighted!=false
    end

    def deprecated_conditions_label=(label)
      if label
        @deprecated_conditions_label=label
        if label=='0%'
          @conditions=2
          @covered_conditions=0
        elsif label=='100%'
          @conditions=2
          @covered_conditions=2
        else
          @conditions=2
          @covered_conditions=1
        end
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