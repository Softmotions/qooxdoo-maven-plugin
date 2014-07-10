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

/**
 * Goal which unpack the qooxdoo sdk to the target directory,
 * by using the qooxdoo.org:qooxdoo-sdk dependency
 * <p/>
 * goal sdk-unpack
 * phase initialize
 * requiresDependencyResolution compile
 */

@Mojo(name = "sdk-unpack",
      defaultPhase = LifecyclePhase.INITIALIZE,
      requiresDependencyResolution = ResolutionScope.COMPILE)
public class SdkUnpackMojo extends AbstractQooxdooMojo {


    public void execute() throws MojoExecutionException {
        boolean sdkReady = this.checkSdk(false);
        if (sdkReady) {
            getLog().info("The qooxdoo sdk \'" + QOOXDOO_SDK_DIRECTORY + "\' in \'" + this.sdkParentDirectory.getAbsolutePath() + "\' is up to date");
            return;
        }
        Artifact qooxdooSdk = this.getQooxdooSdkArtifact();
        if (qooxdooSdk == null) {
            getLog().warn("Could not find org.qooxdoo:qooxdoo-sdk dependency ! Make sure to download and unpack the sdk into the 'sdkDirectory'.");
        } else {
            File sdkDir = new File(this.sdkParentDirectory, QOOXDOO_SDK_DIRECTORY);
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
                UnArchiver unArchiver;
                unArchiver = archiverManager.getUnArchiver(jarFile);
                this.sdkParentDirectory.mkdirs();
                unArchiver.setOverwrite(true);
                unArchiver.setSourceFile(jarFile);
                unArchiver.extract(QOOXDOO_SDK_DIRECTORY, this.sdkParentDirectory);
            } catch (NoSuchArchiverException e) {
                throw new MojoExecutionException("Unknown archiver type", e);
            } catch (Exception ex) {
                throw new MojoExecutionException("Error unpacking file: " + jarFile + "to: " + this.sdkParentDirectory, ex);
            }
        }
        if (!this.checkSdk(true)) {
            throw new MojoExecutionException("Fatal: could not unpack the qooxdoo sdk");
        }
    }

    public boolean checkSdk(boolean verbose) {
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
