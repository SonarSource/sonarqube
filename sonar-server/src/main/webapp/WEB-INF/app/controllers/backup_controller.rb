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
class BackupController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :admin_required
  verify :method => :post, :only => [:export, :import], :redirect_to => { :action => :index }

  def index

  end

  def export
    filename="sonar_backup_#{Date.today}.xml"
    xml=java_facade.getBackup().exportXml()
    send_data xml, :type => "application/xml", :filename => filename, :disposition => 'attachment'
  end

  def import
    file=params[:file]
    xml=read_file(file)
    if xml && !xml.empty?
      java_facade.getBackup().importXml(xml)
      Metric.clear_cache
      flash[:notice] = "Backup restore succeed"
    else
      flash[:error] = "File is empty or invalid"
    end
    redirect_to :action => 'index'
  end

  private

  def read_file(file)
    # file is a StringIO
    if file.respond_to?(:read)
      return file.read
    end
    # file is not a readable object
    nil
  end

end
