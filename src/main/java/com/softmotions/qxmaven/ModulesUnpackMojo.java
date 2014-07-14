package com.softmotions.qxmaven;


import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Goal which unpack the qooxdoo sdk to the target directory,
 * by using the qooxdoo.org:qooxdoo-sdk dependency
 * <p/>
 * goal sdk-unpack
 * phase initialize
 * requiresDependencyResolution compile
 */

@Mojo(name = "modules-unpack",
      defaultPhase = LifecyclePhase.INITIALIZE,
      requiresDependencyResolution = ResolutionScope.COMPILE)
public class ModulesUnpackMojo extends AbstractQooxdooMojo {


    public void execute() throws MojoExecutionException {
        unpackSdk();
        unpackModules();
    }


    void unpackModules() throws MojoExecutionException {
        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts.isEmpty()) {
            return;
        }
        for (final Artifact af : artifacts) {
            if (af.isOptional() ||
                !"sources".equals(af.getClassifier()) ||
                !"jar".equals(af.getType())) {
                continue;
            }
            File moduleDir = new File(this.modulesCacheDirectory, af.getArtifactId());
            File afile = af.getFile();
            try (JarFile jfile = new JarFile(afile)) {
                Manifest mf = jfile.getManifest();
                if (mf == null) {
                    continue;
                }
                Attributes mainAttributes = mf.getMainAttributes();
                String appVersion = mf.getMainAttributes().getValue("Qooxdoo-App-Version");
                if (appVersion == null) {
                    continue;
                }
                if (moduleDir.exists() && moduleDir.lastModified() >= afile.lastModified()) {
                    File oldMfFile = new File(moduleDir, "META-INF/MANIFEST.MF");
                    if (oldMfFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(oldMfFile)) {
                            Manifest oldMf = new Manifest(fis);
                            String oldVersion = oldMf.getMainAttributes().getValue("Qooxdoo-App-Version");
                            if (af.getVersion().equals(oldVersion)) {
                                getLog().info("Unpacked artifact: " + af + " is up to date");
                                continue;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Error JAR access: " + af, e);
            }

            try {
                if (moduleDir.exists()) {
                    FileUtils.cleanDirectory(moduleDir);
                } else {
                    moduleDir.mkdirs();
                }
                getLog().info("Extracting qooxdoo source artifact: " + af + " into: " + moduleDir.getCanonicalPath());
                UnArchiver unArchiver = archiverManager.getUnArchiver(afile);
                unArchiver.setOverwrite(true);
                unArchiver.setSourceFile(afile);
                unArchiver.setDestDirectory(moduleDir);
                unArchiver.extract();
            } catch (NoSuchArchiverException | IOException e) {
                throw new MojoExecutionException("Failed to unpack qooxdoo module: " + af, e);
            }
        }
    }


    void unpackSdk() throws MojoExecutionException {
        boolean sdkReady = this.checkSdk(false);
        if (sdkReady) {
            getLog().info("The qooxdoo sdk \'" + QOOXDOO_SDK_DIRECTORY + "\' in \'" + this.modulesCacheDirectory.getAbsolutePath() + "\' is up to date");
            return;
        }
        Artifact qooxdooSdk = this.getQooxdooSdkArtifact();
        if (qooxdooSdk == null) {
            getLog().warn("Could not find org.qooxdoo:qooxdoo-sdk dependency ! Make sure to download and unpack the sdk into the 'sdkDirectory'.");
        } else {
            File sdkDir = new File(this.modulesCacheDirectory, QOOXDOO_SDK_DIRECTORY);
            if (sdkDir.exists()) {
                getLog().info("Cleaning qooxdoo-sdk directory '" + sdkDir.getAbsolutePath() + "'");
                try {
                    FileUtils.cleanDirectory(sdkDir);
                } catch (Exception e) {
                    getLog().warn("Could not clean qooxdoo-sdk directory:" + e.getMessage());
                }
            }
            getLog().info("Unpacking qooxdoo-sdk dependency [" + qooxdooSdk.toString() + "]");
            File jarFile = qooxdooSdk.getFile();
            try {
                UnArchiver unArchiver = archiverManager.getUnArchiver(jarFile);
                this.modulesCacheDirectory.mkdirs();
                unArchiver.setOverwrite(true);
                unArchiver.setSourceFile(jarFile);
                unArchiver.extract(QOOXDOO_SDK_DIRECTORY, this.modulesCacheDirectory);
            } catch (NoSuchArchiverException e) {
                throw new MojoExecutionException("Unknown archiver type", e);
            } catch (Exception ex) {
                throw new MojoExecutionException("Error unpacking file: " + jarFile + "to: " + this.modulesCacheDirectory, ex);
            }
        }
        if (!this.checkSdk(true)) {
            throw new MojoExecutionException("Fatal: could not unpack the qooxdoo sdk");
        }
    }


    boolean checkSdk(boolean verbose) {
        // Check that the directory exists
        File sdkDirectory = getSdkDirectory();
        if (!sdkDirectory.isDirectory()) {
            if (verbose) {
                getLog().warn("The qooxdoo sdk directory \'" + sdkDirectory.getAbsolutePath() + "\' does not exist or is not a directory");
            }
            return false;
        }
        // Check the version
        File versionFile = new File(sdkDirectory, "version.txt");
        if (!versionFile.isFile()) {
            if (verbose) {
                getLog().warn("Could not find sdk version file: \'" + versionFile.getAbsolutePath() + "\'");
            }
            return false;
        } else {
            try {
                String version = FileUtils.fileRead(versionFile, this.encoding);
                String prjVersion = getSdkVersion() == null ? "null" : getSdkVersion();
                version = (version == null ? "null" : version.trim());
                if (version.equals(prjVersion)) {
                    return true;
                }
                getLog().warn("The version of the sdk (" + version + ") does not match with the required version (" + prjVersion + ")");
            } catch (Exception e) {
                getLog().warn("Could not read sdk version file: " + e.getMessage());
            }
            return false;
        }
    }
}
