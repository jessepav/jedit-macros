package com.illcode.jedit;

import org.apache.commons.exec.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class OS
{
    /**
     * Execute a process synchronously.
     * @param commandLine
     *     the command line. Be sure to quote paths with spaces appropriately.
     * @param workingDir
     *     the working directory for the created process; if null, no working directory will be set.
     * @param exitValue
     *     the expected exit value of the process
     * @param forwardStdStreams
     *     if true, the new process's stdout/err/in will be taken from {@code System.out/err/in}
     * @return the exit code of the process, or -1 if an error occurred.
     */
    public static int executeCommand(String commandLine, File workingDir, int exitValue, boolean forwardStdStreams) {
        final CommandLine cmdLine = CommandLine.parse(commandLine);
        return execCommandImpl(cmdLine, workingDir, exitValue, forwardStdStreams);
    }

    /**
     * Execute a process synchronously.
     * @param executable
     *     the executable to run (without any arguments)
     * @param workingDir
     *     the working directory for the created process; if null, no working directory will be set.
     * @param exitValue
     *     the expected exit value of the process
     * @param forwardStdStreams
     *     if true, the new process's stdout/err/in will be taken from {@code System.out/err/in}
     * @param args
     *     arguments to the executable
     * @return the exit code of the process, or -1 if an error occurred.
     */
    public static int executeCommandWithArgs(String executable, File workingDir, int exitValue,
                                             boolean forwardStdStreams, String... args) {
        final CommandLine cmdLine = new CommandLine(executable);
        cmdLine.addArguments(args, false);
        return execCommandImpl(cmdLine, workingDir, exitValue, forwardStdStreams);
    }

    /**
     * Execute a process synchronously.
     * @param executable
     *     the executable to run (without any arguments)
     * @param workingDir
     *     the working directory for the created process; if null, no working directory will be set.
     * @param exitValue
     *     the expected exit value of the process
     * @param out
     *     stream to which we redirect standard output; if null, no redirection occurs and the values of
     *     {@code err} and {@code in} are ignored.
     * @param err
     *     stream to which we redirect standard error; if null, we redirect stderr to {@code out}.
     * @param in
     *     stream from which we redirect standard input (may be null).
     * @param args
     *     arguments to the executable
     * @return the exit code of the process, or -1 if an error occurred.
     */
    public static int execCommandWithArgsStreams(String executable, File workingDir, int exitValue,
                                                 OutputStream out, OutputStream err, InputStream in,
                                                 String... args) {
        final CommandLine cmdLine = new CommandLine(executable);
        cmdLine.addArguments(args, false);
        return execCommandImplStreams(cmdLine, workingDir, exitValue, out, err, in);
    }

    private static int execCommandImpl(CommandLine cmdLine, File workingDir, int exitValue, boolean forwardStdStreams) {
        return forwardStdStreams ?
            execCommandImplStreams(cmdLine, workingDir, exitValue, System.out, System.err, System.in) :
            execCommandImplStreams(cmdLine, workingDir, exitValue, null, null, null);
    }

    private static int execCommandImplStreams(CommandLine cmdLine, File workingDir, int exitValue,
                                              OutputStream out, OutputStream err, InputStream in) {
        final Executor executor = new DefaultExecutor();
        if (workingDir != null)
            executor.setWorkingDirectory(workingDir);
        executor.setExitValue(exitValue);
        if (out != null)  // it's okay if err and/or in are null
            executor.setStreamHandler(new PumpStreamHandler(out, err != null ? err : out, in));
        int exitVal = 0;
        try {
            exitVal = executor.execute(cmdLine);
        } catch (IOException e) {
            exitVal = -1;
        }
        return exitVal;
    }

    /**
     * Executes a process asynchronously.
     * @param commandLine the command line. Be sure to quote paths with spaces appropriately.
     * @param workingDir the working directory for the spawned process; if null, no working directory
     *                   will be set.
     * @param exitValue the expected exit value of the process
     * @param resultHandler the result of execution will be relayed to this ExecuteResultHandler
     * @return an ExecuteWatchdog that can be used to kill the process, or null if the process failed to start.
     */
    public static ExecuteWatchdog executeCommandAsync(String commandLine, File workingDir, int exitValue,
                                                      ExecuteResultHandler resultHandler) {
        CommandLine cmdLine = CommandLine.parse(commandLine);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        Executor executor = new DefaultExecutor();
        executor.setWatchdog(watchdog);
        if (workingDir != null)
            executor.setWorkingDirectory(workingDir);
        executor.setExitValue(exitValue);
        try {
            executor.execute(cmdLine, resultHandler);
        } catch (IOException e) {
            return null;
        }
        return watchdog;
    }
}
