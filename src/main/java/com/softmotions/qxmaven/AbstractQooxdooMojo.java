package com.softmotions.qxmaven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.eclipse.aether.impl.ArtifactResolver;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.Set;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public abstract class AbstractQooxdooMojo extends AbstractMojo {

    final static protected String QOOXDOO_SDK_DIRECTORY = "qooxdoo-sdk";

    /**
     * @component
     */
    protected MavenProject project;

    /**
     * @component
     */
    protected MojoExecution mojo;

    /**
     * @component
     */
    protected MavenSession session;

    /**
     * @component
     */
    protected PluginDescriptor plugin;

    /**
     * @component
     */
    protected Settings settings;

    /**
     * @component
     */
    protected ArchiverManager archiverManager;

    /**
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * Path to the qooxdoo application source directory, containing the application classes.
     *
     * @parameter property="qooxdoo.application.sourcesDirectory"
     * default-value="${project.basedir}/src/main/qooxdoo/classes"
     * @required
     */
    protected File sourcesDirectory;

    /**
     * Path to the qooxdoo application test directory, containing the application unit-test classes.
     *
     * @parameter property="qooxdoo.application.testDirectory"
     * default-value="${project.basedir}/src/test/qooxdoo"
     * @required
     */
    protected File testDirectory;

    /**
     * Path to the qooxdoo application resources directory.
     *
     * @parameter property="qooxdoo.application.resourcesDirectory"
     * default-value="${project.basedir}/src/main/qooxdoo/resources"
     * @required
     */
    protected File resourcesDirectory;

    /**
     * Path to the output cache directory where the cache informations will be stored.
     *
     * @parameter property="qooxdoo.application.cacheDirectory"
     * default-value="${project.build.directory}/qooxdoo/cache"
     * @required
     */
    protected File cacheDirectory;

    /**
     * Path to the directory containing translation files.
     *
     * @parameter property="qooxdoo.application.translationDirectory"
     * default-value="${project.basedir}/src/main/qooxdoo/translation"
     * @required
     */
    protected File translationDirectory;

    /**
     * Path to the configuration directory.
     *
     * @parameter property="qooxdoo.application.configurationDirectory"
     * default-value="${project.basedir}/src/main/qooxdoo/configuration"
     */
    protected File configuationDirectory;

    /**
     * The namespace of the qooxdoo application.
     *
     * @parameter property="qooxdoo.application.namespace"
     * default-value="${project.artifactId}"
     * @required
     */
    protected String namespace;

    /**
     * Path to the qooxdoo sdk parent directory
     * The parent directory must contains a sub-directory named qooxdoo-sdk, that contains the unpacked qooxdoo sdk.
     * The qooxdoo-sdk is automatically installed in the right place if you are using the qooxdoo-sdk maven dependency in your pom.
     *
     * @parameter property="qooxdoo.sdk.parentDirectory"
     * default-value="${project.build.directory}"
     * @required
     */
    protected File sdkParentDirectory;

    /**
     * Path to the output directory where application will be builded.
     *
     * @parameter property="qooxdoo.application.outputDirectory"
     * default-value="${project.build.directory}/qooxdoo"
     * @required
     */
    protected File outputDirectory;

    /**
     * The character encoding scheme to be applied when filtering resources.
     *
     * @parameter property="project.build.sourceEncoding"
     * default-value="UTF-8"
     * @required
     */
    protected String encoding;

    /**
     * The name of the qooxdoo application configuration file.
     *
     * @parameter property="qooxdoo.application.config"
     * default-value="config.json"
     * @required
     */
    protected String config;

    /**
     * The name of the qooxdoo application manifest file.
     *
     * @parameter property="qooxdoo.application.manifest"
     * default-value="Manifest.json"
     * @required
     */
    protected String manifest;

    /**
     * Name of the job used to build the application.
     *
     * @parameter property="qooxdoo.build.job"
     * default-value="build"
     */
    protected String buildJob;

    /**
     * Path to the sdk directory
     *
     * @readonly
     */
    private File sdkDirectory;

    /**
     * Path to the qooxdoo application target
     *
     * @readonly
     */
    private File applicationTarget;


    protected AbstractQooxdooMojo() {
    }

    /**
     * Get the path to the sdk directory, containing the qooxdoo sdk
     *
     * @return Path to the (unpacked) qooxdoo sdk
     */
    public File getSdkDirectory() {
        if (this.sdkDirectory == null) {
            this.sdkDirectory = new File(this.sdkParentDirectory, QOOXDOO_SDK_DIRECTORY);
        }
        return this.sdkDirectory;
    }

    /**
     * Get the path to the application target
     *
     * @return Path to the application target
     */
    public File getApplicationTarget() {
        if (this.applicationTarget == null) {
            this.applicationTarget = new File(this.outputDirectory, this.namespace);
        }
        return this.applicationTarget;
    }

    public File getConfigDirectory() {
        return new File(this.configuationDirectory, this.namespace);
    }

    public File getConfigJson() {
        return new File(getConfigDirectory(), this.config);
    }

    public File getManifestJson() {
        return new File(getConfigDirectory(), this.manifest);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSdkVersion() {
        Artifact qooxdooSdk = this.getQooxdooSdkArtifact();
        if (qooxdooSdk == null) {
            return null;
        }
        return qooxdooSdk.getVersion();
    }

    /**
     * Get the qooxdoo-sdk dependency
     */
    public Artifact getQooxdooSdkArtifact() {
        Set<Artifact> dependencies = project.getArtifacts();
        if (dependencies.size() == 0) {
            return null;
        }
        //ArtifactFilter runtime = new ScopeArtifactFilter(Artifact.SCOPE_PROVIDED);
        for (Artifact dependency : dependencies) {
            if (!dependency.isOptional()
                && "jar".equals(dependency.getType())
                && "org.qooxdoo".equals(dependency.getGroupId())
                && "qooxdoo-sdk".equals(dependency.getArtifactId())
                /*&& runtime.include(dependency)<*/) {
                return dependency;
            }
        }
        return null;
    }


    protected long getLastMtime(File file, long threshould) {
        if (file == null || !file.exists()) {
            return 0;
        }
        long lm = 0;
        if (!file.isDirectory()) {
            lm = file.lastModified();
            if (lm > threshould) {
                return lm;
            }
        }
        File[] files = file.listFiles();
        if (files == null) {
            return 0;
        }
        for (File f : files) {
            lm = getLastMtime(f, threshould);
            if (lm > 0) {
                return lm;
            }
        }
        return 0;
    }

    protected boolean isQooxdooSourcesChanged() {
        long ts = 0;
        String ljob = null;
        Properties genprops = new Properties();
        File gfile = new File(getApplicationTarget(), ".generation");
        if (gfile.exists()) {
            try (FileReader fr = new FileReader(gfile)) {
                genprops.load(fr);
                ts = Long.valueOf(genprops.getProperty("ts"));
                ljob = genprops.getProperty("job");
            } catch (Exception e) {
                getLog().warn(e);
            }
        } else {
            return true;
        }
        if (buildJob.equals(ljob) &&
            getLastMtime(this.sourcesDirectory, ts) == 0 &&
            getLastMtime(this.resourcesDirectory, ts) == 0 &&
            getLastMtime(this.testDirectory, ts) == 0 &&
            getLastMtime(this.configuationDirectory, ts) == 0) {
            return false;
        }
        return true;
    }
}
