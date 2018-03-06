package com.classparser.bytecode.api.agent;

import java.lang.instrument.Instrumentation;

public interface JavaAgent {

    /**
     * Getting instrumentation from agent premain or agentmain method
     *
     * @return Instrumentation instance
     */
    Instrumentation getInstrumentation();

    /**
     * Getting bytecode holder which contains bytecode of retransform classes
     *
     * @return ByteCodeHolder instance
     */
    ByteCodeHolder getByteCodeHolder();

    /**
     * Try check agent is initialize
     *
     * @return boolean status
     */
    boolean isInitialize();
}