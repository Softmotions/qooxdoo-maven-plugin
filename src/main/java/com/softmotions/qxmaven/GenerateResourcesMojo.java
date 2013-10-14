package com.softmotions.qxmaven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The qooxdoo builder is responsible for copying resources in the right location.
 * <p/>
 * This goal copy the files located into the "root" folder of the resources
 * into the target application directory
 *
 * @goal generate-resources
 * @phase generate-resources
 */
public class GenerateResourcesMojo extends AbstractResourcesMojo {

    /**
     * Check that required resources exist and return the list of them
     *
     * @return A list of html resources to be filtered/copied
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     */
    protected List<Resource> getResources() throws MojoExecutionException {
        List<Resource> resources = new ArrayList<>();
        File resourcesDir = new File(this.resourcesDirectory, this.namespace);
        // ROOT
        File configDir = new File(resourcesDir, "root");
        if (configDir.isDirectory()) {
            Resource config = new Resource();
            config.setFiltering(true);
            config.setDirectory(configDir.getAbsolutePath());
            resources.add(config);
        }
        return resources;
    }

    @Override
    protected File getResourcesTarget() {
        return this.getApplicationTarget();
    }

}
