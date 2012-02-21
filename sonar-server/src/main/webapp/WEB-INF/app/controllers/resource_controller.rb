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
    @resource = Project.by_key(params[:id])
    not_found("Resource not found") unless @resource
    @resource=@resource.permanent_resource
    access_denied unless has_role?(:user, @resource)

    params[:layout]='false'
    @snapshot=@resource.last_snapshot

    load_extensions()

    if @extension
      if @extension.getId()=='violations'
        render_violations()
      elsif (@extension.getId()=='coverage')
        render_coverage()
      elsif (@extension.getId()=='source')
        render_source()
      elsif (@extension.getId()=='duplications')
        render_duplications()
      else
        render_extension()
      end
    else
      render_nothing()
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

  def load_extensions
    @extensions=[]
    java_facade.getResourceTabs(@resource.scope, @resource.qualifier, @resource.language).each do |tab|
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
    @expandable = (@lines!=nil)
    if @lines
      metric = Metric.by_key(params[:coverage_filter]||params[:metric])
      @coverage_filter = (metric ? metric.key : 'coverage')
      it_prefix = (@coverage_filter.start_with?('it_') ? 'it_' : '')

      @hits_by_line = load_distribution("#{it_prefix}coverage_line_hits_data")
      @conditions_by_line = load_distribution("#{it_prefix}conditions_by_line")
      @covered_conditions_by_line = load_distribution("#{it_prefix}covered_conditions_by_line")

      @hits_by_line.each_pair do |line_id, hits|
        line = @lines[line_id-1]
        if line
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
        'new_it_lines_to_cover'==@coverage_filter || 'new_it_coverage'==@coverage_filter || 'new_it_line_coverage'==@coverage_filter)
        @coverage_filter = "#{it_prefix}lines_to_cover"
        filter_lines { |line| line.hits && line.after(to) }

      elsif ('uncovered_lines'==@coverage_filter || 'new_uncovered_lines'==@coverage_filter ||
        'it_uncovered_lines'==@coverage_filter || 'new_it_uncovered_lines'==@coverage_filter)
        @coverage_filter = "#{it_prefix}uncovered_lines"
        filter_lines { |line| line.hits && line.hits==0 && line.after(to) }

      elsif ('conditions_to_cover'==@coverage_filter || 'branch_coverage'==@coverage_filter ||
        'new_conditions_to_cover'==@coverage_filter || 'new_branch_coverage'==@coverage_filter ||
        'it_conditions_to_cover'==@coverage_filter || 'it_branch_coverage'==@coverage_filter ||
        'new_it_conditions_to_cover' == @coverage_filter || 'new_it_branch_coverage'==@coverage_filter)
        @coverage_filter="#{it_prefix}conditions_to_cover"
        filter_lines { |line| line.conditions && line.conditions>0 && line.after(to) }

      elsif ('uncovered_conditions' == @coverage_filter || 'new_uncovered_conditions' == @coverage_filter ||
        'it_uncovered_conditions'==@coverage_filter || 'new_it_uncovered_conditions' == @coverage_filter)
        @coverage_filter="#{it_prefix}uncovered_conditions"
        filter_lines { |line| line.conditions && line.covered_conditions && line.covered_conditions<line.conditions && line.after(to) }
      end
    end
    render :action => 'index', :layout => !request.xhr?
  end


  def render_duplications
    duplications_data = @snapshot.measure('duplications_data');

    # create duplication groups
    @duplication_groups = []
    if duplications_data && duplications_data.measure_data && duplications_data.measure_data.data
      dups = Document.new duplications_data.measure_data.data.to_s
      if (XPath.match(dups, "//g").size() > 0)
        parse_duplications(dups, @duplication_groups)
      else
        parse_duplications_old_format(dups, @duplication_groups)
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

    render :action => 'index', :layout => !request.xhr?
  end

  def parse_duplications(dups, duplication_groups)
    resource_by_key = {}
    resource_by_key[@resource.key] = @resource
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
        dup_group << {:resource => resource, :lines_count => block.attributes['l'], :from_line => block.attributes['s']}
      end
      duplication_groups << dup_group
    end
  end

  # Format before sonar 2.12
  def parse_duplications_old_format(dups, duplication_groups)
    resource_by_key = {}
    dups.elements.each("duplications/duplication") do |dup|
      group = []
      target_key = dup.attributes['target-resource']
      target_resource = resource_by_key[target_key]
      unless target_resource
        # we use the resource_by_id map for optimization
        target_resource = Project.by_key(target_key)
        resource_by_key[target_key] = target_resource
      end
      group << {:lines_count => dup.attributes['lines'], :from_line => dup.attributes['start'], :resource => @resource}
      group << {:lines_count => dup.attributes['lines'], :from_line => dup.attributes['target-start'], :resource => target_resource}
      duplication_groups << group
    end
  end


  def render_violations
    load_sources()
    @display_violations=true
    @global_violations=[]
    @expandable=(@lines!=nil)
    @filtered=!@expanded

    if params[:rule].blank?
      metric = Metric.by_id(params[:metric])
      if metric && (metric.name=="active_reviews" || metric.name=="unassigned_reviews" || metric.name=="unplanned_reviews" || metric.name=="false_positive_reviews"|| metric.name=="unreviewed_violations" || metric.name=="new_unreviewed_violations")
        params[:rule] = metric.name.gsub(/new_/, "")
      end
    end
    
    conditions='snapshot_id=?'
    values=[@snapshot.id]
    if params[:rule].blank? || params[:rule]  == "all"
      conditions+=' AND (switched_off IS NULL OR switched_off=?)'
      values<<false
    else
      if params[:rule] == "false_positive_reviews"
        conditions+=' AND switched_off=?'
        values<<true
      else
        conditions+=' AND (switched_off IS NULL OR switched_off=?)'
        values<<false
        if params[:rule] == "active_reviews"
          open_reviews = Review.find(:all, :conditions => ["resource_id=? AND (status=? OR status=?)", @snapshot.resource_id, Review::STATUS_OPEN, Review::STATUS_REOPENED])
          if open_reviews.empty?
            conditions+=' AND permanent_id=-1'
          else
            conditions+=' AND permanent_id IN (?)'
            values << open_reviews.map {|r| r.rule_failure_permanent_id}
          end
        elsif params[:rule] == "unassigned_reviews"
          unassigned_reviews = Review.find(:all, :conditions => ["resource_id=? AND (status=? OR status=?) AND assignee_id IS NULL", @snapshot.resource_id, Review::STATUS_OPEN, Review::STATUS_REOPENED])
          if unassigned_reviews.empty?
            conditions+=' AND permanent_id=-1'
          else
            conditions+=' AND permanent_id IN (?)'
            values << unassigned_reviews.map {|r| r.rule_failure_permanent_id}
          end
        elsif params[:rule] == "unplanned_reviews"
          planned_reviews = Review.find(:all, :include => ['action_plans'], :conditions => ["resource_id=? AND (status=? OR status=?)", @snapshot.resource_id, Review::STATUS_OPEN, Review::STATUS_REOPENED]).reject{|r| r.planned?}
          if planned_reviews.empty?
            conditions+=' AND permanent_id=-1'
          else
            conditions+=' AND permanent_id IN (?)'
            values << planned_reviews.map {|r| r.rule_failure_permanent_id}
          end
        elsif params[:rule] == "unreviewed_violations"
          not_closed_reviews = Review.find(:all, :conditions => ["resource_id=? AND status!=?", @snapshot.resource_id, Review::STATUS_CLOSED])
          unless not_closed_reviews.empty?
            conditions+=' AND permanent_id NOT IN (?)'
            values << not_closed_reviews.map {|r| r.rule_failure_permanent_id}
          end
        else
          severity=Sonar::RulePriority.id(params[:rule])
          if severity
            conditions += ' AND failure_level=?'
            values<<severity
          else
            rule=Rule.by_key_or_id(params[:rule])
            conditions += ' AND rule_id=?'
            values<<(rule ? rule.id : -1)
          end
        end
      end
    end

    if @period
      date=@snapshot.period_datetime(@period)
      if date
        conditions+=' AND created_at>?'
        values<<date.advance(:minutes => 1)
      end
    end
    
    RuleFailure.find(:all, :include => ['rule', 'review'], :conditions => [conditions] + values, :order => 'failure_level DESC').each do |violation|
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
    render :action => 'index', :layout => !request.xhr?
  end


  def render_source
    load_sources()
    filter_lines_by_date()
    render :action => 'index', :layout => !request.xhr?
  end


  def filter_lines_by_date
    if @period && @snapshot.period_datetime(@period)
      @filtered=true
      to=Java::JavaUtil::Date.new(@snapshot.period_datetime(@period).to_f * 1000)
      if to
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
    attr_accessor :source, :revision, :author, :datetime, :violations, :hits, :conditions, :covered_conditions, :hidden, :highlighted, :deprecated_conditions_label

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
    render :action => 'extension', :layout => !request.xhr?
  end

  def render_nothing()
    render :action => 'nothing', :layout => !request.xhr?
  end
end