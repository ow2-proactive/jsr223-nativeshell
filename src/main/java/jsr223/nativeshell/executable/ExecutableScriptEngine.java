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
package jsr223.nativeshell.executable;

import static jsr223.nativeshell.IOUtils.pipe;
import static jsr223.nativeshell.StringUtils.toEmptyStringIfNull;

import java.io.*;
import java.util.*;

import javax.script.*;

import org.ow2.proactive.scheduler.common.SchedulerConstants;
import org.ow2.proactive.utils.CookieBasedProcessTreeKiller;

import jsr223.nativeshell.IOUtils;
import jsr223.nativeshell.NativeShellRunner;
import jsr223.nativeshell.NativeShellScriptEngine;


public class ExecutableScriptEngine extends AbstractScriptEngine {

    @Override
    public Object eval(String script, ScriptContext scriptContext) throws ScriptException {
        CookieBasedProcessTreeKiller processTreeKiller = null;
        try {

            String commandLineWithBindings = expandAndReplaceBindings(script, scriptContext);
            ProcessBuilder processBuilder = new ProcessBuilder(CommandLine.translateCommandline(commandLineWithBindings));

            Map<String, String> environment = processBuilder.environment();
            for (Map.Entry<String, Object> binding : scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
                environment.put(binding.getKey(), toEmptyStringIfNull(binding.getValue()));
            }
            processTreeKiller = NativeShellRunner.createProcessTreeKiller(scriptContext, environment);

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
                             .containsKey(SchedulerConstants.VARIABLES_BINDING_NAME)) {
                Map<String, Serializable> variables = (Map<String, Serializable>) scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)
                                                                                               .get(SchedulerConstants.VARIABLES_BINDING_NAME);
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
        } finally {
            if (processTreeKiller != null) {
                processTreeKiller.kill();
            }
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
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processOutput));
                        BufferedWriter bufferedWriter = new BufferedWriter(contextWriter)) {
                    pipe(bufferedReader, bufferedWriter, null);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private static Thread writeProcessInput(final OutputStream processOutput, final Reader contextWriter) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try (BufferedReader bufferedReader = new BufferedReader(contextWriter);
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(processOutput)) {
                    pipe(bufferedReader, outputStreamWriter, null);
                } catch (Exception ignored) {

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
