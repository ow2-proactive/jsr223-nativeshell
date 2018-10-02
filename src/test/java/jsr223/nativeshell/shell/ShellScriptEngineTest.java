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
package jsr223.nativeshell.shell;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.StringReader;
import java.io.StringWriter;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;
import jsr223.nativeshell.bash.Bash;
import jsr223.nativeshell.shebang.Shell;


public class ShellScriptEngineTest {

    private NativeShellScriptEngine scriptEngine;

    private StringWriter scriptOutput;

    private StringWriter scriptError;

    @BeforeClass
    public static void runOnlyOnLinux() {
        assumeTrue(System.getProperty("os.name").contains("Linux"));
    }

    @Before
    public void setup() {
        scriptEngine = new NativeShellScriptEngine(new Shell());
        scriptOutput = new StringWriter();
        scriptEngine.getContext().setWriter(scriptOutput);
        scriptError = new StringWriter();
        scriptEngine.getContext().setErrorWriter(scriptError);
    }

    @Test
    public void evaluateKornScript() throws Exception {
        Integer returnCode = (Integer) scriptEngine.eval("#!/bin/ksh\n" + "ls -l ");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
    }


    @Test
    public void evaluatePerlScript() throws Exception {
        Integer returnCode = (Integer) scriptEngine.eval("#!/usr/bin/perl\nprint 'Hello';");

        assertEquals(NativeShellRunner.RETURN_CODE_OK, returnCode);
        assertEquals("Hello", scriptOutput.toString());
    }


    @Test(expected = ScriptException.class)
    public void evaluate_failing_command() throws Exception {
        scriptEngine.eval("nonexistingcommandwhatsoever");
    }
}
