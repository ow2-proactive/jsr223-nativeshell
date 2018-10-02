package jsr223.nativeshell.shebang;

import jsr223.nativeshell.NativeShell;

import javax.script.ScriptEngineFactory;
import java.io.File;

public class Shell implements NativeShell {
    @Override
    public ProcessBuilder createProcess(File commandAsFile) {
        return new ProcessBuilder(commandAsFile.getAbsolutePath());
    }

    @Override
    public ProcessBuilder createProcess(String command) {
        throw new RuntimeException("One line shell sctipt is not supported: shebang nodation required");
    }

    @Override
    public String getInstalledVersionCommand() {
        return "Not available";
    }

    @Override
    public String getMajorVersionCommand() {
        return "Not available";
    }

    @Override
    public ScriptEngineFactory getScriptEngineFactory() {
        return new ShellEngineFactory();
    }

    @Override
    public String getFileExtension() {
        return ".sh";
    }
}
