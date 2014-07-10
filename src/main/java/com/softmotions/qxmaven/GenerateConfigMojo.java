package com.softmotions.qxmaven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generate the qooxdoo configuration files (config.json and Manifest.json) into the output directory.
 * <p/>
 * To generate the files, it filters and copy resources located into:
 * ${resourcesDirectory}/config
 * <p/>
 * goal generate-config
 * phase generate-sources
 */

@Mojo(name = "generate-config",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateConfigMojo extends AbstractResourcesMojo {
    /**
     * Some properties, referring to paths, need to be relativized before being wrote into config files.
     * WARNING: the value of the following properties must contains path to directory (not to a file)
     */
    private static String[] propsDirectoryToRelativize = {
            "qooxdoo.sdk.parentDirectory",
            "qooxdoo.application.resourcesDirectory",
            "qooxdoo.application.sourcesDirectory",
            "qooxdoo.application.testDirectory",
            "qooxdoo.application.outputDirectory",
            "qooxdoo.application.cacheDirectory",
            "qooxdoo.application.translationDirectory",
            "qooxdoo.submodules.dir"
    };

    /**
     * Copy and filter the resources
     * You may override these method in the parent class to fit with your needs.
     */
    protected void filterResources(MavenResourcesExecution mavenResourcesExecution) throws MavenFilteringException {
        this.setProperties(true);
        this.mavenResourcesFiltering.filterResources(mavenResourcesExecution);
        this.setProperties(false);
    }

    /**
     * Make sure the required project properties for filtering are defined
     *
     * @param relativize Some path properties needs to be relativized
     */
    protected void setProperties(Boolean relativize) {
        super.setProperties();
        if (relativize) {
            File target = this.getApplicationTarget();
            getLog().debug("The following path properties will be relativized to the application target '" + target.getAbsolutePath() + "':");
            for (String prop : propsDirectoryToRelativize) {
                try {
                    String spath = this.project.getProperties().getProperty(prop);
                    if (spath == null) {
                        continue;
                    }
                    File path = new File(spath);
                    String relPath = ResourceUtils.getRelativePath(path.getAbsolutePath(), target.getAbsolutePath(), "/", false);
                    getLog().debug("  - " + prop + ": " + path.getAbsolutePath() + " => " + relPath);
                    this.project.getProperties().put(prop, relPath);
                } catch (Exception e) {
                    getLog().warn("  - " + prop + ": " + "Can not relativize path '" + this.project.getProperties().get(prop) + "' :" + e.getMessage());
                }
            }
        }
    }

    /**
     * Check that required resources exist and return the list of them
     *
     * @return A list of qooxdoo resources to be filtered/copied
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    protected List<Resource> getResources() throws MojoExecutionException {
        List<Resource> qxResources = new ArrayList<>();
        File configDir = new File(configuationDirectory, this.namespace);
        // Config
        if (!configDir.isDirectory()) {
            throw new MojoExecutionException("Qooxdoo configuration directory \'" + configDir.getAbsolutePath() + "\' does not exists or is not a directory !");
        }
        File manifestJson = new File(configDir, "Manifest.json");
        if (!manifestJson.isFile()) {
            throw new MojoExecutionException("Qooxdoo manifest file \'" + manifestJson.getAbsolutePath() + "\' does not exists or is not a file !");
        }
        File configJson = new File(configDir, this.config);
        if (!configJson.isFile()) {
            throw new MojoExecutionException("Qooxdoo configuration file \'" + configJson.getAbsolutePath() + "\' does not exists or is not a file !");
        }
        Resource config = new Resource();
        config.setFiltering(true);
        config.setDirectory(configDir.getAbsolutePath());
        qxResources.add(config);
        return qxResources;
    }


    protected File getResourcesTarget() {
        return super.getResourcesTarget();
    }
}
