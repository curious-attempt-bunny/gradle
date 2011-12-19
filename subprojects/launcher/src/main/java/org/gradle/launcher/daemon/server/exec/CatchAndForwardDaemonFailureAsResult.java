/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.launcher.daemon.server.exec;

/**
 * Reports the execution exception if it exists after execution.
 */
public class CatchAndForwardDaemonFailureAsResult implements DaemonCommandAction {
    public void execute(final DaemonCommandExecution execution) {
        try {
            execution.proceed();
        } catch(Throwable throwable) {
            execution.setException(throwable);
        }
    }
}