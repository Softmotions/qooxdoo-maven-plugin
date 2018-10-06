package com.softmotions.qxmaven;

import java.io.File;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Package compiled qxoodoo application
 * assets into single JAR file.
 * <p/>
 * goal package-jar
 * phase package
 * requiresDependencyResolution compile
 *
 * @author Adamansky Anton (adamansky@gmail.com)s
 */

@Mojo(name = "package-jar",
      defaultPhase = LifecyclePhase.PACKAGE)

public class PackageJarMojo extends AbstractQooxdooMojo {

    protected static final String[] DEFAULT_EXCLUDES = new String[]{};

    protected static final String[] DEFAULT_INCLUDES = new String[]{"**/*"};

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     * <p/>
     * parameter
     */
    @Parameter
    protected String[] packageIncludes;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     * <p/>
     * parameter
     */
    @Parameter
    protected String[] packageExcludes;

    /**
     * Directory containing the generated JAR.
     * <p/>
     * parameter default-value="${project.build.directory}"
     * required
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    protected File jarOutputDirectory;

    /**
     * Name of the generated JAR.
     *
     * @parameter alias="jarName" property="qooxdoo.package.finalName" default-value="${project.build.finalName}"
     * @required
     */
    @Parameter(property = "qooxdoo.package.finalName",
               alias = "jarName",
               defaultValue = "${project.build.finalName}",
               required = true)
    protected String jarFinalName;

    /**
     * The Jar archiver.
     * <p/>
     * component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    @Component(role = org.codehaus.plexus.archiver.Archiver.class, hint = "jar")
    protected JarArchiver jarArchiver;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     * <p/>
     * parameter
     */
    @Parameter
    protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * component
     */
    @Component
    protected MavenProjectHelper projectHelper;

    /**
     * Return the specific output directory to serve as the root for the archive.
     */
    protected File getRootDirectory() {
        return new File(this.getApplicationTarget(), "siteroot");
    }

    /**
     * Overload this to produce a jar with another classifier, for example a test-jar.
     */
    protected String getClassifier() {
        return this.buildJob;
    }

    /**
     * Overload this to produce a test-jar, for example.
     */
    protected String getType() {
        return "jar";
    }

    protected String[] getPackageIncludes() {
        if (packageIncludes != null && packageIncludes.length > 0) {
            return packageIncludes;
        }
        return DEFAULT_INCLUDES;
    }

    protected String[] getPackageExcludes() {
        if (packageExcludes != null && packageExcludes.length > 0) {
            return packageExcludes;
        }
        return DEFAULT_EXCLUDES;
    }

    protected static File getJarFile(File basedir, String finalName, String classifier) {
        if (classifier == null) {
            classifier = "";
        } else if (classifier.trim().length() > 0 && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }
        return new File(basedir, finalName + classifier + ".jar");
    }

    protected File createArchive() throws MojoExecutionException {
        File rootdir = getRootDirectory();
        if (!rootdir.isDirectory()) {
            getLog().warn("Missing JAR root directory: " + rootdir.getPath());
            return null;
        }
        File jarFile = getJarFile(jarOutputDirectory, jarFinalName, getClassifier());
        getLog().info("Creating qooxdoo JAR archive: " + jarFile.getPath() +
                      " from: " + rootdir.getPath());
        MavenArchiver ma = new MavenArchiver();
        ma.setArchiver(jarArchiver);
        ma.setOutputFile(jarFile);
        archive.setForced(false);

        File[] qxmeta = {
                new File(super.getApplicationTarget(), this.config),
                new File(super.getApplicationTarget(), this.manifest)
        };
        for (File mf : qxmeta) {
            if (mf.exists()) {
                ma.getArchiver()
                        .addFile(mf, "META-INF/qooxdoo/" + mf.getName());
            }
        }
        archive.addManifestEntry("Qooxdoo-App-Artifact", this.project.getArtifactId());
        archive.addManifestEntry("Qooxdoo-App-Namespace", this.getNamespace());
        archive.addManifestEntry("Qooxdoo-App-Build-Job", this.buildJob);
        archive.addManifestEntry("Qooxdoo-App-Version", this.project.getVersion());

        if (getSdkVersion() != null) {
            archive.addManifestEntry("Qooxdoo-Sdk-Version", getSdkVersion());
        }
        try {
            if (!rootdir.isDirectory()) {
                getLog().warn("Missing JAR root directory: " +
                              rootdir.getPath() + " empty JAR will be produced");
            } else {
                ma.getArchiver()
                        .addDirectory(rootdir, this.project.getArtifactId() + "/" + this.namespace + "/",
                                      getPackageIncludes(), getPackageExcludes());
            }
            ma.createArchive(session, project, archive);
            return jarFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Error assembling JAR", e);
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        File jarFile = createArchive();
        String classifier = getClassifier();
        if (jarFile == null) {
            return;
        }
        getLog().info("Archive: " + jarFile.getPath() + " successfully created, classifier: " + classifier);
        if (classifier != null) {
            projectHelper.attachArtifact(project, getType(), classifier, jarFile);
            if (project.getArtifact().getFile() == null) {
                project.getArtifact().setFile(jarFile);
            }
        } else {
            project.getArtifact().setFile(jarFile);
        }
    }
}
