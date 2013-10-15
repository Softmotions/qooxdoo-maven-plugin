package com.softmotions.qxmaven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FileReader;
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
 *
 * @goal compile
 * @phase compile
 * @requiresDependencyResolution compile
 */
public class CompileMojo extends AbstractGeneratorMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        File gfile = new File(super.getApplicationTarget(), ".generation");
        long ts = 0;
        String ljob = null;
        Properties genprops = new Properties();

        if (gfile.exists()) {
            try (FileReader fr = new FileReader(gfile)) {
                genprops.load(fr);
                ts = Long.valueOf(genprops.getProperty("ts"));
                ljob = genprops.getProperty("job");
            } catch (Exception e) {
                getLog().warn(e);
            }
        }

        if (buildJob.equals(ljob) &&
            getLastMtime(this.sourcesDirectory, ts) == 0 &&
            getLastMtime(this.resourcesDirectory, ts) == 0 &&
            getLastMtime(this.testDirectory, ts) == 0 &&
            getLastMtime(this.configuationDirectory, ts) == 0) {
            getLog().info("No Qooxdoo sources/job changed skip application generation");
            return;
        }

        genprops.setProperty("ts", String.valueOf(System.currentTimeMillis()));
        genprops.setProperty("job", buildJob);

        this.setJobName(buildJob);
        super.execute();

        try (FileWriter fr = new FileWriter(gfile)) {
            genprops.store(fr, null);
        } catch (Exception e) {
            getLog().warn(e);
        }
    }

    private long getLastMtime(File file, long threshould) {
        if (file == null || !file.exists()) {
            return 0;
        }
        long lm = 0;
        if (!file.isDirectory()) {
            lm = file.lastModified();
            if (lm > threshould) {
                return lm;
            }
        }
        File[] files = file.listFiles();
        if (files == null) {
            return 0;
        }
        for (File f : files) {
            lm = getLastMtime(f, threshould);
            if (lm > 0) {
                return lm;
            }
        }
        return 0;
    }
}
