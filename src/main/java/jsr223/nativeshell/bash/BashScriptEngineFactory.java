/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package jsr223.nativeshell.bash;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;


public class BashScriptEngineFactory implements ScriptEngineFactory {

    private static final String NAME = "bash";

    private static final String ENGINE = "Bash interpreter";

    private static final String ENGINE_VERSION = new NativeShellRunner(new Bash()).getInstalledVersion();

    private static final String LANGUAGE = "Bash";

    private static final String LANGUAGE_VERSION = new NativeShellRunner(new Bash()).getMajorVersion();

    private static final Map<String, Object> parameters = new HashMap<String, Object>();

    static {
        parameters.put(ScriptEngine.NAME, NAME);
        parameters.put(ScriptEngine.ENGINE, ENGINE);
        parameters.put(ScriptEngine.ENGINE_VERSION, ENGINE_VERSION);
        parameters.put(ScriptEngine.LANGUAGE, LANGUAGE);
        parameters.put(ScriptEngine.LANGUAGE_VERSION, LANGUAGE_VERSION);
    }

    @Override
    public String getEngineName() {
        return NAME;
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("bash");
    }

    @Override
    public List<String> getMimeTypes() {
        return asList("application/x-sh", "application/x-bash");
    }

    @Override
    public List<String> getNames() {
        return asList("bash", "Bash");
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        String methodCall = m + " ";
        for (String arg : args) {
            methodCall += arg + " ";
        }
        return methodCall;
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "echo -n " + toDisplay;
    }

    @Override
    public String getProgram(String... statements) {
        String program = "#!/bin/bash\n";
        for (String statement : statements) {
            program += statement + "\n";
        }
        return program;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new NativeShellScriptEngine(new Bash());
    }
}
