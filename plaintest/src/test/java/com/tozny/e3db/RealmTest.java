package com.tozny.e3db;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.tozny.e3db.Realm.Companion;

import static com.tozny.e3db.TestUtilities.withTimeout;

public class RealmTest {
    private class LoginHandler implements LoginActionHandler {
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
        String host = "https://api.e3db.com";
        String brokerUrl = "";
        String realmName = "";
        String username = "";
        String password = "";
        String appName = "account";

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
        System.out.println(client.clientId());
    }
}
