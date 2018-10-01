package jsr223.nativeshell.shebang;

import jsr223.nativeshell.NativeShellScriptEngine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class ShellEngineFactory implements ScriptEngineFactory {
    @Override
    public String getEngineName() {
        return "shell";
    }

    @Override
    public String getEngineVersion() {
        return null;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("sh");
    }

    @Override
    public List<String> getMimeTypes() {
        return null;
    }

    @Override
    public List<String> getNames() {
        return asList("shell", "Shell");
    }

    @Override
    public String getLanguageName() {
        return null;
    }

    @Override
    public String getLanguageVersion() {
        return null;
    }

    @Override
    public Object getParameter(String s) {
        return null;
    }

    @Override
    public String getMethodCallSyntax(String s, String s1, String... strings) {
        return null;
    }

    @Override
    public String getOutputStatement(String s) {
        return null;
    }

    @Override
    public String getProgram(String... strings) {
        return null;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new NativeShellScriptEngine(new Shell());
    }
}
