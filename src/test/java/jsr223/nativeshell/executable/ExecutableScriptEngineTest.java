/*
 * The MIT License (MIT)
 *
 * Copyright (c) [year] [fullname]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package jsr223.nativeshell.executable;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;

import java.io.*;
import java.util.HashMap;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;


public class ExecutableScriptEngineTest {

    private ExecutableScriptEngine scriptEngine;

    private StringWriter scriptOutput;

    private StringWriter scriptError;

    @BeforeClass
    public static void notOnWindows() {
        org.junit.Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("windows"));
        // rest of setup.
    }

    @Before
    public void setup() {
        scriptEngine = new ExecutableScriptEngine();
        scriptOutput = new StringWriter();
        scriptEngine.getContext().setWriter(scriptOutput);
        scriptError = new StringWriter();
        scriptEngine.getContext().setErrorWriter(scriptError);
    }

    @Test
    public void simple_executable() throws Exception {
        Object result = scriptEngine.eval("hostname");

        assertEquals(0, result);
        assertNotEquals("", scriptOutput.toString());
        assertEquals("", scriptError.toString());
    }

    @Test
    public void with_args() throws Exception {
        Object result = scriptEngine.eval("echo hello");

        assertEquals(0, result);
        assertEquals("hello\n", scriptOutput.toString());
        assertEquals("", scriptError.toString());
    }

    @Test
    public void quoted_args() throws Exception {
        Object result = scriptEngine.eval("echo \"hello; bob\"");

        assertEquals(0, result);
        assertEquals("hello; bob\n", scriptOutput.toString());
        assertEquals("", scriptError.toString());
    }

    @Test(expected = ScriptException.class)
    public void non_existing_command() throws Exception {
        scriptEngine.eval("blawhhhhhh");
    }

    @Test(expected = ScriptException.class)
    public void error_returned() throws Exception {
        scriptEngine.eval("false");
    }

    @Test
    public void bindings() throws Exception {
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("var", "value");
        bindings.put("another", "foo");

        scriptEngine.eval("echo $var ${another} $not_existing", bindings);

        assertEquals("value foo $not_existing\n", scriptOutput.toString());
    }

    @Test
    public void null_binding() throws Exception {
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("var", null);

        scriptEngine.eval("echo $var", bindings);

        assertEquals("\n", scriptOutput.toString());
    }

    @Test
    public void exitCodeBindingSuccess() throws Exception {
        Bindings bindings = scriptEngine.createBindings();

        HashMap<String, Serializable> variables = new HashMap<>();
        bindings.put(NativeShellScriptEngine.VARIABLES_BINDING_NAME, variables);

        scriptEngine.eval("echo ok", bindings);

        assertEquals(bindings.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME), 0);
        assertEquals(variables.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME), 0);
    }

    @Test
    public void exitCodeBindingError() throws Exception {
        Bindings bindings = scriptEngine.createBindings();

        HashMap<String, Serializable> variables = new HashMap<>();
        bindings.put(NativeShellScriptEngine.VARIABLES_BINDING_NAME, variables);

        boolean exceptionThrown = false;
        try {
            scriptEngine.eval("pprijbjhbqjhrj", bindings);

        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        assertNotEquals(bindings.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME), 0);
        assertNotEquals(variables.get(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME), 0);
    }

    @Test
    public void reading_input() throws Exception {
        StringReader stringInput = new StringReader("hello\n");
        scriptEngine.getContext().setReader(stringInput);
        assertEquals(NativeShellRunner.RETURN_CODE_OK, scriptEngine.eval("head -n 1"));
        assertEquals("hello\n", scriptOutput.toString());
    }

    @Test
    public void number_bindings() throws Exception {
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("int", 42);
        bindings.put("float", 42.0);

        scriptEngine.eval("echo $int ${float}", bindings);

        assertEquals("42 42.0\n", scriptOutput.toString());
    }

    @Test
    public void collection_bindings() throws Exception {
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("array", new String[] { "one", "two" });
        bindings.put("long_array", "a a a a a a a a a a b".split(" "));
        bindings.put("list", singletonList("l1"));
        bindings.put("map", singletonMap("key", "value"));

        scriptEngine.eval("echo $array_0 $array_1 $list_0 $map_key $long_array_10", bindings);

        assertEquals("one two l1 value b\n", scriptOutput.toString());
    }

    @Test
    public void environment_bindings() throws Exception {
        Bindings bindings = scriptEngine.createBindings();
        bindings.put("var", "foo");

        scriptEngine.eval("printenv var", bindings);

        assertEquals("foo\n", scriptOutput.toString());
    }

    @Test
    public void read_closed_input() throws Exception {
        Reader closedInput = new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("closed");
            }

            @Override
            public void close() throws IOException {

            }
        };
        scriptEngine.getContext().setReader(closedInput);
        scriptEngine.eval("cat");
    }
}
