// See the COPYRIGHT file for copyright and license information
package org.znerd.lessc2java.maven.plugins;

import static org.znerd.util.text.TextUtils.quote;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.znerd.lessc2java.Lessc;
import org.znerd.util.io.DirectoryUtils;
import org.znerd.util.log.Limb;
import org.znerd.util.log.LogLevel;
import org.znerd.util.log.MavenLimb;
import org.znerd.util.proc.CommandRunner;
import org.znerd.util.proc.CommonsExecCommandRunner;
import org.znerd.util.text.TextUtils;

/**
 * A Maven plugin for generating source files and/or documentation from Logdoc definitions.
 * 
 * @goal css
 * @phase generate-resources
 */
public class LesscMojo extends AbstractMojo {

    /**
     * @parameter alias="in" default-value="${basedir}/src/main/resources/less"
     */
    private File _sourceDir;

    /**
     * @parameter alias="out" default-value="${basedir}/target/css"
     * @required
     */
    private File _targetDir;

    /**
     * @parameter alias="command" default-value="lessc"
     * @required
     */
    private String _command;

    /**
     * @parameter alias="time-out" default-value="0"
     */
    private long _timeOut;

    /**
     * @parameter alias="overwrite" default-value="false"
     */
    private boolean _overwrite;
    
    /**
     * @parameter alias="failOnError" default-value="true"
     */
    private boolean _failOnError;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            executeImpl();
        } catch (MojoExecutionException cause) {
            handleExecutionException(cause);
        }
    }
        
    private void executeImpl() throws MojoExecutionException {
        sendInternalLoggingThroughMaven();
        checkSourceDirExists();
        transform();
    }

    private void sendInternalLoggingThroughMaven() {
        Limb.setLogger(new MavenLimb(getLog()));
    }

    private void checkSourceDirExists() throws MojoExecutionException {
        try {
            DirectoryUtils.checkDir("Source directory containing Less files", _sourceDir, true, false, false);
        } catch (IOException cause) {
            throw new MojoExecutionException(cause.getMessage(), cause);
        }
    }

    private void transform() throws MojoExecutionException {
        CommandRunner commandRunner = createCommandRunner();
        String[] includedFiles = determineIncludedFiles();
        try {
            Lessc.compile(commandRunner, _sourceDir, includedFiles, _targetDir, _command, _overwrite);
        } catch (IOException cause) {
            throw new MojoExecutionException("Failed to perform transformation.", cause);
        }
    }

    private CommonsExecCommandRunner createCommandRunner() {
        return new CommonsExecCommandRunner(_timeOut);
    }

    private String[] determineIncludedFiles() {
        FilenameFilter filter = new IncludeFilenameFilter();
        return _sourceDir.list(filter);
    }

    private void handleExecutionException(MojoExecutionException cause) throws MojoExecutionException {
        if (_failOnError) {
            throw cause;
        } else {
            Limb.log(LogLevel.WARNING, "Ignoring execution exception, since 'failOnError' is 'false'.", cause);
        }
    }

    class IncludeFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            boolean match = isFileNameMatch(name) && isNotDirectory(dir, name);
            if (match) {
                Limb.log(LogLevel.INFO, "File " + quote(name) + " matches; including.");
            } else {
                Limb.log(LogLevel.INFO, "File " + quote(name) + " does not match; excluding.");
            }
            return match;
        }

        private boolean isFileNameMatch(String name) {
            return TextUtils.matches(name, "\\.less$") && TextUtils.matches(name, "^[^.]");
        }

        private boolean isNotDirectory(File dir, String name) {
            File file = new File(dir, name);
            return !file.isDirectory();
        }
    }
}
