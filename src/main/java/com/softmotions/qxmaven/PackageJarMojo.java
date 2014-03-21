package com.softmotions.qxmaven;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;

/**
 * Package compiled qxoodoo application
 * assets into single JAR file.
 *
 * @goal package-jar
 * @phase package
 * @requiresDependencyResolution compile
 *
 * @author Adamansky Anton (adamansky@gmail.com)s
 */
public class PackageJarMojo extends AbstractQooxdooMojo {

    private static final String[] DEFAULT_EXCLUDES = new String[]{};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     *
     * @parameter
     */
    private String[] packageIncludes;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     *
     * @parameter
     */
    private String[] packageExcludes;

    /**
     * Directory containing the generated JAR.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private File jarOutputDirectory;

    /**
     * Name of the generated JAR.
     *
     * @parameter alias="jarName" property="qooxdoo.package.finalName" default-value="${project.build.finalName}"
     * @required
     */
    private String jarFinalName;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

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

    private String[] getPackageIncludes() {
        if (packageIncludes != null && packageIncludes.length > 0) {
            return packageIncludes;
        }
        return DEFAULT_INCLUDES;
    }

    private String[] getPackageExcludes() {
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

    protected File createArchive(File rootdir) throws MojoExecutionException {
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
        File rootdir = getRootDirectory();
        if (!rootdir.isDirectory()) {
            getLog().warn("Missing JAR root directory: " + rootdir.getPath());
            return;
        }
        File jarFile = createArchive(rootdir);
        getLog().info("Archive: " + jarFile.getPath() + " successfully created");
        String classifier = getClassifier();
        if (classifier != null) {
            projectHelper.attachArtifact(project, getType(), classifier, jarFile);
        } else {
            project.getArtifact().setFile(jarFile);
        }
    }
}
