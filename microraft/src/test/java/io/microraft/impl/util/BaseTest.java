/*
 * Copyright (c) 2020, MicroRaft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.microraft.impl.util;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class BaseTest {

    @Rule
    public final TestWatcher testWatcher = new TestWatcher() {
        private long start;

        @Override
        protected void succeeded(Description description) {
            super.succeeded(description);

            long durationNanos = System.nanoTime() - start;
            long durationMicros = durationNanos / 1000;
            long durationMillis = durationMicros / 1000;
            long durationSeconds = durationMillis / 1000;
            long duration = durationSeconds > 0 ? durationSeconds : (
                    durationMillis > 0 ? durationMillis : (durationMicros > 0 ? durationMicros : durationNanos));
            String unit =
                    durationSeconds > 0 ? "secs" : (durationMillis > 0 ? "millis" : (durationMicros > 0 ? "micros" : "nanos"));
            System.out.println("+ SUCCEEDED: " + description.getMethodName() + " IN " + duration + " " + unit);
        }

        @Override
        protected void failed(Throwable e, Description description) {
            super.failed(e, description);

            long durationNs = System.nanoTime() - start;
            long durationMicros = durationNs / 1000;
            long durationMs = durationMicros / 1000;
            long durationSecs = durationMs / 1000;
            long duration = durationSecs > 0 ? durationSecs : (
                    durationMs > 0 ? durationMs : (durationMicros > 0 ? durationMicros : durationNs));
            final String unit =
                    durationSecs > 0 ? "secs" : (durationMs > 0 ? "millis" : (durationMicros > 0 ? "micros" : "nanos"));
            System.out.println("- FAILED: " + description.getMethodName() + " IN " + duration + " " + unit);
        }

        @Override
        protected void starting(Description description) {
            super.starting(description);
            System.out.println("- STARTED: " + description.getMethodName());
            start = System.nanoTime();
            super.starting(description);
        }

    };

}