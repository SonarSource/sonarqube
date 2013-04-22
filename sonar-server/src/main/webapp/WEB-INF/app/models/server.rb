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
class Server
  
  def info
    system_info + sonar_info + system_statistics + sonar_plugins + system_properties
  end
  
  def system_info
    system_info=[]
    add_property(system_info, 'System date') {java.util.Date.new()}
    add_property(system_info, 'JVM Vendor') {java.lang.management.ManagementFactory.getRuntimeMXBean().getVmVendor()}
    add_property(system_info, 'JVM Name') {java.lang.management.ManagementFactory.getRuntimeMXBean().getVmName()}
    add_property(system_info, 'JVM Version') {java.lang.management.ManagementFactory.getRuntimeMXBean().getVmVersion() }
    add_property(system_info, 'Java Version') {java_property('java.runtime.version') }
    add_property(system_info, 'Java Home') {java_property('java.home')}
    add_property(system_info, 'JIT Compiler') {java_property('java.compiler')}
    add_property(system_info, 'Application Server Container') {$servlet_context.getServerInfo()  }
    add_property(system_info, 'User Name') {java_property('user.name')}
    add_property(system_info, 'User TimeZone') {java_property('user.timezone')}
    add_property(system_info, 'OS') {"#{java_property('os.name')} / #{java_property('os.arch')} / #{java_property('os.version')}"}
    add_property(system_info, 'Processors') {java.lang.Runtime.getRuntime().availableProcessors()}
    add_property(system_info, 'System Classpath') {java.lang.management.ManagementFactory.getRuntimeMXBean().getClassPath()}
    add_property(system_info, 'Boot Classpath') {java.lang.management.ManagementFactory.getRuntimeMXBean().getBootClassPath() }
    add_property(system_info, 'Library Path') {java.lang.management.ManagementFactory.getRuntimeMXBean().getLibraryPath()  }
    system_info
  end
  
  def system_statistics
    system_statistics=[] 
    add_property(system_statistics, 'Total Memory') {"#{java.lang.Runtime.getRuntime().totalMemory() / 1000000} MB"}
    add_property(system_statistics, 'Free Memory') {"#{java.lang.Runtime.getRuntime().freeMemory() / 1000000} MB"}
    add_property(system_statistics, 'Max Memory') {"#{java.lang.Runtime.getRuntime().maxMemory() / 1000000} MB"}
    add_property(system_statistics, 'Heap') {"#{java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()}"}
    add_property(system_statistics, 'Non Heap') {"#{java.lang.management.ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()}"}
    add_property(system_statistics, 'System Load Average (last minute)') {system_load_average()}
    add_property(system_statistics, 'Loaded Classes (currently/total/unloaded)') {"#{java.lang.management.ManagementFactory.getClassLoadingMXBean().getLoadedClassCount()} / #{java.lang.management.ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount()} / #{java.lang.management.ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount()}"}
    add_property(system_statistics, 'Start Time') {"#{format_date(java.util.Date.new(java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime()))}"}
    add_property(system_statistics, 'Threads (total/peak/daemon)') {"#{java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount()} / #{java.lang.management.ManagementFactory.getThreadMXBean().getPeakThreadCount()} / #{java.lang.management.ManagementFactory.getThreadMXBean().getDaemonThreadCount() }" }
    system_statistics
  end

  def sonar_info
    sonar_info=[]
    add_property(sonar_info, 'Server ID') {sonar_property(ServerIdConfigurationController::PROPERTY_SERVER_ID)}
    add_property(sonar_info, 'Version') {org.sonar.server.platform.Platform.getServer().getVersion()}
    add_property(sonar_info, 'Started at') {org.sonar.server.platform.Platform.getServer().getStartedAt()}
    add_property(sonar_info, 'Database') {"#{jdbc_metadata. getDatabaseProductName()} #{jdbc_metadata. getDatabaseProductVersion()}"}
    add_property(sonar_info, 'Database URL') {sonar_property('sonar.jdbc.url')}
    add_property(sonar_info, 'Database Login') {sonar_property('sonar.jdbc.username')}
    add_property(sonar_info, 'Database Driver') {"#{jdbc_metadata.getDriverName()} #{jdbc_metadata.getDriverVersion()}"}
    add_property(sonar_info, 'Database Driver Class') {sonar_property('sonar.jdbc.driverClassName')}
    add_property(sonar_info, 'Database Dialect (Hibernate)') {"#{Java::OrgSonarServerUi::JRubyFacade.getInstance().getDatabase().getDialect().getId()} (#{Java::OrgSonarServerUi::JRubyFacade.getInstance().getDatabase().getDialect().getHibernateDialectClass().getName()})"}
    add_property(sonar_info, 'Hibernate Default Schema') {sonar_property('sonar.hibernate.default_schema')}
    add_property(sonar_info, 'External User Authentication') {sonar_property(org.sonar.api.CoreProperties.CORE_AUTHENTICATOR_CLASS)}
    add_property(sonar_info, 'Automatic User Creation') {sonar_property(org.sonar.api.CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS)}
    add_property(sonar_info, 'Allow Users to Sign Up') {sonar_property(org.sonar.api.CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY)}
    add_property(sonar_info, 'Force Authentication') {sonar_property(org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY)}
    sonar_info
  end
  
  def sonar_plugins
    sonar_plugins=[]
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getPluginsMetadata().select{|plugin| !plugin.isCore()}.sort.each do |plugin|
      add_property(sonar_plugins, plugin.getName()) {plugin.getVersion()}
    end
    sonar_plugins
  end
  
  def system_properties
    system_properties=[]
    keys=java.lang.System.getProperties().keySet().sort
    keys.each do |key|
      add_property(system_properties, key) {java.lang.System.getProperty(key)}
    end
    system_properties
  end
  
  
  
  private 
  
  def java_property(key)
    java.lang.System.getProperty(key)   
  end

  def add_property(properties, label)
    begin
      value=yield || '-'
      properties<<[label, value]
    rescue Exception => e
      Rails.logger.error("Can not get the property #{label}")
      Rails.logger.error(e)
      properties<<[label, 'N/A']
    end  
  end
  
  def format_double(d)
   (d * 10).to_i / 10.0
  end
  
  def format_date(date)
    java.text.SimpleDateFormat.new("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(date)
  end
  
  def sonar_property(key)
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getContainer().getComponentByType(Java::OrgApacheCommonsConfiguration::Configuration.java_class).getProperty(key)
  end
  
  def jdbc_metadata
    @metadata ||=
      begin
        ActiveRecord::Base.connection.instance_variable_get('@connection').connection.get_meta_data
      end
  end

  def system_load_average
    begin
      "#{format_double(100.0 * java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())}%"
    rescue
      # not available on Java 5. See http://jira.codehaus.org/browse/SONAR-2208
      'N/A'
    end
  end
end