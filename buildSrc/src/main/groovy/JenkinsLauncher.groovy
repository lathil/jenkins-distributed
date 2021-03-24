import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.tasks.DefaultGroovySourceSet
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException
import org.apache.commons.io.FileUtils
import org.gradle.internal.impldep.org.apache.ivy.util.FileUtil

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class JenkinsLauncher extends DefaultTask {

    String jenkinsHome = project.DEFAULT_BUILD_DIR_NAME
    String jenkinHttpPort = 8080
    String initialUserName = 'admin'
    String initialUserPassword = 'password'
    String jenkinsUiUrl = 'http://localhost:8080'
    String logfilepath = jenkinsHome + '/logs'
    String jenkinsupdateSiteUrl = 'https://updates.jenkins.io/update-center.json'
    String jenkinsPluginsFile = 'plugins.txt'
    String cascConfigFile ='jenkins.yaml'

    @OutputDirectory
    File jenkinsHomeDir

    @TaskAction
    def start() {



        JavaPluginConvention conventionPlugin = project.convention.getPlugin(JavaPluginConvention.class)

        SourceSet initSourceSet = conventionPlugin.getSourceSets().findByName('init.groovy.d')
        if( initSourceSet != null) {
            println( "SourceSet: ${initSourceSet.allSource.name}" )
            initSourceSet.allGroovy.srcDirs.each { srcDir->
                srcDir.listFiles().each { srcFile ->
                    if(!srcFile.isDirectory()){
                        def dst = new File('init.groovy.d/' + srcFile.getName(),jenkinsHomeDir)
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
                            FileUtils.copyDirectory( resFile, new File( resFile.name,jenkinsHomeDir))
                        } else {
                            FileUtils.copyFile(resFile, new File( resFile.name,jenkinsHomeDir))
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

        System.setProperty('JENKINS_HOME', jenkinsHomeDir.absolutePath)
        System.setProperty('initialsetup.jenkins.admin.user', initialUserName)
        System.setProperty('initialsetup.jenkins.admin.password', initialUserPassword)
        System.setProperty('initialsetup.jenkins.ui.url', jenkinsUiUrl)
        //System.setProperty('CASC_JENKINS_CONFIG', jenkinsHome + '/conf/jenkins.yaml')
        //System.setProperty('hudson.Main.development', 'true')
        //System.setProperty('jenkins.install.runSetupWizard', 'false')
        System.setProperty('initialsetup.jenkins.jul.configfile', jenkinsHome + '/conf/logging.properties')
        System.setProperty('initialsetup.jenkins.jul.logfilepath', logfilepath )
        System.setProperty('initlalsetup.jenkins.updatesite.url', jenkinsupdateSiteUrl)
        System.setProperty('initialsetup.jenkins.plugins.file', jenkinsPluginsFile)
        System.setProperty('CASC_JENKINS_CONFIG', cascConfigFile)


        def outDir = new File('war', jenkinsHomeDir)
        outDir.mkdirs()
        extractWarFile(war, outDir)

        List<String> args = ["--webroot=${outDir.absolutePath}"]
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

    def extractWarFile (File warFile, File destDir){
        byte[] buffer = new byte[1024];
        def warZipInputStream = new ZipInputStream(new FileInputStream(warFile))
        ZipEntry zipEntry = warZipInputStream.getNextEntry()
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                Files.createDirectories(Path.of(newFile.path))
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if( !parent.exists()){
                    Files.createDirectories(Path.of(parent.path))
                    //parent.mkdirs()
                }
                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = warZipInputStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = warZipInputStream.getNextEntry()
        }
        warZipInputStream.closeEntry();
        warZipInputStream.close();

    }

    def  newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
