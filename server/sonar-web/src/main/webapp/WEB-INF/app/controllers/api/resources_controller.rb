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
    indexes = ResourceIndex.all(:select => 'distinct(component_uuid),root_component_uuid,qualifier,name_size', # optimization to not load unused columns like 'kee'
                                 :conditions => [conditions.join(' and ')].concat(condition_values),
                                 :order => 'name_size')

    indexes = select_authorized(:user, indexes)
    total = indexes.size

    select2_format=(params[:f]=='s2')

    if select2_format && qualifiers.size>1
      # select2.js does not manage lazy loading of grouped options -> (almost) all the results are returned
      resource_uuids=indexes[0...100].map { |index| index.component_uuid }
    else
      # we don't group results when only one qualifier is requested, so we can enable lazy loading (pagination)
      offset=(page-1)*page_size
      resource_uuids=indexes[offset...offset+page_size].map { |index| index.component_uuid }
    end

    resources=[]
    unless resource_uuids.empty?
      resources=Project.all(:select => 'id,qualifier,name,long_name,kee,uuid', :conditions => ['uuid in (?) and enabled=?', resource_uuids, true])
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
        @analysis=(@resource && @resource.last_analysis)
        raise ApiException.new(404, "Resource [#{resource_id}] not found") if @analysis.nil?
        raise ApiException.new(401, "Unauthorized") unless has_role?(:user, @resource)
      else
        @analysis=nil
        if params['scopes'].blank? && params['qualifiers'].blank?
          params['scopes']='PRJ'
          params['qualifiers']='TRK'
        end
      end

      # ---------- PARAMETERS
      components_conditions=['snapshots.islast=:islast']
      components_values={:islast => true}

      load_measures=false
      measures_conditions=[]
      measures_values={}
      measures_order = nil
      measures_limit = nil
      measures_by_component_uuid={}
      measures=nil

      if params['scopes']
        components_conditions << 'projects.scope in (:scopes)'
        components_values[:scopes]=params['scopes'].split(',')
      end

      if params['qualifiers']
        components_conditions << 'projects.qualifier in (:qualifiers)'
        components_values[:qualifiers]=params['qualifiers'].split(',')
      end

      if @analysis
        depth=(params['depth'] ? params['depth'].to_i : 0)
        if depth==0
          components_conditions << 'projects.uuid=:component_uuid'
          components_values[:component_uuid]=@resource.uuid

        else
          # negative : all the tree
          components_conditions << 'projects.project_uuid = :project_uuid and projects.enabled=:enabled and (projects.uuid=:component_uuid OR projects.uuid_path LIKE :uuid_path)'
          components_values[:component_uuid]=@resource.uuid
          components_values[:project_uuid] = @resource.project_uuid
          components_values[:enabled] = true
          components_values[:uuid_path]="#{@resource.uuid_path}#{@resource.uuid}.%"
        end
      end

      if params['metrics'] && params['metrics']!='false'
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

        measures = ProjectMeasure.all(:joins => [:analysis, :project],
                                     :select => select_columns_for_measures,
                                     :conditions => [(components_conditions + measures_conditions).join(' AND '), components_values.merge(measures_values)],
                                     :order => measures_order,
                                     # SONAR-6584 avoid OOM errors
                                     :limit => measures_limit ? measures_limit : 10000)

        measures.each do |measure|
          measures_by_component_uuid[measure.component_uuid] ||= []
          measures_by_component_uuid[measure.component_uuid] << measure
        end

        if measures_limit
          components_conditions << 'projects.uuid IN (:component_uuids)'
          components_values[:component_uuids] = measures_by_component_uuid.keys
        end

      end

      # ---------- LOAD COMPONENTS
      # H2 does not support empty lists, so short-breaking if no measures
      if measures_limit && measures_by_component_uuid.empty?
        components = []
      else
        components = Project.all(
          :include => :last_analysis,
          :conditions => [components_conditions.join(' AND '), components_values],
          # SONAR-6584 avoid OOM errors
          :limit => 500)
      end

      # ---------- APPLY SECURITY - remove unauthorized resources - only if no selected resource
      if @resource.nil?
        components = select_authorized(:user, components)
      end

      # ---------- PREPARE RESPONSE
      components_by_uuid = {}
      components.each do |c|
        components_by_uuid[c.uuid]=c
      end


      # ---------- SORT RESOURCES
      if load_measures && measures_order && measures && !measures.empty?
        # components are sorted by measures
        sorted_components = measures.map do |measure|
          components_by_uuid[measure.component_uuid]
        end
      else
        # no specific sort
        sorted_components = components
      end

      sorted_components = sorted_components.uniq.compact

      # ---------- FORMAT RESPONSE
      objects={:sorted_components => sorted_components, :components_by_uuid => components_by_uuid, :measures_by_component_uuid => measures_by_component_uuid, :params => params}
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

  def select_columns_for_measures
    select_columns='project_measures.id,project_measures.value,project_measures.metric_id,project_measures.component_uuid,project_measures.text_value,project_measures.measure_data'
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

  def to_json(objects)
    components = objects[:sorted_components]
    components_by_uuid = objects[:components_by_uuid]
    measures_by_component_uuid = objects[:measures_by_component_uuid]
    params = objects[:params]

    result=[]
    components.each do |component|
      measures = measures_by_component_uuid[component.uuid]
      result << component_to_json(component, measures, params)
    end
    result
  end

  def to_xml(objects)
    components = objects[:sorted_components]
    components_by_uuid = objects[:components_by_uuid]
    measures_by_component_uuid = objects[:measures_by_component_uuid]
    params = objects[:params]

    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!

    xml.resources do
      components.each do |component|
        measures = measures_by_component_uuid[component.uuid]
        component_to_xml(xml, component, measures, params)
      end
    end
  end

  def component_to_json(component, measures, options={})
    verbose=(options[:verbose]=='true')
    include_alerts=(options[:includealerts]=='true')
    include_trends=(options[:includetrends]=='true')
    include_descriptions=(options[:includedescriptions]=='true')

    json = {
      'id' => component.id,
      'uuid' => component.uuid,
      'key' => component.key,
      'uuid' => component.uuid,
      'name' => component.name,
      'scope' => component.scope,
      'qualifier' => component.qualifier,
      'creationDate' => Api::Utils.format_datetime(component.created_at)}
    json['date'] =  Api::Utils.format_datetime(component.last_analysis.created_at) if component.last_analysis
    json['lname'] = component.long_name if component.long_name
    json['lang']=component.language if component.language
    json['version']= component.last_analysis.version if component.last_analysis && component.last_analysis.version
    json['branch']=component.branch if component.branch
    json['description']=component.description if component.description
    if include_trends && component.last_analysis
      json[:p1]=component.last_snapshot.period1_mode if component.last_analysis.period1_mode
      json[:p1p]=component.last_analysis.period1_param if component.last_analysis.period1_param
      json[:p1d]=Api::Utils.format_datetime(component.last_analysis.period1_date) if component.last_analysis.period1_date

      json[:p2]=component.last_analysis.period2_mode if component.last_analysis.period2_mode
      json[:p2p]=component.last_analysis.period2_param if component.last_analysis.period2_param
      json[:p2d]=Api::Utils.format_datetime(component.last_analysis.period2_date) if component.last_analysis.period2_date

      json[:p3]=component.last_analysis.period3_mode if component.last_analysis.period3_mode
      json[:p3p]=component.last_analysis.period3_param if component.last_analysis.period3_param
      json[:p3d]=Api::Utils.format_datetime(component.last_analysis.period3_date) if component.last_analysis.period3_date

      json[:p4]=component.last_analysis.period4_mode if component.last_analysis.period4_mode
      json[:p4p]=component.last_analysis.period4_param if component.last_analysis.period4_param
      json[:p4d]=Api::Utils.format_datetime(component.last_analysis.period4_date) if component.last_analysis.period4_date

      json[:p5]=component.last_analysis.period5_mode if component.last_analysis.period5_mode
      json[:p5p]=component.last_analysis.period5_param if component.last_analysis.period5_param
      json[:p5d]=Api::Utils.format_datetime(component.last_analysis.period5_date) if component.last_analysis.period5_date
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
      end
    end
    json
  end

  def component_to_xml(xml, component, measures, options={})
    verbose=(options[:verbose]=='true')
    include_alerts=(options[:includealerts]=='true')
    include_trends=(options[:includetrends]=='true')
    include_descriptions=(options[:includedescriptions]=='true')

    xml.resource do
      xml.id(component.id)
      xml.key(component.key)
      xml.name(component.name)
      xml.lname(component.long_name) if component.long_name
      xml.branch(component.branch) if component.branch
      xml.scope(component.scope)
      xml.qualifier(component.qualifier)
      xml.lang(component.language) if component.language
      xml.version(component.last_analysis.version) if component.last_analysis && component.last_analysis.version
      xml.date(Api::Utils.format_datetime(component.last_analysis.created_at)) if component.last_analysis
      xml.creationDate(Api::Utils.format_datetime(component.created_at))
      xml.description(component.description) if include_descriptions && component.description

      if include_trends && component.last_analysis
        xml.period1(component.last_analysis.period1_mode) if component.last_analysis.period1_mode
        xml.period1_param(component.last_analysis.period1_param) if component.last_analysis.period1_param
        xml.period1_date(Api::Utils.format_datetime(component.last_analysis.period1_date)) if component.last_analysis.period1_date

        xml.period2(component.last_analysis.period2_mode) if component.last_analysis.period2_mode
        xml.period2_param(component.last_analysis.period2_param) if component.last_analysis.period2_param
        xml.period2_date(Api::Utils.format_datetime(component.last_analysis.period2_date)) if component.last_analysis.period2_date

        xml.period3(component.last_analysis.period3_mode) if component.last_analysis.period3_mode
        xml.period3_param(component.last_analysis.period3_param) if component.last_analysis.period3_param
        xml.period3_date(Api::Utils.format_datetime(component.last_analysis.period3_date)) if component.last_analysis.period3_date

        xml.period4(component.last_analysis.period4_mode) if component.last_analysis.period4_mode
        xml.period4_param(component.last_analysis.period4_param) if component.last_analysis.period4_param
        xml.period4_date(Api::Utils.format_datetime(component.last_analysis.period4_date)) if component.last_analysis.period4_date

        xml.period5(component.last_analysis.period5_mode) if component.last_analysis.period5_mode
        xml.period5_param(component.last_analysis.period5_param) if component.last_analysis.period5_param
        xml.period5_date(Api::Utils.format_datetime(component.last_analysis.period5_date)) if component.last_analysis.period5_date
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
          end
        end
      end
    end
  end
end
