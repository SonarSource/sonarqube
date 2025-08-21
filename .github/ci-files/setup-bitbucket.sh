sudo bash -c '
TAR_PATH=$(curl -L https://marketplace.atlassian.com/download/plugins/atlassian-plugin-sdk-tgz -OJ -sw '\''%{filename_effective}'\'') \
  && tar -xvzf ${TAR_PATH} -C /opt \
  && rm $TAR_PATH \
  && SDK_DIR=`ls -d /opt/atlassian-plugin-sdk*` \
  && mv $SDK_DIR /opt/atlassian-plugin-sdk \
  && chmod -R 755 /opt/atlassian-plugin-sdk
'

export PATH="$PATH:/opt/atlassian-plugin-sdk/bin:/opt/atlassian-plugin-sdk/apache-maven-3.9.8/bin"