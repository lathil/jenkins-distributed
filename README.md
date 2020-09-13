# Jenkins-distributed

A project to build and validate differents settups of jenkins single or with multiple slaves / agents, with docker-compose or kubernetes. The idea here is :
- test the settings of Jenkins single locally ( with desired choice of plugins, configs)
- test the same setting locally with slaves or agents on docker-compose
- push everything on Kubernetes.

## Booting of Jenkins

Jenkins is configured at statup time by a groovyscript ( src/init/groovy/initialsetup.groovy) placed at init.groovy.d that by-passed all the installer phases:
- display dialog for creating administrator details
- display dialog for selecting plugins
- display dialog for jenkins url

Instead the initialsetup.groovy scripts allows to provide startup details via system properties:
- initialsetup.jenkins.admin.user : name of initial administrator 
- initialsetup.jenkins.admin.password : pass of initial administrator
- initialsetup.jenkins.plugins.file : name of plugins file containing list of plugin (name:version) to install at init. Default to plugins.txt
- initlalsetup.jenkins.updatesite.url : plugin site url to use for loading the plugins
- initialsetup.jenkins.ui.url : public url for jenkins to configirure at startup. default to http://localhost:8080
- initialsetup.jenkins.jul.configfile : path of Java Util Logging config file to use by Jenkins
- initialsetup.jenkins.jul.logfilepath : path to use for JUL log files

The initialsetup script listen for Jenkins startup phases, so it only does this when Jenkins is not initialize ( fresh startup)

## Startup with Gradle 

A custom task JenkinsLauncher is wwriten in the project ( buildSrc/src/main/groovy/JenkinsLauncher). It allows launching a jenkins instance via the initialsetup.groovy script described above.
Parameters:
- jenkinsHome : jenkins workspace, default to project.DEFAULT_BUILD_DIR_NAME
- jenkinHttpPort : jenkins's jetty port to use. default to 8080
- initialUserName : jenkins initial user name, default 'admin'
- initialUserPassword : jenkins initial user password, default to 'password'
- jenkinsUiUrl : jenkins url, default to  'http://localhost:8080'
- logfilepath : path for the logs, default to jenkinsHome + '/logs'
- jenkinsupdateSiteUrl : jenkins update site to use for loading the plugins, default to 'https://updates.jenkins.io/update-center.json'
- jenkinsPluginsFile : name of the plugin list file, default to 'plugins.txt'
- cascConfigFile  the name of the jenkins configuration as code plugin file, default to: 'jankins.yaml'

In the main build.gradle file, the custom task is used in a task called launchJenkins with a default configuration. The task is visible unser verification tasks list.

### Configuration
When the JenkinsLauncher starts, it copies the content of the src/conf/resources folder into Jenkins workspace. You can place everything you need in this folder to preconfigure Jenkins when it starts.
Actually it is organize this way:
- src/conf/resources/conf : plugins list files and loggin configurations
- src/conf/resources/secrets : various certificates , trustores, private / public keys, password used for using jenkins with slaves on docker compose or kubernetes
- src/conf/resources/usersettings : npm an maven settings injected in slaves with docker or kubernetes
- src/conf/resources : Configuration As Code files for use with Jenkins solo / docker / slaves / kubernetes

# Docker
Several docker file are availble. All are based on Centos
## Jenkins master Dockerfiles:
Thoses dockerfiles are based on official Jenkins centos docker images. they all include the booting script described above, a plugin.txt and casc file as well as some environement variables.
- Dockerfile-centos-master : designed for running as solo as it include 3 differents maven installation a 3 jdk
- Dockerfile-centos-master-kubernetes : designed for running on kubernetes. No tools
## Jenkins slave files Dockerfiles:
