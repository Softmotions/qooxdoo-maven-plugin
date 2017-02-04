package com.softmotions.qxmaven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The qooxdoo builder is responsible for copying resources in the right location.
 * <p/>
 * This goal copy the files located into the "root" folder of the resources
 * into the target application directory
 *
 * goal generate-resources
 * phase generate-resources
 */

@Mojo(name = "generate-resources",
      defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateResourcesMojo extends AbstractResourcesMojo {

    File getSourceSiteRoot() {
        return new File(this.resourcesDirectory, "siteroot");
    }

    public void execute() throws MojoExecutionException {
        File siteroot = getResourcesTarget();
        if (siteroot.isDirectory()) {
            if (!isQooxdooSourcesChanged()) {
                getLog().info("No Qooxdoo sources/job changed skip application resources re-creation");
            } else {
                try {
                    FileUtils.deleteDirectory(siteroot);
                } catch (IOException e) {
                    getLog().error(e);
                }
            }
        }
        super.execute();
    }

    /**
     * Check that required resources exist and return the list of them
     *
     * @return A list of html resources to be filtered/copied
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    protected List<Resource> getResources() throws MojoExecutionException {
        List<Resource> resources = new ArrayList<>();
        File siteroot = getSourceSiteRoot();
        if (siteroot.isDirectory()) {
            Resource config = new Resource();
            config.setFiltering(false);
            config.setDirectory(siteroot.getAbsolutePath());
            config.setExcludes(Collections.singletonList("WEB-INF/**/*.xml"));
            resources.add(config);

            config = new Resource();
            config.setFiltering(true);
            config.setDirectory(siteroot.getAbsolutePath());
            config.setIncludes(Collections.singletonList("WEB-INF/**/*.xml"));
            resources.add(config);
        }
        return resources;
    }

    @Override
    protected File getResourcesTarget() {
        return new File(this.getApplicationTarget(), "siteroot");
    }

}
