import hudson.model.UpdateCenter
import hudson.security.HudsonPrivateSecurityRealm
import hudson.util.VersionNumber
import hudson.model.UpdateSite
import hudson.model.UpdateCenter
import hudson.util.PersistedList
import jenkins.install.*;


import javax.inject.Provider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.List
import java.util.stream.Collectors
import java.util.stream.Stream

import java.util.logging.FileHandler
import java.util.logging.LogManager
import java.util.logging.Logger

import java.util.logging.SimpleFormatter

import jenkins.model.*


class Constants {
    static final String JENKINS_UI_URL = 'initialsetup.jenkins.ui.url'
    static final String JENKINS_ADMIN_USER = 'initialsetup.jenkins.admin.user'
    static final String JENKINS_ADMIN_PASSWORD = 'initialsetup.jenkins.admin.password'
    static final String JENKINS_JUL_CONFIGFILE = 'initialsetup.jenkins.jul.configfile'
    static final String ENV_JENKINS_JUL_CONFIGFILE = 'INITIALSETUP_JENKINS_JUL_CONFIGFILE'
    static final String JENKINS_JUL_LOGFILE_PATH = 'initialsetup.jenkins.jul.logfilepath'
    static final String ENV_ENKINS_JUL_LOGFILE_PATH = 'INITIALSETUP_JUL_LOGFILEPATH'
    static final String JENKINS_UPDATESITE_URL = 'initlalsetup.jenkins.updatesite.url'
    static final String ENV_JENKINS_UPDATESITE_URL = 'INITIALSETUP_JENKINS_UPDATESITE_URL'
    static final String JENKINS_PLUGINS_FILE = 'initialsetup.jenkins.plugins.file'
}

/**
 * Skip the test that ask for the instance url
 *
 */
class SkipConfigureInstanceStepStateFilter extends InstallStateFilter {

    private final Logger log = Logger.getLogger('initialsetup')

    private static String name(InstallState state) {
        return state == null ? null : state.name()
    }

    @Override
    InstallState getNextInstallState(InstallState current, Provider<InstallState> proceed) {
        final InstallState proposedNext = proceed.get()

        InstallState next


        if (Objects.equals(name(proposedNext), name(InstallState.CONFIGURE_INSTANCE))) {

            def jenkins = Jenkins.get()

            def jenkinsUrl = System.getProperty(Constants.JENKINS_UI_URL)
            jenkinsUrl = jenkinsUrl ? jenkinsUrl : 'http://localhost:8080'


            Map<String, String> errors = new HashMap<>()
            log.info("Validating custom ihm url: ${jenkinsUrl}")
            jenkins.setupWizard.checkRootUrl(errors, jenkinsUrl)
            log.info("Setting custom ihm url: ${jenkinsUrl}")
            jenkins.setupWizard.useRootUrl(errors, jenkinsUrl)

            next = InstallState.INITIAL_SETUP_COMPLETED
        } else {
            next = proposedNext
        }

        if (next != proposedNext) {
            log.warning("Current state: '${name(current)}', default next state: '${name(proposedNext)}', overridden to: '${name(next)}'")
        } else {
            log.info("Current state: '${name(current)}', default next state: '${name(proposedNext)}'")
        }

        return next
    }


}

/**
 * Skip the step that ask for a initial user.
 *
 *
 */
class SkipCreateAdminUserStepStateFilter extends InstallStateFilter {

    private final Logger log = Logger.getLogger('initialsetup')

    private static String name(InstallState state) {
        return state == null ? null : state.name()
    }

    @Override
    InstallState getNextInstallState(InstallState current, Provider<InstallState> proceed) {
        final InstallState proposedNext = proceed.get()

        InstallState next


        if (Objects.equals(name(proposedNext), name(InstallState.CREATE_ADMIN_USER))) {

            def jenkins = Jenkins.get()
            if (jenkins.getSetupWizard().isUsingSecurityDefaults()) {

                System.getenv()
                log.info("Removing default admin user.")
                def adminName = System.getProperty(Constants.JENKINS_ADMIN_USER)
                adminName = adminName ? adminName : 'admin'
                def adminPassword = System.getProperty(Constants.JENKINS_ADMIN_PASSWORD)
                adminPassword = adminPassword ? adminPassword : 'password'

                log.info("Initial user: ${adminName} , password: ${adminPassword}")

                def securityRealm = jenkins.getSecurityRealm() as HudsonPrivateSecurityRealm
                def defaultAdmin = securityRealm.getUser(SetupWizard.initialSetupAdminUserName)
                if( defaultAdmin ){
                    defaultAdmin.delete()
                    try {
                        jenkins.setupWizard.getInitialAdminPasswordFile().delete()
                    } catch (InterruptedException ex) {
                        throw new IOException(e)
                    }
                }

                log.info("Creating new admin user: ${adminName}, ${adminPassword}")
                securityRealm.createAccount(adminName, adminPassword)
                jenkins.save()
            }

            next = InstallState.CONFIGURE_INSTANCE
        } else {
            next = proposedNext
        }

        if (next != proposedNext) {
            log.warning("Current state: '${name(current)}', default next state: '${name(proposedNext)}', overridden to: '${name(next)}'")
        } else {
            log.info("Current state: '${name(current)}', default next state: '${name(proposedNext)}'")
        }

        return next
    }
}

/**
 * Skip the step that present a dialog that ask for the plugin to install
 *
 * instead, install plugins that are described in a file 'plugin.txt' under $JENKSIN_HOME$/conf if this file is present
 * The plugin.txt file can include the plugin version in this format 'plugin_name':'plugin_version'
 * Plugin are installed with Jenkins update center and plugin manager, that mean that the plugin manger wil try to install the indicated version or more recent in the update center.
 *
 */

class SkipPluginInstallStepStateFilter extends InstallStateFilter {

    private final Logger log = Logger.getLogger('initialsetup')

    private static String name(InstallState state) {
        return state == null ? null : state.name()
    }


    @Override
    InstallState getNextInstallState(InstallState current, Provider<InstallState> proceed) {
        final InstallState proposedNext = proceed.get()

        InstallState next

        if (Objects.equals(name(proposedNext), name(InstallState.NEW))) {

            def jenkinsHome = Jenkins.get().getRootDir().getPath()

            UpdateCenter.updateDefaultSite()
            Jenkins.get().updateCenter.siteList.each{ site ->
                log.info( "Update site url : ${site.url}")

            }
            def pluginConfFile = System.getProperty(Constants.JENKINS_PLUGINS_FILE)
            pluginConfFile = pluginConfFile ? pluginConfFile : 'plugins.txt'
            File pluginsFile = new File("${jenkinsHome}/conf/${pluginConfFile}")
            if( pluginsFile.exists()){

                def pluginManager = Jenkins.get().pluginManager
                pluginsFile.eachLine { pluginLine ->

                    def pluginName = pluginLine
                    def pluginVersion = null
                    if( pluginLine.contains(':')){
                        def pluginCoordonates = pluginLine.split(':')
                        pluginName = pluginCoordonates[0]
                        pluginVersion = pluginCoordonates[1]
                    }

                    if( !pluginManager.getPlugin(pluginName)){

                        log.info( "Installing plugin ${pluginName}")
                        def plugin = pluginVersion == null ? Jenkins.get().updateCenter.getPlugin(pluginName) : Jenkins.get().updateCenter.getPlugin(pluginName, new VersionNumber(pluginVersion))

                        if( plugin != null) {
                            plugin.deploy(true).get()
                            log.info( "Plugin ${plugin.name}:${plugin.version} installed")
                        } else {
                            log.info("Plugin ${pluginName} not found in updates centers")
                        }

                    }
                }
                log.info( "All plugins installed")
            } else {
                log.info( "No ${jenkinsHome}/conf/plugins.txt configuration found")
            }

            next = InstallState.CREATE_ADMIN_USER
        } else {
            next = proposedNext
        }

        if (next != proposedNext) {
            log.warning("Current state: '${name(current)}', default next state: '${name(proposedNext)}', overridden to: '${name(next)}'")
        } else {
            log.info("Current state: '${name(current)}', default next state: '${name(proposedNext)}'")
        }

        return next
    }
}



def logsDir = new File(Jenkins.get().rootDir, "logs")
if(!logsDir.exists()){logsDir.mkdirs()}

/**
 * Modify path of file handler in jul config fila
 */

def julConfiFilePath = System.getProperty(Constants.JENKINS_JUL_CONFIGFILE)
julConfiFilePath == null ? julConfiFilePath = System.getenv(Constants.ENV_JENKINS_JUL_CONFIGFILE) : null
if( julConfiFilePath != null) {
    def julConfigFile = new File(julConfiFilePath)
    if( julConfigFile.exists()){
        def logFilePath = System.getProperty(Constants.JENKINS_JUL_LOGFILE_PATH)
        logFilePath == null ? logfilepath = System.getenv(Constants.ENV_ENKINS_JUL_LOGFILE_PATH) : null
        def newConfig = julConfigFile.text.replace('${' + Constants.JENKINS_JUL_LOGFILE_PATH + '}',logFilePath)
        julConfigFile.text = newConfig
        def logConfigStream = new FileInputStream(julConfiFilePath)
        LogManager.logManager.readConfiguration(logConfigStream)
    }

}

FileHandler allLogsHandler = new FileHandler(logsDir.absolutePath+"/jenkins-logs-%g.log", 1024 * 1024 * 10, 10, true)
allLogsHandler.setFormatter(new SimpleFormatter())

final Logger initialSetupLogger = Logger.getLogger(getClass().getName())
initialSetupLogger.info("initialsetup.grovvy called.")

/**
 * Modify default update site url, either by system properties or by environment variable
 */
def updateSiteUrl = System.getProperty(Constants.JENKINS_UPDATESITE_URL)
updateSiteUrl == null ? updateSiteUrl = System.getenv(Constants.ENV_JENKINS_UPDATESITE_URL) : null
if( updateSiteUrl != null){
    PersistedList<UpdateSite> sites = Jenkins.get().getUpdateCenter().getSites()
    for (UpdateSite s : sites) {
        if (s.getId().equals(UpdateCenter.ID_DEFAULT))
            sites.remove(s)
    }
    initialSetupLogger.info("Override default update site with: ${updateSiteUrl}")
    sites.add(new UpdateSite(UpdateCenter.ID_DEFAULT, updateSiteUrl))
}

/**
 *  In case of new install, override normal state machine by using custom install state, for initial account and plugin loading
 */
VersionNumber lastRunVersion = new VersionNumber(InstallUtil.getLastExecVersion())
def newInstall = ( lastRunVersion.compareTo(InstallUtil.NEW_INSTALL_VERSION) == 0)
if( newInstall) {
    initialSetupLogger.info("New installation detected.")

    initialSetupLogger.info("Add plugin install setup filter.")
    Jenkins.get().getExtensionList(InstallStateFilter.class).add(new SkipPluginInstallStepStateFilter())
    initialSetupLogger.info("Add custom admin setup filter.")
    Jenkins.get().getExtensionList(InstallStateFilter.class).add(new SkipCreateAdminUserStepStateFilter())
    initialSetupLogger.info("Add custom configure instance filter")
    Jenkins.get().getExtensionList(InstallStateFilter.class).add(new SkipConfigureInstanceStepStateFilter())


} else {
    initialSetupLogger.info("Last run version detected: ${lastRunVersion}. No customized setup initiated")
}
