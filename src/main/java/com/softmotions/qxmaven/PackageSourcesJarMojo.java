package com.softmotions.qxmaven;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@Mojo(name = "package-sources",
      defaultPhase = LifecyclePhase.PROCESS_RESOURCES)

public class PackageSourcesJarMojo extends PackageJarMojo {

    @Override
    protected String getClassifier() {
        return "sources";
    }

    @Override
    protected File createArchive() throws MojoExecutionException {
        File jarFile = getJarFile(jarOutputDirectory, jarFinalName, getClassifier());
        if (jarFile.isFile() && !isQooxdooSourcesChanged()) {
            getLog().info("Source JAR archive: " + jarFile.getPath() + " is up to date ");
            return jarFile;
        }
        getLog().info("Creating source JAR archive: " + jarFile.getPath());
        MavenArchiver ma = new MavenArchiver();
        ma.setArchiver(jarArchiver);
        ma.setOutputFile(jarFile);
        archive.setForced(false);

        archive.addManifestEntry("Qooxdoo-App-Artifact", this.project.getArtifactId());
        archive.addManifestEntry("Qooxdoo-App-Namespace", this.getNamespace());
        archive.addManifestEntry("Qooxdoo-App-Build-Job", this.buildJob);
        archive.addManifestEntry("Qooxdoo-App-Version", this.project.getVersion());
        archive.addManifestEntry("Qooxdoo-Jar-Timestamp", String.valueOf(System.currentTimeMillis()));
        if (getSdkVersion() != null) {
            archive.addManifestEntry("Qooxdoo-Sdk-Version", getSdkVersion());
        }

        File mf = new File(super.getApplicationTarget(), this.manifest);
        if (!mf.exists()) {
            throw new MojoExecutionException("Missing required manifest file: " + mf);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        ObjectNode mfNode;
        ObjectNode mfProvides;
        try {
            mfNode = (ObjectNode) mapper.readTree(mf);
            if (mfNode.get("provides") == null || !mfNode.get("provides").isObject()) {
                throw new MojoExecutionException("Invalid meta file: " + mf + " missing 'provides' section");
            }
            mfProvides = (ObjectNode) mfNode.get("provides");
        } catch (IOException e) {
            throw new MojoExecutionException("Error assembling JAR", e);
        }

        if (sourcesDirectory.exists()) {
            jarArchiver.addDirectory(sourcesDirectory, "classes/",
                                     getPackageIncludes(), getPackageExcludes());
            mfProvides.put("class", "classes");
        }

        if (resourcesDirectory.exists()) {
            jarArchiver.addDirectory(resourcesDirectory, "resources/",
                                     getPackageIncludes(), getPackageExcludes());
            mfProvides.put("resource", "resources");
        }

        if (translationDirectory.exists()) {
            jarArchiver.addDirectory(translationDirectory, "translation/",
                                     getPackageIncludes(), getPackageExcludes());
            mfProvides.put("translation", "translation");
        }
        mfProvides.put("type", "library");
        try {

            File temp = File.createTempFile("qx-maven", "tmp");
            temp.deleteOnExit();
            try (FileWriter fw = new FileWriter(temp)) {
                mapper.writeValue(fw, mfNode);
            }
            jarArchiver.addFile(temp, manifest);

            ma.createArchive(session, project, archive);

        } catch (IOException | ManifestException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error assembling JAR", e);
        }

        return jarFile;
    }
}
