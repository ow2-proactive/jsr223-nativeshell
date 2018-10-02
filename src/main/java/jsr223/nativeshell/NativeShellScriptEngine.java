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
package jsr223.nativeshell;

import java.io.Reader;
import java.io.Serializable;
import java.util.Map;

import javax.script.*;


public class NativeShellScriptEngine extends AbstractScriptEngine {

    public static final String ENABLE_VERSION_PROPERTY_NAME = "jsr223.nativeshell.enableVersionCheck";

    public static final String DEFAULT_VERSION = "1.0.0";

    public static final String DEFAULT_MAJOR_VERSION = "1";

    public static final String EXIT_VALUE_BINDING_NAME = "EXIT_VALUE";

    public static final String VARIABLES_BINDING_NAME = "variables";

    private NativeShell nativeShell;

    public NativeShellScriptEngine(NativeShell nativeShell) {
        this.nativeShell = nativeShell;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        int exitValue = new NativeShellRunner(nativeShell).run(script, context);
        if (context.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(VARIABLES_BINDING_NAME)) {
            Map<String, Serializable> variables = (Map<String, Serializable>) context.getBindings(ScriptContext.ENGINE_SCOPE)
                                                                                     .get(VARIABLES_BINDING_NAME);
            variables.put(EXIT_VALUE_BINDING_NAME, exitValue);
        }
        context.getBindings(ScriptContext.ENGINE_SCOPE).put(EXIT_VALUE_BINDING_NAME, exitValue);
        if (exitValue != 0) {
            throw new ScriptException("Script failed with exit code " + exitValue);
        }
        return exitValue;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return eval(IOUtils.toString(reader), context);
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return nativeShell.getScriptEngineFactory();
    }
}
