package jsr223.nativeshell.bash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;


public class BashScriptEngineFactoryTest {

    @Test
    public void testBashScriptEngineIsFound() {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

        assertNotNull(scriptEngineManager.getEngineByExtension("bash"));
        assertNotNull(scriptEngineManager.getEngineByName("bash"));
        assertEquals("bash", scriptEngineManager.getEngineByMimeType("application/x-bash").getFactory().getEngineName());
    }

    @Test
    public void testBashScriptEngineVersions() {
        ScriptEngine bashScriptEngine = new ScriptEngineManager().getEngineByExtension("bash");

        assertNotNull(bashScriptEngine.getFactory().getEngineVersion());
        assertNotNull(bashScriptEngine.getFactory().getLanguageVersion());
    }
}
