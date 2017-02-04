package com.softmotions.qxmaven;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.kevinsawicki.http.HttpRequest;

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
     * <p/>
     * parameter property="qooxdoo.build.python"
     * default-value="python"
     */
    @Parameter(property = "qooxdoo.build.python",
               defaultValue = "python")
    private String pythonInterpreter;

    private String realInterpreter;

    /**
     * Run the script
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Launch job
        getLog().info("Starting '" + getScriptName() + "' using external Python interpreter...");
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
        File pythonScript = new File(getSdkDirectory(), "tool" + File.separator + "bin" + File.separator + getScriptName());
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
        CommandLine cmdLine = new CommandLine(loadPythonInterpreter());
        cmdLine.addArgument(resolvePythonScriptPath().getAbsolutePath());
        for (String o : this.getCommandLineOptions()) {
            cmdLine.addArgument(o);
        }
        return cmdLine;
    }

    public String loadPythonInterpreter() {
        if (!"internal".equals(pythonInterpreter)) {
            getLog().info("Using python interpreter: " + pythonInterpreter);
            return pythonInterpreter;
        }
        if (realInterpreter != null) {
            return realInterpreter;
        }
        String url = null;
        Path execPath = null;
        boolean is64bit;
        if (System.getProperty("os.name").contains("Windows")) {
            is64bit = (System.getenv("ProgramFiles(x86)") != null);
        } else {
            is64bit = (System.getProperty("os.arch").contains("64"));
        }
        if (SystemUtils.IS_OS_LINUX) {
            if (is64bit) {
                url = "https://bitbucket.org/pypy/pypy/downloads/pypy2-v5.6.0-linux64.tar.bz2";
                execPath = Paths.get(binDir.toString(), "pypy2-v5.6.0-linux64", "bin/pypy");
            } else {
                url = "https://bitbucket.org/pypy/pypy/downloads/pypy2-v5.6.0-linux32.tar.bz2";
                execPath = Paths.get(binDir.toString(), "pypy2-v5.6.0-linux32", "bin/pypy");
            }
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            url = "https://bitbucket.org/pypy/pypy/downloads/pypy2-v5.6.0-osx64.tar.bz2";
            execPath = Paths.get(binDir.toString(), "pypy2-v5.6.0-osx64", "bin/pypy");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            url = "https://bitbucket.org/pypy/pypy/downloads/pypy2-v5.6.0-win32.zip";
            execPath = Paths.get(binDir.toString(), "pypy2-v5.6.0-win32", "pypy.exe");
        }
        if (url == null) {
            getLog().warn("Failed to find PyPy distribution! Default python interpreter will be used.");
            realInterpreter = "python";
            return realInterpreter;
        }
        File execFile = execPath.toFile();
        if (execFile.exists()) {
            return execFile.getAbsolutePath();
        }
        getLog().info("Downloading PyPy distribution from " + url + " ...");
        File tempFile = null;
        try {
            tempFile = Files.createTempFile("pypy-", null).toFile();
            getLog().info("Downloading to: " + tempFile);
            HttpRequest req = HttpRequest.get(url)
                                         .followRedirects(true)
                                         .acceptGzipEncoding();
            int code = req.code();
            int clen = req.contentLength();
            if (clen != -1) {
                getLog().info(clen + " bytes to retrive");
            }
            if (code != 200) {
                throw new IOException("Invalid HTTP response code: " + code);
            }
            try (InputStream is = new ProgressInputStream(req.stream(), new DownloadProgress(), clen);
                 FileOutputStream os = new FileOutputStream(tempFile)) {
                IOUtils.copyLarge(is, os);
            }
            getLog().info("Extracting PyPy distribution into: " + binDir + " ...");
            if (url.endsWith("tar.bz2")) {
                extractTarBz2(tempFile, binDir);
            } else if (url.endsWith(".zip")) {
                extractZIP(tempFile, binDir);
            } else {
                throw new RuntimeException("Unknown archive: " + url);
            }
        } catch (IOException e) {
            String msg = "Failed to process PyPy archive from: " + url;
            getLog().error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        realInterpreter = execPath.toAbsolutePath().toString();
        getLog().info("Using python interpreter: " + realInterpreter);
        return realInterpreter;
    }


    public static String getScriptName() {
        return SCRIPT_NAME;
    }

    public static void setScriptName(String name) {
        SCRIPT_NAME = name;
    }

    public void extractZIP(File file, File targetDir) throws IOException {
        if (!file.canRead()) {
            throw new IOException("Cannot read: " + file);
        }
        targetDir.mkdirs();
        if (!targetDir.isDirectory()) {
            throw new IOException(targetDir + " is not a directory");
        }
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            ZipArchiveInputStream ain = new ZipArchiveInputStream(in);
            ZipArchiveEntry entry;
            while ((entry = (ZipArchiveEntry) ain.getNextEntry()) != null) {
                getLog().debug("Extracting: " + entry.getName());
                File f = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    f.mkdirs();
                } else {
                    try (FileOutputStream fos = new FileOutputStream(f)) {
                        IOUtils.copyLarge(ain, fos);
                    }
                }
            }
        }
    }

    public void extractTarBz2(File file, File targetDir) throws IOException {
        if (!file.canRead()) {
            throw new IOException("Cannot read: " + file);
        }
        targetDir.mkdirs();
        if (!targetDir.isDirectory()) {
            throw new IOException(targetDir + " is not a directory");
        }
        targetDir.mkdirs();
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            TarArchiveInputStream ain = new TarArchiveInputStream(new BZip2CompressorInputStream(in));
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) ain.getNextEntry()) != null) {
                getLog().debug("Extracting: " + entry.getName());
                File f = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    f.mkdirs();
                } else {
                    try (FileOutputStream fos = new FileOutputStream(f)) {
                        IOUtils.copyLarge(ain, fos);
                    }
                    Files.setPosixFilePermissions(f.toPath(), intModeToPosix(entry.getMode() & 0000777));
                }
            }
        }
    }

    public static Set<PosixFilePermission> intModeToPosix(int mode) {
        PosixFilePermission[] perms = PosixFilePermission.values();
        if ((mode & ((1 << perms.length) - 1)) != mode) {
            throw new RuntimeException("Invalid mode: " + mode);
        }
        Set<PosixFilePermission> set = EnumSet.noneOf(PosixFilePermission.class);
        for (int i = 0; i < perms.length; i++) {
            if ((mode & 1) == 1) {
                set.add(perms[perms.length - i - 1]);
            }
            mode >>= 1;
        }
        return set;
    }


    private class DownloadProgress implements ProgressListener {

        long lastPercent;

        long lastUnits;

        final int unit;

        final String unitMark;

        boolean usePercents;

        private DownloadProgress() {
            this(1024 * 1024, "Mb", true);
        }

        private DownloadProgress(int unit, String unitMark, boolean usePercents) {
            this.unit = unit;
            this.unitMark = unitMark;
            this.usePercents = usePercents;
        }

        @Override
        public void onProgressChanged(long uploaded, long total) {
            long currUnits = uploaded / unit;
            if (total == -1 || !usePercents) {
                if (currUnits > lastUnits) {
                    lastUnits = currUnits;
                    getLog().info("... " + currUnits + " " + unitMark);
                }
            } else {
                long currPercents = (long) (((double) uploaded / total) * 100);
                if (currPercents > lastPercent && (currPercents % 5) == 0) {
                    lastPercent = currPercents;
                    getLog().info("... " + currPercents
                                  + "%"
                                  + (currPercents > 9 ? "" : " ")
                                  + (currPercents > 99 ? "" : " ")
                                  + " (" + currUnits + " " + unitMark + ")");
                }
            }
        }
    }
}
