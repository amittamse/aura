/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.impl.renderer;

import java.io.IOException;
import java.io.StringWriter;

import org.auraframework.def.DefDescriptor;
import org.auraframework.def.RendererDef;
import org.auraframework.impl.AuraImplTestCase;
import org.auraframework.impl.java.renderer.JavaRendererDef;
import org.auraframework.impl.renderer.sampleJavaRenderers.TestSimpleRenderer;
import org.auraframework.impl.system.DefDescriptorImpl;
import org.auraframework.instance.Component;
import org.auraframework.system.RenderContext;
import org.auraframework.throwable.AuraError;
import org.auraframework.throwable.AuraExecutionException;
import org.auraframework.throwable.quickfix.InvalidDefinitionException;
import org.auraframework.util.json.JsonEncoder;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test class to verify implementation of Java (server side) renderers for component.
 */
public class JavaRendererDefTest extends AuraImplTestCase {
    public Component dummyCmp = null;
    StringWriter sw = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        sw = new StringWriter();
    }

    @Override
    public void tearDown() throws Exception {
        sw.close();
        super.tearDown();
    }

    /**
     * Verify that server side renderers are defined as local.
     */
    @Test
    public void testIsLocal() throws Exception {
        JavaRendererDef.Builder builder = new JavaRendererDef.Builder().setRendererClass(TestSimpleRenderer.class);
        JavaRendererDef def = builder.build();
        assertTrue("Server side renderers should be defined as Local", def.isLocal());
    }

    /**
     * Verify that JavaRendererDef creates nothing when serialized.
     */
    @Test
    public void testSerializedFormat() throws Exception {
        JavaRendererDef def = createRenderer("java://org.auraframework.impl.renderer.sampleJavaRenderers.TestSimpleRenderer");
        assertTrue(JsonEncoder.serialize(def, false, false).isEmpty());
    }

    /**
     * Verify that calling render function on JavaRendererDef returns the mark up generated by render() method in the
     * renderer.
     * 
     * @expectedResults JavaRendererDef.render() function accepts a character stream and returns the stream, populated
     *                  with markup.
     */
    @Test
    public void testInvokeRender() throws Exception {
        JavaRendererDef def = createRenderer("java://org.auraframework.impl.renderer.sampleJavaRenderers.TestSimpleRenderer");
        RenderContext rc = Mockito.mock(RenderContext.class);
        Mockito.when(rc.getCurrent()).thenReturn(sw);
        def.render(dummyCmp, rc);
        this.goldFileText(sw.toString());
    }

    private Appendable getThrower(Throwable t) throws Exception {
        Appendable thrower = Mockito.mock(Appendable.class);
        Mockito.doThrow(t).when(thrower).append(Mockito.anyChar());
        Mockito.doThrow(t).when(thrower).append(Mockito.any(CharSequence.class), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doThrow(t).when(thrower).append(Mockito.any(CharSequence.class));
        return thrower;
    }

    /**
     * Verify that Exceptions/Errors are surfaced.
     * 
     * ComponentImpl just makes render() call on the RenderDef object. All exceptions should be wrapped in
     * AuraExecutionException, while errors and quickfix exceptions are passed through.
     */
    @Test
    public void testExceptionThrownByComponentRendererHandled() throws Exception {
        JavaRendererDef def = createRenderer("java://org.auraframework.impl.renderer.sampleJavaRenderers.TestSimpleRenderer");
        RuntimeException re = new RuntimeException("expected");
        RenderContext rc;

        IOException ioe = new IOException();
        rc = Mockito.mock(RenderContext.class);
        Appendable append = getThrower(ioe);
        Mockito.when(rc.getCurrent()).thenReturn(append);
        try {
            def.render(null, rc);
            fail("no exception on a throwing appendable");
        } catch (AuraExecutionException expected) {
            assertEquals("Did not throw wrapped IOException", ioe, expected.getCause());
        }

        AuraError err = new AuraError("expected");
        rc = Mockito.mock(RenderContext.class);
        append = getThrower(err);
        Mockito.when(rc.getCurrent()).thenReturn(append);
        try {
            def.render(null, rc);
            fail("No exception on a throwing appendable.");
        } catch (AuraError expected) {
            assertEquals("Did not throw error", err, expected);
        }

        rc = Mockito.mock(RenderContext.class);
        append = getThrower(re);
        Mockito.when(rc.getCurrent()).thenReturn(append);
        try {
            def.render(null, rc);
            fail("no exception on a throwing appendable");
        } catch (AuraExecutionException expected) {
            assertEquals("Did not throw error", re, expected.getCause());
        }

        // Make sure ArithmeticExceptions are wrapped and sent up the chain
        def = createRenderer("java://org.auraframework.impl.renderer.sampleJavaRenderers.TestRendererThrowingException");
        rc = Mockito.mock(RenderContext.class);
        Mockito.when(rc.getCurrent()).thenReturn(Mockito.mock(Appendable.class));
        try {
            def.render(dummyCmp, rc);
            fail("Should be able to catch exceptions during rendering.");
        } catch (AuraExecutionException e) {
            // The thrown Exception should be AuraExecutionException, but we should still have the ArithemeticException
            // with the correct message for the cause
            checkExceptionFull(e.getCause(), ArithmeticException.class, "From TestRendererThrowingException");
        }

        // Make sure QuickFixExceptions are not swallowed
        def = createRenderer("java://org.auraframework.impl.renderer.sampleJavaRenderers.TestRendererThrowsQFEDuringRender");
        rc = Mockito.mock(RenderContext.class);
        Mockito.when(rc.getCurrent()).thenReturn(Mockito.mock(Appendable.class));
        try {
            def.render(dummyCmp, rc);
            fail("Should be able to catch QuickFixExceptions during rendering.");
        } catch (Exception e) {
            checkExceptionFull(e, InvalidDefinitionException.class, "From TestRendererThrowsQFEDuringRender");
        }
    }

    /**
     * create a renderer def from a qualified name of a java class.
     * 
     * @param qualifiedName
     * @return the new RendererDef
     * @throws Exception
     */
    private JavaRendererDef createRenderer(String qualifiedName) throws Exception {
        JavaRendererDef.Builder builder = new JavaRendererDef.Builder();
        DefDescriptor<RendererDef> descriptor = DefDescriptorImpl.getInstance(qualifiedName, RendererDef.class);
        Class<?> rendererClass = Class.forName(String.format("%s.%s", descriptor.getNamespace(), descriptor.getName()));

        builder.setLocation(rendererClass.getCanonicalName(), -1);
        builder.setRendererClass(rendererClass);
        return builder.build();
    }
}
