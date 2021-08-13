package com.tozny.e3db;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.tozny.e3db.TestUtilities.withTimeout;
import static org.junit.Assert.assertFalse;

public class RealmTest {
    String host = "https://api.e3db.com";
    String brokerUrl = "";
    String realmName = "";
    String token = "";
    String username = "";
    String password = "";
    String email = "";
    String appName = "account";

    @Test
    public void testRealmRegistration() throws Exception {
        AtomicReference<Throwable> throwableRef = new AtomicReference<>();
        Realm realm = new Realm(realmName, appName, new URI(brokerUrl), new URI(host));
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        AtomicReference<PartialIdentityClient> pic = new AtomicReference<>();
        withTimeout(new AsyncAction() {
            @Override
            public void act(CountDownLatch wait) throws Exception {
                String identifier = Long.toString(Math.abs(UUID.randomUUID().getLeastSignificantBits()), Character.MAX_RADIX);
                realm.register("+" + identifier + "@tozny.com", "test123", token, email + identifier + "@tozny.com", null, null, 600, r -> {
                    if (r.isError()) {
                        wait.countDown();
                        atomicBoolean.set(false);
                        throwableRef.set(r.asError().other());
                    } else {
                        pic.set(r.asValue());
                        wait.countDown();
                    }
                });
            }
        });
        if (!atomicBoolean.get()) {
            throw new Error(throwableRef.get());
        }
    }

    private static class LoginHandler implements LoginActionHandler {
        @NotNull
        @Override
        public Map<String, Object> handleAction(@NotNull LoginAction loginAction) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("", "");
            return data;
        }
    }

    @Test
    public void testLogin() throws Exception {
        Realm realm = new Realm(realmName, appName, new URI(brokerUrl), new URI(host));
        AtomicReference<Throwable> throwableRef = new AtomicReference<>();
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        AtomicReference<IdentityClient> pic = new AtomicReference<>();
        withTimeout(new AsyncAction() {
            @Override
            public void act(CountDownLatch wait) throws Exception {
                realm.login(username, password, new LoginHandler(), r -> {
                   if (r.isError()) {
                       System.out.println();
                       atomicBoolean.set(false);
                       throwableRef.set(r.asError().other());
                   } else {
                       System.out.println("Logged in");
                       pic.set(r.asValue());
                   }
                   wait.countDown();
                });
            }
        });
        if (!atomicBoolean.get()) {
            throw new Error(throwableRef.get());
        }
        IdentityClient partialIdentityClient = pic.get();
        Client client = partialIdentityClient.getClient();
    }

    // Timeout can occur here. Re-ran the test passes.
    @Test
    public void brokerInitiateLogin() throws Exception {
        Realm realm = new Realm(realmName, "account", new URI(brokerUrl), new URI(host));
        withTimeout(new AsyncAction() {
            @Override
            public void act(CountDownLatch wait) throws Exception {
                realm.initiateBrokerLogin(email, r -> {
                    assertFalse(r.isError());
                    wait.countDown();
                });
            }
        });
    }
}
