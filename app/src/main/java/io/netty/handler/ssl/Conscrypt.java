/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.ssl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.net.ssl.SSLEngine;

/**
 * Contains methods that can be used to detect if conscrypt is usable.
 */
final class Conscrypt {
    // This class exists to avoid loading other conscrypt related classes using features only available in JDK8+,
    // because we need to maintain JDK6+ runtime compatibility.
    private static final Method IS_CONSCRYPT_SSLENGINE = loadIsConscryptEngine();
    private static final boolean CAN_INSTANCE_PROVIDER = canInstanceProvider();

    private static Method loadIsConscryptEngine() {
        try {
            Class<?> conscryptClass = Class.forName("org.conscrypt.Conscrypt", true,
                    ConscryptAlpnSslEngine.class.getClassLoader());
            return conscryptClass.getMethod("isConscrypt", SSLEngine.class);
        } catch (Throwable ignore) {
            // Conscrypt was not loaded.
            return null;
        }
    }

    private static boolean canInstanceProvider() {
        try {
            Class<?> providerClass = Class.forName("org.conscrypt.OpenSSLProvider", true,
                    ConscryptAlpnSslEngine.class.getClassLoader());
            providerClass.newInstance();
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * Indicates whether or not conscrypt is available on the current system.
     */
    static boolean isAvailable() {
        // return CAN_INSTANCE_PROVIDER && IS_CONSCRYPT_SSLENGINE != null && PlatformDependent.javaVersion() >= 8;
        return CAN_INSTANCE_PROVIDER && IS_CONSCRYPT_SSLENGINE != null;
    }

    static boolean isEngineSupported(SSLEngine engine) {
        return isAvailable() && isConscryptEngine(engine);
    }

    private static boolean isConscryptEngine(SSLEngine engine) {
        try {
            return (Boolean) IS_CONSCRYPT_SSLENGINE.invoke(null, engine);
        } catch (IllegalAccessException ignore) {
            return false;
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Conscrypt() { }
}
