#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
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
class Api::ResourcesController < Api::ApiController

  # since version 3.3
  # Exemple : /api/resources/search?s=sonar
  #
  # -- Optional parameters
  # 'display_key' is used to return the resource key instead of the resource id. Default is false
  #
  def search
    search_text = params[:s]||''
    page=(params[:p] ? params[:p].to_i : 1)
    page_size=(params[:ps] ? params[:ps].to_i : 10)
    display_key=params[:display_key]||false
    display_uuid=params[:display_uuid]||false
    if params[:q]
      qualifiers=params[:q].split(',')
    elsif params[:qp]
      qualifiers=Java::OrgSonarServerUi::JRubyFacade.getInstance().getQualifiersWithProperty(params[:qp])
    else
      qualifiers=[]
    end

    bad_request("Page index must be greater than 0") if page<=0
    bad_request("Page size must be greater than 0") if page_size<=0

    key = escape_like(search_text).downcase
    conditions=['kee like ?']
    condition_values=[key + '%']

    unless qualifiers.empty?
      conditions<<'qualifier in (?)'
      condition_values<<qualifiers
    end
    indexes = ResourceIndex.all(:select => 'distinct(resource_id),root_project_id,qualifier,name_size', # optimization to not load unused columns like 'kee'
                                 :conditions => [conditions.join(' and ')].concat(condition_values),
                                 :order => 'name_size')

    indexes = select_authorized(:user, indexes)
    total = indexes.size

    select2_format=(params[:f]=='s2')

    if select2_format && qualifiers.size>1
      # select2.js does not manage lazy loading of grouped options -> (almost) all the results are returned
      resource_ids=indexes[0...100].map { |index| index.resource_id }
    else
      # we don't group results when only one qualifier is requested, so we can enable lazy loading (pagination)
      offset=(page-1)*page_size
      resource_ids=indexes[offset...offset+page_size].map { |index| index.resource_id }
    end

    resources=[]
    unless resource_ids.empty?
      resources=Project.all(:select => 'id,qualifier,name,long_name,kee,uuid', :conditions => ['id in (?) and enabled=?', resource_ids, true])
    end

    if select2_format
      # customize format for select2.js (see ApplicationHelper::resource_select_tag)
      if qualifiers.size>1
        resources_by_qualifier = resources.group_by(&:qualifier)
        json = {
          :more => false,
          :results => resources_by_qualifier.map { |qualifier, grouped_resources| {:text => message("qualifiers.#{qualifier}"),
                                                                                   :children => grouped_resources.map { |r| {:id => display_key ? r.key : r.id, :text => r.name(true)} }} }
        }
      else
        json = {
          :more => (page * page_size)<total,
          :results => resources.map { |resource| {:id => display_uuid ? resource.uuid : (display_key ? resource.key : resource.id), :text => resource.name(true)} }
        }
      end
    else
      json = {:total => total, :page => page, :page_size => page_size, :data => resources.map { |r| {:id => r.id, :key => r.key, :nm => r.name(true), :q => r.qualifier} }}
    end

    respond_to do |format|
      format.json { render :json => jsonp(json) }
      format.xml { render :xml => xml_not_supported }
      format.text { render :text => text_not_supported }
    end
  end

  def index
    begin
      resource_id=params[:resource]
      if resource_id
        @resource=Project.by_key(resource_id)
        @snapshot=(@resource && @resource.last_snapshot)
        raise ApiException.new(404, "Resource [#{resource_id}] not found") if @snapshot.nil?
        raise ApiException.new(401, "Unauthorized") unless has_role?(:user, @snapshot)
      else
        @snapshot=nil
        if params['scopes'].blank? && params['qualifiers'].blank?
          params['scopes']=Project::SCOPE_SET
          params['qualifiers']=Project::QUALIFIER_PROJECT
        end
      end

      # ---------- PARAMETERS
      snapshots_conditions=['snapshots.islast=:islast']
      snapshots_values={:islast => true}

      load_measures=false
      measures_conditions=[]
      measures_values={}
      measures_order = nil
      measures_limit = nil
      measures_by_sid={}
      measures=nil
      rules_by_id=nil

      if params['scopes']
        snapshots_conditions << 'snapshots.scope in (:scopes)'
        snapshots_values[:scopes]=params['scopes'].split(',')
      end

      if params['qualifiers']
        snapshots_conditions << 'snapshots.qualifier in (:qualifiers)'
        snapshots_values[:qualifiers]=params['qualifiers'].split(',')
      end

      if @snapshot
        depth=(params['depth'] ? params['depth'].to_i : 0)
        if depth==0
          snapshots_conditions << 'snapshots.id=:sid'
          snapshots_values[:sid]=@snapshot.id

        elsif depth>0
          snapshots_conditions << 'snapshots.root_snapshot_id=:root_sid'
          snapshots_values[:root_sid] = (@snapshot.root_snapshot_id || @snapshot.id)

          snapshots_conditions << 'snapshots.path LIKE :path'
          snapshots_values[:path]="#{@snapshot.path}#{@snapshot.id}.%"

          snapshots_conditions << 'snapshots.depth=:depth'
          snapshots_values[:depth] = @snapshot.depth + depth

        else
          # negative : all the resource tree
          snapshots_conditions << '(snapshots.id=:sid OR (snapshots.root_snapshot_id=:root_sid AND snapshots.path LIKE :path))'
          snapshots_values[:sid]=@snapshot.id
          snapshots_values[:root_sid] = (@snapshot.root_snapshot_id || @snapshot.id)
          snapshots_values[:path]="#{@snapshot.path}#{@snapshot.id}.%"
        end
      end

      if params['metrics'] && params['metrics']!='false'
        set_backward_compatible
        load_measures=true

        if params['metrics']!='true'
          metrics = Metric.by_keys(params[:metrics].split(','))
          # Derby does not accept "metric_id in (NULL)"
          # The workaround is to use the unknown id -1
          if metrics.empty?
            measures_conditions << 'project_measures.metric_id=-1'
          else
            measures_conditions << 'project_measures.metric_id IN (:metrics)'
            measures_values[:metrics]=metrics.select { |m| m.id }
          end
          if metrics.size==1
            measures_limit = (params[:limit] ? [params[:limit].to_i, 500].min : 500)
            measures_order = "project_measures.value #{'DESC' if metrics.first.direction<0}"
          end
        end

        measures_conditions << 'project_measures.person_id IS NULL'
        add_rule_filters(measures_conditions, measures_values)

        measures=ProjectMeasure.all(:joins => :snapshot,
                                     :select => select_columns_for_measures,
                                     :conditions => [(snapshots_conditions + measures_conditions).join(' AND '), snapshots_values.merge(measures_values)],
                                     :order => measures_order,
                                     # SONAR-6584 avoid OOM errors
                                     :limit => measures_limit ? measures_limit : 10000)

        measures.each do |measure|
          measures_by_sid[measure.snapshot_id]||=[]
          measures_by_sid[measure.snapshot_id]<<measure
        end

        if measures_limit
          snapshots_conditions << 'snapshots.id IN (:sids)'
          # Derby does not support empty lists, that's why a fake value is set
          snapshots_values[:sids] = (measures_by_sid.empty? ? [-1] : measures_by_sid.keys)
        end

        # load coding rules
        rules_by_id={}
        rule_ids=measures.map { |m| m.rule_id }.compact.uniq
        unless rule_ids.empty?
          Rule.find(rule_ids).each do |rule|
            rules_by_id[rule.id]=rule
          end
        end
      end

      # ---------- LOAD RESOURCES
      if params['scopes']
        # optimization : add constraint to projects table
        snapshots_conditions << 'projects.scope in (:scopes)'
      end

      if params['qualifiers']
        # optimization : add constraint to projects table
        snapshots_conditions << 'projects.qualifier in (:qualifiers)'
      end

      snapshots_including_resource=Snapshot.all(
        :conditions => [snapshots_conditions.join(' AND '), snapshots_values],
        :include => 'project',
        # SONAR-6584 avoid OOM errors
        :limit => 500)

      # ---------- APPLY SECURITY - remove unauthorized resources - only if no selected resource
      if @resource.nil?
        snapshots_including_resource=select_authorized(:user, snapshots_including_resource)
      end

      # ---------- PREPARE RESPONSE
      resource_by_sid={}
      snapshots_by_rid={}
      snapshots_including_resource.each do |snapshot|
        resource_by_sid[snapshot.id]=snapshot.project
        snapshots_by_rid[snapshot.project_id]=snapshot
      end


      # ---------- SORT RESOURCES
      if load_measures && measures_order && measures && !measures.empty?
        # resources sorted by measures
        sorted_resources=measures.map do |measure|
          resource_by_sid[measure.snapshot_id]
        end
      else
        # no specific sort
        sorted_resources=resource_by_sid.values
      end

      sorted_resources=sorted_resources.uniq.compact

      # ---------- FORMAT RESPONSE
      objects={:sorted_resources => sorted_resources, :snapshots_by_rid => snapshots_by_rid, :measures_by_sid => measures_by_sid, :params => params, :rules_by_id => rules_by_id}
      respond_to do |format|
        format.json { render :json => jsonp(to_json(objects)) }
        format.xml { render :xml => to_xml(objects) }
        format.text { render :text => text_not_supported }
      end
    rescue ApiException => e
      render_error(e.msg, e.code)
    end
  end

  private

  def set_backward_compatible
    # backward-compatibility with sonar 1.9
    if params['filter_rules']
      (params['filter_rules']=='true') ? params['rules']='false' : params['rules']='true'
    end
  end

  def select_columns_for_measures
    select_columns='project_measures.id,project_measures.value,project_measures.metric_id,project_measures.snapshot_id,project_measures.rule_id,project_measures.text_value,project_measures.measure_data'
    if params[:includetrends]=='true'
      select_columns+=',project_measures.variation_value_1,project_measures.variation_value_2,project_measures.variation_value_3,project_measures.variation_value_4,project_measures.variation_value_5'
    end
    if params[:includealerts]=='true'
      select_columns+=',project_measures.alert_status,project_measures.alert_text'
    end
    if params[:includedescriptions]=='true'
      select_columns+=',project_measures.url,project_measures.description'
    end
    select_columns
  end

  def add_rule_filters(measures_conditions, measures_values)
    param_rules = params['rules'] || 'false'
    if param_rules=='true'
      measures_conditions << "project_measures.rule_id IS NOT NULL"

    elsif param_rules=='false'
      measures_conditions << "project_measures.rule_id IS NULL"
    else
      rule_ids=param_rules.split(',').map do |key_or_id|
        Rule.to_i(key_or_id)
      end
      measures_conditions << 'project_measures.rule_id IN (:rule_ids)'
      measures_values[:rule_ids]=rule_ids.compact
    end

  end

  def to_json(objects)
    resources = objects[:sorted_resources]
    snapshots_by_rid = objects[:snapshots_by_rid]
    measures_by_sid = objects[:measures_by_sid]
    rules_by_id = objects[:rules_by_id]
    params = objects[:params]

    result=[]
    resources.each do |resource|
      snapshot=snapshots_by_rid[resource.id]
      result<<resource_to_json(resource, snapshot, measures_by_sid[snapshot.id], rules_by_id, params)
    end
    result
  end

  def to_xml(objects)
    resources = objects[:sorted_resources]
    snapshots_by_rid = objects[:snapshots_by_rid]
    measures_by_sid = objects[:measures_by_sid]
    rules_by_id = objects[:rules_by_id]
    params = objects[:params]

    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!

    xml.resources do
      resources.each do |resource|
        snapshot=snapshots_by_rid[resource.id]
        resource_to_xml(xml, resource, snapshot, measures_by_sid[snapshot.id], rules_by_id, params)
      end
    end
  end

  def resource_to_json(resource, snapshot, measures, rules_by_id, options={})
    verbose=(options[:verbose]=='true')
    include_alerts=(options[:includealerts]=='true')
    include_trends=(options[:includetrends]=='true')
    include_descriptions=(options[:includedescriptions]=='true')

    json = {
      'id' => resource.id,
      'uuid' => resource.uuid,
      'key' => resource.key,
      'uuid' => resource.uuid,
      'name' => resource.name,
      'scope' => resource.scope,
      'qualifier' => resource.qualifier,
      'date' => Api::Utils.format_datetime(snapshot.created_at),
      'creationDate' => Api::Utils.format_datetime(resource.created_at)}
    json['lname']=resource.long_name if resource.long_name
    json['lang']=resource.language if resource.language
    json['version']=snapshot.version if snapshot.version
    json['branch']=resource.branch if resource.branch
    json['description']=resource.description if resource.description
    json['copy']=resource.copy_resource_id if resource.copy_resource_id
    if include_trends
      json[:p1]=snapshot.period1_mode if snapshot.period1_mode
      json[:p1p]=snapshot.period1_param if snapshot.period1_param
      json[:p1d]=Api::Utils.format_datetime(snapshot.period1_date) if snapshot.period1_date

      json[:p2]=snapshot.period2_mode if snapshot.period2_mode
      json[:p2p]=snapshot.period2_param if snapshot.period2_param
      json[:p2d]=Api::Utils.format_datetime(snapshot.period2_date) if snapshot.period2_date

      json[:p3]=snapshot.period3_mode if snapshot.period3_mode
      json[:p3p]=snapshot.period3_param if snapshot.period3_param
      json[:p3d]=Api::Utils.format_datetime(snapshot.period3_date) if snapshot.period3_date

      json[:p4]=snapshot.period4_mode if snapshot.period4_mode
      json[:p4p]=snapshot.period4_param if snapshot.period4_param
      json[:p4d]=Api::Utils.format_datetime(snapshot.period4_date) if snapshot.period4_date

      json[:p5]=snapshot.period5_mode if snapshot.period5_mode
      json[:p5p]=snapshot.period5_param if snapshot.period5_param
      json[:p5d]=Api::Utils.format_datetime(snapshot.period5_date) if snapshot.period5_date
    end
    if measures
      json_measures=[]
      json['msr']=json_measures
      measures.select { |measure| !measure.metric.key.start_with?('new_') || include_trends }.each do |measure|
        json_measure={}
        json_measures<<json_measure
        json_measure[:key]=measure.metric.name
        json_measure[:name]=measure.metric.short_name if verbose
        json_measure[:val]=measure.value.to_f if measure.value
        json_measure[:frmt_val]=measure.formatted_value if measure.value
        json_measure[:data]=measure.data if measure.data
        json_measure[:description]=measure.description if include_descriptions && measure.description
        json_measure[:url]=measure.url if include_descriptions && measure.url
        if include_alerts
          json_measure[:alert]=measure.alert_status
          json_measure[:alert_text]=measure.alert_text
        end
        if include_trends
          json_measure[:var1]=measure.variation_value_1.to_f if measure.variation_value_1
          json_measure[:fvar1]=measure.format_numeric_value(measure.variation_value_1.to_f) if measure.variation_value_1
          json_measure[:var2]=measure.variation_value_2.to_f if measure.variation_value_2
          json_measure[:fvar2]=measure.format_numeric_value(measure.variation_value_2.to_f) if measure.variation_value_2
          json_measure[:var3]=measure.variation_value_3.to_f if measure.variation_value_3
          json_measure[:fvar3]=measure.format_numeric_value(measure.variation_value_3.to_f) if measure.variation_value_3
          json_measure[:var4]=measure.variation_value_4.to_f if measure.variation_value_4
          json_measure[:fvar4]=measure.format_numeric_value(measure.variation_value_4.to_f) if measure.variation_value_4
          json_measure[:var5]=measure.variation_value_5.to_f if measure.variation_value_5
          json_measure[:fvar5]=measure.format_numeric_value(measure.variation_value_5.to_f) if measure.variation_value_5
        end
        if measure.rule_id
          rule = rules_by_id[measure.rule_id]
          json_measure[:rule_key] = rule.key if rule
          json_measure[:rule_name] = rule.name if rule
        end
      end
    end
    json
  end

  def resource_to_xml(xml, resource, snapshot, measures, rules_by_id, options={})
    verbose=(options[:verbose]=='true')
    include_alerts=(options[:includealerts]=='true')
    include_trends=(options[:includetrends]=='true')
    include_descriptions=(options[:includedescriptions]=='true')

    xml.resource do
      xml.id(resource.id)
      xml.key(resource.key)
      xml.name(resource.name)
      xml.lname(resource.long_name) if resource.long_name
      xml.branch(resource.branch) if resource.branch
      xml.scope(resource.scope)
      xml.qualifier(resource.qualifier)
      xml.lang(resource.language) if resource.language
      xml.version(snapshot.version) if snapshot.version
      xml.date(Api::Utils.format_datetime(snapshot.created_at))
      xml.creationDate(Api::Utils.format_datetime(resource.created_at))
      xml.description(resource.description) if include_descriptions && resource.description
      xml.copy(resource.copy_resource_id) if resource.copy_resource_id

      if include_trends
        xml.period1(snapshot.period1_mode) if snapshot.period1_mode
        xml.period1_param(snapshot.period1_param) if snapshot.period1_param
        xml.period1_date(Api::Utils.format_datetime(snapshot.period1_date)) if snapshot.period1_date

        xml.period2(snapshot.period2_mode) if snapshot.period2_mode
        xml.period2_param(snapshot.period2_param) if snapshot.period2_param
        xml.period2_date(Api::Utils.format_datetime(snapshot.period2_date)) if snapshot.period2_date

        xml.period3(snapshot.period3_mode) if snapshot.period3_mode
        xml.period3_param(snapshot.period3_param) if snapshot.period3_param
        xml.period3_date(Api::Utils.format_datetime(snapshot.period3_date)) if snapshot.period3_date

        xml.period4(snapshot.period4_mode) if snapshot.period4_mode
        xml.period4_param(snapshot.period4_param) if snapshot.period4_param
        xml.period4_date(Api::Utils.format_datetime(snapshot.period4_date)) if snapshot.period4_date

        xml.period5(snapshot.period5_mode) if snapshot.period5_mode
        xml.period5_param(snapshot.period5_param) if snapshot.period5_param
        xml.period5_date(Api::Utils.format_datetime(snapshot.period5_date)) if snapshot.period5_date
      end

      if measures
        measures.select { |measure| !measure.metric.key.start_with?('new_') || include_trends }.each do |measure|
          xml.msr do
            xml.key(measure.metric.name)
            xml.name(measure.metric.short_name) if verbose
            xml.val(measure.value.to_f) if measure.value
            xml.frmt_val(measure.formatted_value) if measure.value
            xml.data(measure.data) if measure.data
            xml.description(measure.description) if include_descriptions && measure.description
            xml.url(measure.url) if include_descriptions && measure.url
            if include_alerts
              xml.alert(measure.alert_status) if measure.alert_status
              xml.alert_text(measure.alert_text) if measure.alert_text
            end
            if include_trends
              xml.var1(measure.variation_value_1.to_f) if measure.variation_value_1
              xml.fvar1(measure.format_numeric_value(measure.variation_value_1.to_f)) if measure.variation_value_1
              xml.var2(measure.variation_value_2.to_f) if measure.variation_value_2
              xml.fvar2(measure.format_numeric_value(measure.variation_value_2.to_f)) if measure.variation_value_2
              xml.var3(measure.variation_value_3.to_f) if measure.variation_value_3
              xml.fvar3(measure.format_numeric_value(measure.variation_value_3.to_f)) if measure.variation_value_3
              xml.var4(measure.variation_value_4.to_f) if measure.variation_value_4
              xml.fvar4(measure.format_numeric_value(measure.variation_value_4.to_f)) if measure.variation_value_4
              xml.var5(measure.variation_value_5.to_f) if measure.variation_value_5
              xml.fvar5(measure.format_numeric_value(measure.variation_value_5.to_f)) if measure.variation_value_5
            end
            if measure.rule_id
              rule = rules_by_id[measure.rule_id]
              xml.rule_key(rule.key) if rule
              xml.rule_name(rule.name) if rule
            end
          end
        end
      end
    end
  end
end
