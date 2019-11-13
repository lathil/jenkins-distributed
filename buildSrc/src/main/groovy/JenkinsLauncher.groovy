import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.DefaultGroovySourceSet
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException
import org.apache.commons.io.FileUtils

import java.util.jar.JarFile

class JenkinsLauncher extends DefaultTask {

    String jenkinsHome = project.DEFAULT_BUILD_DIR_NAME
    String jenkinHttpPort = 8080
    String initialUserName = 'admin'
    String initialUserPassword = 'password'
    String jenkinsUiUrl = 'http://localhost:8080'
    String logfilepath = jenkinsHome + '/logs'
    String jenkinsupdateSiteUrl = 'https://updates.jenkins.io/update-center.json'

    @TaskAction
    def start() {

        JavaPluginConvention conventionPlugin = project.convention.getPlugin(JavaPluginConvention.class)
        SourceSet initSourceSet = conventionPlugin.getSourceSets().findByName('init.groovy.d')
        if( initSourceSet != null) {
            println( "SourceSet: ${initSourceSet.allSource.name}" )
            initSourceSet.allGroovy.srcDirs.each { srcDir->
                srcDir.listFiles().each { srcFile ->
                    if(!srcFile.isDirectory()){
                        def dst = new File(jenkinsHome + '/init.groovy.d/' + srcFile.getName())
                        dst.getParentFile().mkdirs()
                        println("Copy ${srcFile.getAbsolutePath()} to ${dst.path}")
                        if( dst.exists()) dst.delete()
                        dst << srcFile.text
                    }
                }

            }
        }

        SourceSet pluginSourceSet = conventionPlugin.getSourceSets().findByName("conf")
        if( pluginSourceSet != null){
            pluginSourceSet.resources.srcDirs.each { resDir ->
                resDir.listFiles().each { resFile ->
                        if( resFile.isDirectory()){
                            FileUtils.copyDirectory( resFile, new File(jenkinsHome + '/' + resFile.name))
                        } else {
                            FileUtils.copyFile(resFile, new File(jenkinsHome + '/' + resFile.name))
                        }

                        //def dst = new File(jenkinsHome + '/conf/' + resFile.getName())
                        //dst.getParentFile().mkdirs()
                        //println("Copy ${resFile.getAbsolutePath()} to ${dst.path}")
                        //if( dst.exists()) dst.delete()
                        //dst << resFile.text

                }
            }
        }



        def c = project.configurations.getByName("jenkinsWar")
        def files = c.resolve()
        if (files.isEmpty()) {
            throw new GradleException('No jenkins.war dependency is specified')
        }
        File war = files.toArray()[0]



        System.setProperty('JENKINS_HOME', jenkinsHome)
        System.setProperty('initialsetup.jenkins.admin.user', initialUserName)
        System.setProperty('initialsetup.jenkins.admin.password', initialUserPassword)
        System.setProperty('initialsetup.jenkins.ui.url', jenkinsUiUrl)
        //System.setProperty('CASC_JENKINS_CONFIG', jenkinsHome + '/conf/jenkins.yaml')
        //System.setProperty('hudson.Main.development', 'true')
        //System.setProperty('jenkins.install.runSetupWizard', 'false')
        System.setProperty('initialsetup.jenkins.jul.configfile', jenkinsHome + '/conf/logging.properties')
        System.setProperty('initialsetup.jenkins.jul.logfilepath', logfilepath )
        System.setProperty('initlalsetup.jenkins.updatesite.url', jenkinsupdateSiteUrl)


        List<String> args = []
        String port = jenkinHttpPort
        if (port) {
            args << "--httpPort=${port}"
        }

        def cl = new URLClassLoader([war.toURI().toURL()] as URL[])
        def mainClass = new JarFile(war).manifest.mainAttributes.getValue('Main-Class')
        cl.loadClass(mainClass).main(args as String[])

        // make the thread hang
        Thread.currentThread().join()
    }
}
