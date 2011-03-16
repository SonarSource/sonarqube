#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
class ResourceController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  helper DashboardHelper
  
  def index
    @resource = Project.by_key(params[:id])

    if (@resource && has_role?(:user, @resource))
      params[:layout]='false'
      @snapshot=@resource.last_snapshot

      load_extensions()

      if @extension
        if (@extension.getId()=='violations')
          render_violations()
        elsif (@extension.getId()=='coverage')
          render_coverage()
        elsif (@extension.getId()=='source')
          render_source()
        else
          render_extension()
        end
      else
        render_nothing()
      end
    else
      access_denied
    end
  end

  private

  def load_extensions
    @extensions=[]
    java_facade.getResourceTabs(@resource.scope, @resource.qualifier, @resource.language).each do |tab|
      tab.getUserRoles().each do |role|
        if has_role?(role, @resource)
          @extensions<<tab
          break
        end
      end
    end

    if !params[:tab].blank?
      @extension=@extensions.find{|extension| extension.getId()==params[:tab]}

    elsif !params[:metric].blank?
      metric=Metric.by_key(params[:metric])
      @extension=@extensions.find{|extension| extension.getDefaultTabForMetrics().include?(metric.key)}

    end
    @extension=@extensions.find{|extension| extension.isDefaultTab()} if @extension==nil
  end

  def load_sources
    @period = params[:period].to_i unless params[:period].blank?
    @expanded=(params[:expand]=='true')

    if @snapshot.source
      source_lines=Java::OrgSonarServerUi::JRubyFacade.new.colorizeCode(@snapshot.source.data, @snapshot.project.language).split("\n")
      init_scm()

      @lines=[]
      source_lines.each_with_index do |source, index|
        line=Line.new(source)
        @lines<<line

        line.revision=@revisions_by_line[index+1]
        line.author=@authors_by_line[index+1]

        date_string=@dates_by_line[index+1]
        line.datetime=(date_string ? DateTime::strptime(date_string): nil)
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
    @display_coverage=true
    if @lines
      @hits_by_line=load_distribution('coverage_line_hits_data')
      @conditions_by_line=load_distribution('conditions_by_line')
      @covered_conditions_by_line=load_distribution('covered_conditions_by_line')

      @hits_by_line.each_pair do |line_id,hits|
        line=@lines[line_id-1]
        if line
          line.hits=hits.to_i
          line.conditions=@conditions_by_line[line_id].to_i
          line.covered_conditions=@covered_conditions_by_line[line_id].to_i
        end
      end

      if @snapshot.measure('conditions_by_line').nil?
        deprecated_branches_by_line=load_distribution('branch_coverage_hits_data')
        deprecated_branches_by_line.each_pair do |line_id,label|
          line=@lines[line_id-1]
          if line
            line.deprecated_conditions_label=label
          end
        end
      end

      filter_lines_by_date()
    end
    render :action => 'index', :layout => !request.xhr?
  end

  
  
  def render_violations
    load_sources()
    @display_violations=true
    @global_violations=[]
    @expandable=(@lines!=nil)
    @filtered=!@expanded

    conditions='snapshot_id=?'
    values=[@snapshot.id]
    unless params[:rule].blank?
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

    if @period
      date=@snapshot.period_datetime(@period)
      if date
        conditions+=' AND created_at>?'
        values<<date.advance(:minutes => 1)
      else
        conditions+=' AND id=-1'
      end
    end

    RuleFailure.find(:all, :include => 'rule', :conditions => [conditions] + values, :order => 'failure_level DESC').each do |violation|
      # sorted by severity => from blocker to info
      if violation.line && violation.line>0 && @lines
        @lines[violation.line-1].add_violation(violation)
      else
        @global_violations<<violation
      end
    end

    if !@expanded && @lines
      @lines.each_with_index do |line,index|
        if line.violations?
          for i in index-4..index+4
            @lines[i].hidden=false if i>=0 && i<@lines.size
          end
        elsif line.hidden==nil
          line.hidden=true
        end
      end
    end
    render :action => 'index', :layout => !request.xhr?
  end
  
  
  

  def render_source
    load_sources()
    filter_lines_by_date()
    render :action => 'index', :layout => !request.xhr?
  end

  
  def filter_lines_by_date
    if @period
      @filtered=true
      to=@snapshot.period_datetime(@period)
      if to
        @lines.each do |line|
          line.hidden=true if line.datetime==nil || line.datetime<to
        end
      end
    end
  end

  class Line
    attr_accessor :source, :revision, :author, :datetime, :violations, :hits, :conditions, :covered_conditions, :hidden, :deprecated_conditions_label

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

    def date
      @datetime ? @datetime.to_date : nil
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