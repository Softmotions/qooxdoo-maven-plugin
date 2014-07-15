package com.softmotions.qxmaven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

/**
 * Goal which builds the qooxdoo application
 * <p/>
 * A build contains all the necessaries javascript dependencies and the application sources,
 * generally compacted into a single file
 * <p/>
 * Qooxdoo supports two types of build:
 * - The development one, that works directly on the application sources, useful to develop your application
 * (no need to recompile the application on every changes)
 * - The production one, that create an optimized javascript file, or multiple ones by using packages
 * <p/>
 * goal compile
 * phase compile
 * requiresDependencyResolution compile
 */
@Mojo(name = "compile",
      defaultPhase = LifecyclePhase.COMPILE)
public class CompileMojo extends AbstractGeneratorMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isQooxdooSourcesChanged()) {
            getLog().info("No Qooxdoo sources/job changed skip application generation");
            return;
        }
        String ts = String.valueOf(System.currentTimeMillis());
        Properties genprops = new Properties();
        genprops.setProperty("ts", ts);
        genprops.setProperty("job", buildJob);

        this.setJobName(buildJob);
        super.execute();

        try (FileWriter fr = new FileWriter(new File(super.getApplicationTarget(), ".generation"))) {
            genprops.store(fr, null);
        } catch (Exception e) {
            getLog().warn(e);
        }
    }
}
