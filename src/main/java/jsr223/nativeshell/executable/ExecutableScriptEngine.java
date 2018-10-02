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

import static jsr223.nativeshell.IOUtils.pipe;
import static jsr223.nativeshell.StringUtils.toEmptyStringIfNull;

import java.io.*;
import java.util.*;

import javax.script.*;

import jsr223.nativeshell.IOUtils;
import jsr223.nativeshell.NativeShellScriptEngine;


public class ExecutableScriptEngine extends AbstractScriptEngine {

    @Override
    public Object eval(String script, ScriptContext scriptContext) throws ScriptException {
        try {

            String commandLineWithBindings = expandAndReplaceBindings(script, scriptContext);
            ProcessBuilder processBuilder = new ProcessBuilder(CommandLine.translateCommandline(commandLineWithBindings));

            Map<String, String> environment = processBuilder.environment();
            for (Map.Entry<String, Object> binding : scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
                environment.put(binding.getKey(), toEmptyStringIfNull(binding.getValue()));
            }

            final Process process = processBuilder.start();
            Thread input = writeProcessInput(process.getOutputStream(), scriptContext.getReader());
            Thread output = readProcessOutput(process.getInputStream(), scriptContext.getWriter());
            Thread error = readProcessOutput(process.getErrorStream(), scriptContext.getErrorWriter());

            input.start();
            output.start();
            error.start();

            process.waitFor();
            output.join();
            error.join();
            input.interrupt();

            int exitValue = process.exitValue();

            if (scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)
                             .containsKey(NativeShellScriptEngine.VARIABLES_BINDING_NAME)) {
                Map<String, Serializable> variables = (Map<String, Serializable>) scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)
                                                                                               .get(NativeShellScriptEngine.VARIABLES_BINDING_NAME);
                variables.put(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME, exitValue);
            }
            scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).put(NativeShellScriptEngine.EXIT_VALUE_BINDING_NAME,
                                                                      exitValue);
            if (exitValue != 0) {
                throw new ScriptException("Command execution failed with exit code " + exitValue);
            }

            return exitValue;
        } catch (ScriptException e) {
            throw e;
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    private String expandAndReplaceBindings(String script, ScriptContext scriptContext) {
        Bindings collectionBindings = createBindings();

        for (Map.Entry<String, Object> binding : scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
            String bindingKey = binding.getKey();
            Object bindingValue = binding.getValue();

            if (bindingValue instanceof Object[]) {
                addArrayBindingAsEnvironmentVariable(bindingKey, (Object[]) bindingValue, collectionBindings);
            } else if (bindingValue instanceof Collection) {
                addCollectionBindingAsEnvironmentVariable(bindingKey, (Collection) bindingValue, collectionBindings);
            } else if (bindingValue instanceof Map) {
                addMapBindingAsEnvironmentVariable(bindingKey, (Map<?, ?>) bindingValue, collectionBindings);
            }
        }

        scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).putAll(collectionBindings);

        Set<Map.Entry<String, Object>> bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).entrySet();

        ArrayList<Map.Entry<String, Object>> sortedBindings = new ArrayList<Map.Entry<String, Object>>(bindings);
        Collections.sort(sortedBindings, LONGER_KEY_FIRST);

        for (Map.Entry<String, Object> binding : sortedBindings) {
            String bindingKey = binding.getKey();
            Object bindingValue = binding.getValue();

            if (script.contains("$" + bindingKey)) {
                script = script.replaceAll("\\$" + bindingKey, toEmptyStringIfNull(bindingValue));
            }
            if (script.contains("${" + bindingKey + "}")) {
                script = script.replaceAll("\\$\\{" + bindingKey + "\\}", toEmptyStringIfNull(bindingValue));
            }
        }
        return script;
    }

    private void addMapBindingAsEnvironmentVariable(String bindingKey, Map<?, ?> bindingValue, Bindings bindings) {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) bindingValue).entrySet()) {
            bindings.put(bindingKey + "_" + entry.getKey(),
                         (entry.getValue() == null ? "" : toEmptyStringIfNull(entry.getValue())));
        }
    }

    private void addCollectionBindingAsEnvironmentVariable(String bindingKey, Collection bindingValue,
            Bindings bindings) {
        Object[] bindingValueAsArray = bindingValue.toArray();
        addArrayBindingAsEnvironmentVariable(bindingKey, bindingValueAsArray, bindings);
    }

    private void addArrayBindingAsEnvironmentVariable(String bindingKey, Object[] bindingValue, Bindings bindings) {
        for (int i = 0; i < bindingValue.length; i++) {
            bindings.put(bindingKey + "_" + i, (bindingValue[i] == null ? "" : toEmptyStringIfNull(bindingValue[i])));
        }
    }

    private static Thread readProcessOutput(final InputStream processOutput, final Writer contextWriter) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pipe(new BufferedReader(new InputStreamReader(processOutput)), new BufferedWriter(contextWriter));
                } catch (IOException ignored) {
                }
            }
        });
    }

    private static Thread writeProcessInput(final OutputStream processOutput, final Reader contextWriter) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pipe(new BufferedReader(contextWriter), new OutputStreamWriter(processOutput));
                } catch (IOException closed) {
                    try {
                        processOutput.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
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
        return new ExecutableScriptEngineFactory();
    }

    public static final Comparator<Map.Entry<String, Object>> LONGER_KEY_FIRST = new Comparator<Map.Entry<String, Object>>() {
        @Override
        public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
            return o2.getKey().length() - o1.getKey().length();
        }
    };

}
