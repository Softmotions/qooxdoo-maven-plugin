package com.softmotions.qxmaven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An abstract class that inherits from AbstractQooxdooMojo,
 * to use when implementing Mojo that needs to handle resources (copy/filtering).
 */
public abstract class AbstractResourcesMojo extends AbstractQooxdooMojo {

    /**
     * Whether to escape backslashes and colons in windows-style paths.
     *
     * parameter property="qooxdoo.resources.escapeWindowsPaths"
     * default-value="true"
     */

    @Parameter(property = "qooxdoo.resources.escapeWindowsPaths",
               defaultValue = "true",
               required = false)
    protected boolean escapeWindowsPaths;

    /**
     * Expression preceded with the String won't be interpolated \${foo} will be
     * replaced with ${foo}.
     *
     * parameter property="qooxdoo.resources.escapeString"
     */
    @Parameter(property = "qooxdoo.resources.escapeString")
    protected String escapeString;

    /**
     * component role="org.apache.maven.shared.filtering.MavenResourcesFiltering"
     * role-hint="default"
     * required
     */
    @Parameter(required = true)
    @Component(role = org.apache.maven.shared.filtering.MavenResourcesFiltering.class,
               hint = "default")
    protected MavenResourcesFiltering mavenResourcesFiltering;

    public void execute() throws MojoExecutionException {
        try {
            if (StringUtils.isEmpty(this.encoding)) {
                getLog().warn(
                        "File encoding has not been set, using platform encoding "
                        + ReaderFactory.FILE_ENCODING
                        + ", i.e. build is platform dependent!");
            }

            final MavenResourcesExecution mavenResourcesExecution =
                    new MavenResourcesExecution(
                            this.getResources(), this.getResourcesTarget(), this.project,
                            this.encoding, null, Collections.EMPTY_LIST,
                            this.session);

            mavenResourcesExecution.setEscapeWindowsPaths(this.escapeWindowsPaths);
            mavenResourcesExecution.setInjectProjectBuildFilters(false);
            mavenResourcesExecution.setEscapeString(this.escapeString);
            mavenResourcesExecution.setOverwrite(true);
            mavenResourcesExecution.setIncludeEmptyDirs(false);
            mavenResourcesExecution.setSupportMultiLineFiltering(false);

            this.filterResources(mavenResourcesExecution);

        } catch (final MavenFilteringException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Copy and filter the resources
     * You may override these method in the parent class to fit with your needs.
     */
    protected void filterResources(MavenResourcesExecution mavenResourcesExecution) throws MavenFilteringException {
        this.setProperties();
        this.mavenResourcesFiltering.filterResources(mavenResourcesExecution);
    }

    /**
     * Check that required resources exist and return the list of them
     * You need to override these method in the parent class to fit with your needs
     *
     * @return A list resources to be filtered/copied
     * @throws MojoExecutionException
     */
    protected List<Resource> getResources() throws MojoExecutionException {
        return new ArrayList<Resource>();
    }

    /**
     * Directory where resources will be copied
     * You may need to override these method in the parent class to fit with your needs.
     *
     * @return The directory where to store resources
     */
    protected File getResourcesTarget() {
        return this.getApplicationTarget();
    }

    /**
     * Make sure the required project properties for filtering are defined.
     */
    protected void setProperties() {
        this.project.getProperties().put("qooxdoo.application.namespace", this.namespace);
        this.project.getProperties().put("qooxdoo.application.config", this.config);
        this.project.getProperties().put("qooxdoo.application.resourcesDirectory", this.resourcesDirectory.getAbsolutePath());
        this.project.getProperties().put("qooxdoo.application.sourcesDirectory", this.sourcesDirectory.getAbsolutePath());
        this.project.getProperties().put("qooxdoo.application.testDirectory", this.testDirectory.getAbsolutePath());
        this.project.getProperties().put("qooxdoo.application.outputDirectory", this.outputDirectory.getAbsolutePath());
        this.project.getProperties().put("qooxdoo.application.cacheDirectory", this.cacheDirectory.getAbsolutePath());
        this.project.getProperties().put("qooxdoo.application.translationDirectory", this.translationDirectory.getAbsolutePath());
        if (this.getSdkVersion() != null) {
            this.project.getProperties().put("qooxdoo.sdk.version", this.getSdkVersion());
        }
        this.project.getProperties().put("qooxdoo.build.sourceEncoding", this.encoding);
        this.project.getProperties().put("qooxdoo.sdk.parentDirectory", this.sdkParentDirectory.getAbsolutePath());
    }
}
