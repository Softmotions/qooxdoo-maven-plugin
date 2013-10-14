package com.softmotions.qxmaven;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An abstract class that inherits from AbstractQooxdooMojo,
 * to use when implementing Mojo that needs to run a Qooxdoo python script
 *
 * @author charless
 */
public abstract class AbstractPythonMojo extends AbstractQooxdooMojo {

    private static String SCRIPT_NAME = "script.py";

    /**
     * Name of the python interpreter or full path to it
     *
     * @parameter property="qooxdoo.build.python"
     * default-value="python"
     */
    protected String pythonInterpreter;

    /**
     * Run the script
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Launch job
        getLog().info("Starting '" + getSCRIPT_NAME() + "' using external Python interpreter...");
        python();
    }

    /**
     * Launch a script with the external python interpreter
     *
     * @throws MojoExecutionException
     */
    protected void python() throws MojoExecutionException {
        CommandLine cmdLine = getPythonCommandLine();
        getLog().debug("Command line: '" + cmdLine.toString() + "'");
        long starts = System.currentTimeMillis();
        try {
            DefaultExecutor executor = new DefaultExecutor();
            executor.setExitValue(0);
            executor.execute(cmdLine);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        long passedTimeInSeconds = TimeUnit.SECONDS.convert(System.currentTimeMillis() - starts, TimeUnit.MILLISECONDS);
        getLog().info("DONE in " + passedTimeInSeconds + " seconds");

    }

    /**
     * Resolve the full path to the python script
     *
     * @return The resolved file
     */
    protected File resolvePythonScriptPath() throws MojoExecutionException {
        File pythonScript = new File(getSdkDirectory(), "tool" + File.separator + "bin" + File.separator + getSCRIPT_NAME());
        // Check script existence
        if (!pythonScript.exists() || !pythonScript.canRead()) {
            getLog().warn(
                    "The python script \'"
                    + pythonScript.getAbsolutePath()
                    + "\' does not exist or is not readable !"
            );
            throw new MojoExecutionException("Could not find python script");
        }
        return pythonScript;
    }

    /**
     * To overide
     *
     * @return
     */
    protected String[] getCommandLineOptions() {
        return new String[0];
    }

    /**
     * Return the command line to use with python
     *
     * @return The command line
     */
    protected CommandLine getPythonCommandLine() throws MojoExecutionException {
        Map<String, Object> map = new HashMap<>();
        CommandLine cmdLine = new CommandLine(pythonInterpreter);
        cmdLine.addArgument(resolvePythonScriptPath().getAbsolutePath());
        for (String o : this.getCommandLineOptions()) {
            cmdLine.addArgument(o);
        }
        return cmdLine;
    }

    public static String getSCRIPT_NAME() {
        return SCRIPT_NAME;
    }

    public static void setSCRIPT_NAME(String sCRIPT_NAME) {
        SCRIPT_NAME = sCRIPT_NAME;
    }
}
