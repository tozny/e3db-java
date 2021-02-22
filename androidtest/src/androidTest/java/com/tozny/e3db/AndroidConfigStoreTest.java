/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3db;

import android.content.Context;

import androidx.biometric.BiometricPrompt;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import android.util.Log;

import com.tozny.e3db.android.*;
import org.junit.Test;

import java.security.UnrecoverableKeyException;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class AndroidConfigStoreTest {
  private static final String TAG = "AndroidConfigStoreTest";
  private static final String name  = "foo";
  private ConfigStore.SaveHandler badParamSaveHandler = new ConfigStore.SaveHandler() {
    @Override
    public void saveConfigDidSucceed() {
      assertFalse("TestCase: Unexpected saveConfigDidSucceed", true);
    }

    @Override
    public void saveConfigDidCancel() {
      assertFalse("TestCase: Unexpected saveConfigDidCancel", true);
    }

    @Override
    public void saveConfigDidFail(Throwable e) {
      if (e.getLocalizedMessage().startsWith("TestCase: Unexpected")) {
        throw new RuntimeException(e);
      }

      Log.d(TAG, e.getLocalizedMessage());
      assertTrue(true);
    }
  };
  private ConfigStore.LoadHandler badParamLoadHandler = new ConfigStore.LoadHandler() {
    @Override
    public void loadConfigDidSucceed(String config) {
      assertFalse("TestCase: Unexpected loadConfigDidSucceed", true);
    }

    @Override
    public void loadConfigDidCancel() {
      assertFalse("TestCase: Unexpected loadConfigDidCancel", true);
    }

    @Override
    public void loadConfigNotFound() {
      assertFalse("TestCase: Unexpected loadConfigNotFound", true);
    }

    @Override
    public void loadConfigDidFail(Throwable e1) {
      if (e1.getLocalizedMessage().startsWith("TestCase: Unexpected")) {
        throw new RuntimeException(e1);
      }

      Log.d(TAG, e1.getLocalizedMessage());
      assertTrue(true);
    }
  };
  private ConfigStore.RemoveHandler badParamRemoveHandler = new ConfigStore.RemoveHandler() {
    @Override
    public void removeConfigDidSucceed() {
      assertFalse("TestCase: Unexpected removeConfigDidSucceed", true);
    }

    @Override
    public void removeConfigDidFail(Throwable e1) {
      if (e1.getLocalizedMessage().startsWith("TestCase: Unexpected")) {
        throw new RuntimeException(e1);
      }

      Log.d(TAG, e1.getLocalizedMessage());
      assertTrue(true);
    }
  };

  private void testStringWithKPNone(final String config) throws InterruptedException {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ConfigStore ConfigStore = new AndroidConfigStore(context, name);

    Config.saveConfigSecurely(ConfigStore, config, new ConfigStore.SaveHandler() {
      @Override
      public void saveConfigDidSucceed() {
        Config.loadConfigSecurely(ConfigStore, new ConfigStore.LoadHandler() {
          @Override
          public void loadConfigDidSucceed(String cfg) {
            if(! config.equals(cfg))
              throw new RuntimeException("configs not equal");
          }

          @Override
          public void loadConfigDidCancel() {
            throw new RuntimeException("TestCase: Unexpected loadConfigDidCancel");
          }

          @Override
          public void loadConfigNotFound() {
            throw new RuntimeException("TestCase: Unexpected loadConfigNotFound");
          }

          @Override
          public void loadConfigDidFail(Throwable e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
          }
        });
      }

      @Override
      public void saveConfigDidCancel() {
        throw new RuntimeException("TestCase: Unexpected saveConfigDidCancel");
      }

      @Override
      public void saveConfigDidFail(Throwable e) {
        throw new RuntimeException(e.getLocalizedMessage(), e);
      }
    });
  }

  @Test
  public void testSaveLoadPasswordProtected() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final String testPassword = UUID.randomUUID().toString();
    final String identifier = UUID.randomUUID().toString();

    final KeyAuthenticator keyAuthenticator = new KeyAuthenticator() {
      @Override
      public void getPassword(PasswordHandler handler) {
        try {
          handler.handlePassword(testPassword);
        } catch (UnrecoverableKeyException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void authenticateWithLockScreen(AuthenticateHandler handler) {
        fail("Incorrect authentication.");
      }

      @Override
      public void authenticateWithFingerprint(FingerprintManagerCompat.CryptoObject cryptoObject, AuthenticateHandler handler) {
        fail("Incorrect authentication.");
      }

      @Override
      public void authenticateWithBiometric(BiometricPrompt.CryptoObject cryptoObject, AuthenticateHandler handler) {
        fail("Incorrect authentication.");
      }
    };
    ConfigStore ConfigStore = new AndroidConfigStore(context, identifier, KeyAuthentication.withPassword(), keyAuthenticator);

    Config.saveConfigSecurely(ConfigStore, "foo", new ConfigStore.SaveHandler() {
      @Override
      public void saveConfigDidSucceed() {
      }

      @Override
      public void saveConfigDidCancel() {
        fail("Cancelled while saving.");
      }

      @Override
      public void saveConfigDidFail(Throwable e) {
        throw new RuntimeException(e);
      }
    });

    Config.loadConfigSecurely(ConfigStore, new ConfigStore.LoadHandler() {
      @Override
      public void loadConfigDidSucceed(String config) {

      }

      @Override
      public void loadConfigDidCancel() {
        fail("Cancelled while loading:" + identifier);
      }

      @Override
      public void loadConfigNotFound() {
        fail("Config not found: " + identifier);
      }

      @Override
      public void loadConfigDidFail(Throwable e) {
        throw new RuntimeException("Error loading config: " + identifier, e);
      }
    });
  }

  @Test
  public void testSaveLoadShortStringKPNone() throws InterruptedException {
    final String shortString = "hello";
    testStringWithKPNone(shortString);
  }

  @Test
  public void testSaveLoadLongStringKPNone() throws InterruptedException {
    final String longString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec pellentesque orci eget ipsum porttitor, tincidunt luctus massa pharetra. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Pellentesque elementum nibh nec vehicula aliquam. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Quisque id lobortis ante. Sed auctor, magna id scelerisque semper, nulla nulla placerat justo, at tempus mauris lectus sit amet sem. Nam luctus sem in velit ornare, eu malesuada orci lobortis. Nulla facilisi. Suspendisse potenti. Integer vitae facilisis nunc. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed interdum est est. Morbi dignissim dictum congue. Quisque non dictum sem, ut auctor tellus. Donec auctor porta commodo. Aenean sit amet porta elit, vitae euismod diam. Vivamus a neque dolor. Curabitur ultricies purus nec dignissim iaculis. Pellentesque sed urna non ante condimentum finibus nec ac leo. Curabitur non consequat nisl. Proin tempus velit at tincidunt maximus. Curabitur a diam at nulla condimentum convallis. Nullam lacinia dictum mi id posuere. Fusce eu erat vel turpis interdum laoreet quis sit amet elit. Vivamus sit amet consequat orci, nec efficitur neque. Pellentesque venenatis ornare libero at accumsan. Vestibulum sed fringilla tellus, vel vestibulum mauris. Curabitur accumsan ultricies faucibus. Aenean diam arcu, posuere sed consequat vitae, vulputate vitae lorem. Maecenas eget ullamcorper eros. Curabitur convallis semper eros. Aenean turpis erat, faucibus sit amet ultrices eget, aliquam eu tortor. Nulla a tortor et purus fermentum commodo non eu leo. Cras mattis, nibh a facilisis bibendum, erat nisl vehicula sem, quis bibendum orci augue sit amet velit. Mauris dapibus metus metus, eget commodo sapien scelerisque vitae. Integer eget pulvinar enim, ut vehicula ligula. Morbi condimentum leo eget efficitur gravida. Sed ultrices vitae libero vulputate venenatis. Donec lobortis arcu urna, nec facilisis magna convallis ac. Aenean aliquet eleifend eros id rhoncus. Donec ultrices, elit id interdum malesuada, ante quam auctor felis, ac consectetur mi ante sed magna. Fusce ipsum arcu, tempus quis nunc quis, feugiat sollicitudin lorem. Suspendisse ut mi et libero rutrum dignissim. Curabitur iaculis sem velit, at semper neque luctus vitae. In eu felis lorem. Morbi tincidunt mattis fringilla. Pellentesque in velit sit amet nunc egestas vehicula sit amet at nunc. Vivamus ac efficitur sapien. Quisque quis ornare nunc. Maecenas consequat, leo laoreet efficitur eleifend, arcu nisl malesuada purus, eget rutrum neque magna tincidunt nunc. Vivamus luctus molestie lorem, luctus iaculis lorem. Duis id mattis leo. Aliquam rutrum eros mollis felis sagittis dictum. Suspendisse facilisis ultricies sagittis. Ut lacus elit, egestas in tempus eu, lacinia eget justo. Aenean porta, odio quis aliquet ornare, libero orci dictum est, at dignissim quam eros vehicula velit. Vivamus vehicula nibh eget dui eleifend ullamcorper. Duis venenatis pellentesque ligula eget tempus. Nullam quis quam mattis, porta enim eget, tempor lorem. Nulla sodales justo sed dui tempor, ac imperdiet risus egestas. Fusce viverra ex egestas elementum tristique. In fringilla neque congue libero hendrerit, eu semper risus pharetra. Aenean commodo rutrum tortor, ac efficitur arcu iaculis a. In in metus turpis. Nulla scelerisque tristique augue, sed suscipit nulla. Nullam fringilla, metus quis consectetur elementum, tortor lorem rhoncus velit, sed pretium erat enim non felis. Aliquam lacus tellus, consequat vitae augue eget, interdum varius odio. Suspendisse sed hendrerit nisi. Duis et nibh vel nunc euismod ultrices non et mi. Nullam at dolor vestibulum, porttitor turpis in, lacinia felis. Nulla non est sapien. Nunc placerat tincidunt dolor vel aliquam. Quisque feugiat erat vitae felis dignissim pharetra. Morbi venenatis purus et orci fermentum tincidunt. Donec ac risus ultricies lectus facilisis auctor. Ut vitae ipsum dapibus, lobortis libero sed, finibus arcu. Etiam quam lorem, efficitur ut odio nec, suscipit sagittis ipsum. Ut in ornare ipsum, vitae fermentum libero. Etiam non dui pharetra, pretium magna eget, congue tortor. Nunc tempus magna tellus. Vivamus ultricies elit augue, a mollis justo blandit non. Sed sed urna elementum, dignissim odio vitae, rutrum eros. Sed sit amet lacus semper, viverra est quis, posuere erat. Aliquam egestas suscipit ligula, a interdum libero sollicitudin ac. Praesent dictum nisi ac varius ornare. Proin rutrum non felis eu dignissim. Nullam vel enim tellus. Curabitur quis diam non tellus sagittis vulputate vel ac sapien. Sed mattis dapibus mattis. Maecenas vulputate nisl eget fermentum scelerisque. Integer et leo quis ex pretium tincidunt id vitae enim. Fusce et scelerisque ligula. Mauris ornare eu nisi quis placerat. Donec pretium maximus luctus. Morbi sed interdum nisi. Duis justo lacus, tempor at velit ac, pretium pharetra purus. Quisque lorem nisi, finibus quis sollicitudin quis, laoreet sit amet libero. Sed rutrum odio ante, vitae cursus odio posuere quis. Donec pharetra augue non eros mollis venenatis. Quisque nec elit non nibh scelerisque congue. Aliquam varius orci nunc, vel malesuada magna ultrices sed. Maecenas consequat viverra odio, eget sollicitudin ex scelerisque convallis. Aliquam imperdiet sem a mi luctus dignissim ac ac felis. Integer interdum pharetra diam. Donec finibus dolor tortor, vitae lobortis lorem efficitur ac. Nunc quis mauris ut mi volutpat ullamcorper. Proin interdum purus vitae mauris tincidunt, aliquam varius nulla aliquet. Integer placerat est non risus ultricies, sit amet euismod justo feugiat. Morbi tempus molestie ipsum sit amet cursus. Vestibulum placerat vitae justo at mattis. Nunc auctor ipsum vel libero dictum, vel commodo nibh ultrices. Morbi viverra luctus neque, vel gravida mi gravida a. Nulla gravida, urna non vulputate mattis, leo nunc aliquam ex, eu egestas nisl est id erat. Duis suscipit nulla sed magna mollis, non molestie arcu mattis. In imperdiet ultrices ligula, varius interdum est sagittis et. Nulla molestie sapien elit, in dignissim nibh finibus ac. Suspendisse a lectus ut augue dapibus efficitur vel sed augue. Vestibulum tempus id augue a eleifend. Phasellus vitae gravida est. Integer varius placerat consectetur. Vestibulum vehicula nisi nec nisl tincidunt, nec posuere eros ultricies. Donec finibus, orci at dignissim condimentum, eros urna finibus neque, a porta neque mi vitae quam. Maecenas volutpat fringilla mollis. Suspendisse potenti. Aliquam in diam eu tortor placerat pharetra ac at tortor. Integer non ligula ullamcorper, placerat erat id, tincidunt diam. Ut rhoncus tellus ut mauris facilisis tristique. Nulla suscipit sagittis blandit. Vestibulum quis justo sit amet nisl venenatis pretium quis quis tellus. Nullam eget elit nec nisi commodo facilisis sed non lectus. Proin commodo, ante sit amet consequat auctor, lacus massa scelerisque leo, sed commodo arcu purus eget nulla. Vestibulum imperdiet erat et rhoncus molestie. Vestibulum vitae iaculis ante, in ullamcorper velit. Praesent pulvinar magna ac ligula rhoncus, vitae lacinia velit efficitur. Morbi eu commodo lorem. Nulla sapien leo, dapibus nec magna non, varius dapibus mi. Ut scelerisque tellus eget risus tempor accumsan. Aliquam et arcu id mi aliquet euismod. Nunc eget porta sapien. Nullam pulvinar convallis enim vitae porta. Curabitur ut urna mi. Donec nec ultricies ipsum, mattis commodo tellus. Suspendisse sit amet metus eget lectus consequat accumsan. Aliquam viverra faucibus cursus. Mauris vel lobortis purus, ut faucibus nisi. Mauris accumsan nibh sed turpis vulputate, nec tristique dui lobortis. Fusce quis tincidunt libero. Nullam ac tellus vel orci aliquet pulvinar vel egestas neque. Ut fringilla odio nec molestie accumsan. Cras condimentum tellus et metus sagittis, sit amet consectetur quam molestie. Sed ut diam non lacus pellentesque elementum quis at ante. Donec scelerisque maximus erat molestie viverra. Nulla facilisi. Morbi porta enim placerat erat cursus fermentum ac sit amet magna. Sed arcu diam, tincidunt a consectetur ut, interdum vitae mauris. Donec sollicitudin tellus sed erat dapibus volutpat. Sed diam tortor, varius eu ullamcorper vitae, facilisis in justo. Quisque pulvinar tortor felis, eu posuere est blandit vel. Vivamus placerat lacinia elit sit amet pharetra. Suspendisse justo odio, suscipit fermentum est non, ultrices dapibus velit. Proin eu justo id leo blandit eleifend et in nunc. Duis sit amet hendrerit enim. Aliquam quis magna sed felis cursus tincidunt vitae ut arcu. Curabitur sit amet euismod augue. Vestibulum arcu mi, imperdiet ut magna eu, ornare commodo eros. Suspendisse quis condimentum massa, vel auctor nunc. Nullam vehicula leo id risus eleifend, at euismod mauris varius. Cras egestas ligula a sapien dapibus, vel lacinia erat sagittis. Quisque tincidunt in nulla sodales maximus. Sed consectetur ultricies sapien at rutrum. Fusce tincidunt auctor sollicitudin. Nulla et enim sit amet nisi suscipit ultricies ut a libero. Morbi malesuada euismod porta. Cras sed diam vitae metus cursus consectetur ut ut erat. Integer massa orci, hendrerit et euismod et, rhoncus malesuada enim. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam fermentum blandit augue, eu congue nunc condimentum eu. Nunc id est at metus posuere lacinia. Donec auctor lacinia dignissim. Integer tincidunt nunc eu pellentesque laoreet. Curabitur ut nibh vestibulum ex gravida porta. Nullam suscipit fermentum purus, eu consectetur enim vestibulum eu. Suspendisse non arcu nisi. Quisque sit amet orci vitae eros congue tempus et vitae dolor. Suspendisse eu diam sodales, efficitur elit eget, vulputate felis. Donec sagittis ac urna et tincidunt. Sed magna mauris, interdum ut nunc et, lobortis sollicitudin mauris. Fusce tristique, urna quis viverra lacinia, enim nunc sagittis massa, eu rhoncus eros orci eget enim. Morbi eget elit justo. Donec fringilla, sapien eget efficitur facilisis, enim nisi lacinia lacus, varius euismod odio augue eget mi. Curabitur libero mauris, hendrerit ultrices mi at, tristique vulputate massa. Fusce est nisl, volutpat at ante nec, blandit vestibulum ipsum. Nunc dignissim varius tortor, sed aliquam odio vestibulum vel. Quisque finibus et ligula sit amet ornare. Vivamus tortor neque, hendrerit vel sagittis sit amet, luctus quis nulla. Nulla facilisi. Fusce sagittis, massa ut consequat rhoncus, lacus metus interdum sem, sed sodales orci nulla ac leo. Vestibulum feugiat mauris a turpis convallis, id tincidunt nisi blandit. Donec scelerisque sapien eu purus dapibus tempus. Curabitur in nunc sapien. Etiam in nisl id lorem accumsan ultricies sit amet ut neque. Donec elementum, eros vel bibendum venenatis, nunc elit semper eros, sit amet ullamcorper nunc eros in turpis. Aenean sed mollis lectus. In dignissim velit sed elit gravida, vel accumsan sem molestie. Fusce neque ligula, mattis vitae scelerisque vel, semper vitae sapien. Quisque libero enim, venenatis vitae tellus mollis, pellentesque finibus lorem. Curabitur eu erat a odio pulvinar molestie. Aenean vitae lorem ac nulla bibendum viverra. Aliquam elementum augue vel magna tincidunt, non porttitor est convallis. Maecenas semper bibendum venenatis. Suspendisse potenti. Curabitur at volutpat mauris. Praesent lectus orci, tincidunt eu posuere quis, vulputate at massa. Vestibulum scelerisque hendrerit orci, nec fermentum tortor. Vestibulum convallis laoreet venenatis. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Donec id erat vehicula, facilisis lacus ut, porttitor justo. Quisque erat ante, laoreet eu sagittis consequat, suscipit sit amet quam. Fusce dignissim commodo quam eget imperdiet. Donec ac cursus enim. Mauris porttitor diam lectus, nec sollicitudin odio sollicitudin at. Nulla vel libero justo. Integer accumsan pellentesque augue, vel volutpat turpis rutrum non. Ut quis euismod lorem, eget pulvinar purus. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam porta lacus ut nulla suscipit vestibulum. Ut ac est suscipit quam rutrum laoreet posuere gravida metus. Nunc maximus pharetra leo. Vestibulum vitae erat mattis, hendrerit metus sed, dapibus erat. Donec vitae diam tincidunt, vestibulum augue nec, euismod purus.";
    testStringWithKPNone(longString);
  }

  @Test
  public void testSaveLoadEmptyStringKPNone() throws InterruptedException {
    final String emptyString = "";
    testStringWithKPNone(emptyString);
  }

  @Test
  public void testAndroidConfigHelperParams() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    /* Null Context */
    try {
      new AndroidConfigStore(null, name);
      fail("Didn't hit expected exception.");
    } catch (IllegalArgumentException e) { }

    /* Null identifier */
    try {
      new AndroidConfigStore(context, null);
      fail("Didn't hit expected exception.");
    } catch (IllegalArgumentException e) { }

    /* Null protection */
    try {
      new AndroidConfigStore(context, name, null, KeyAuthenticator.noAuthentication());
      fail("Didn't hit expected exception.");
    } catch (IllegalArgumentException e) { }

    /* Null authenticator */
    try {
      new AndroidConfigStore(context, name, KeyAuthentication.withPassword(), null);
      fail("Didn't hit expected exception.");
    } catch (IllegalArgumentException e) { }
  }

  @Test
  public void testSaveWithNullParams() throws InterruptedException {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    String config = "string";

    /* Null AndroidConfigStore */
    try {
      Config.saveConfigSecurely(null, config, badParamSaveHandler);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }

    /* Null String */
    try {
      Config.saveConfigSecurely(new AndroidConfigStore(context, name), null, badParamSaveHandler);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }

    /* Null SaveHandler */
    try {
      Config.saveConfigSecurely(new AndroidConfigStore(context, name), config, null);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }
  }

  @Test
  public void testLoadWithNullParams() throws InterruptedException {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ConfigStore.LoadHandler badParamLoadHandler = new ConfigStore.LoadHandler() {
      @Override
      public void loadConfigDidSucceed(String config) {
        fail("TestCase: Unexpected loadConfigDidSucceed");
      }

      @Override
      public void loadConfigDidCancel() {
        fail("TestCase: Unexpected loadConfigDidCancel");
      }

      @Override
      public void loadConfigNotFound() {
        fail("TestCase: Unexpected loadConfigNotFound");
      }

      @Override
      public void loadConfigDidFail(Throwable e1) {
        throw new RuntimeException("TestCase: Unexpected loadConfigDidFail", e1);
      }
    };

    /* Null AndroidConfigStore */
    try {
      Config.loadConfigSecurely(null, badParamLoadHandler);
    }
    catch(IllegalArgumentException ex) { }

    /* Null SaveHandler */
    boolean expectedExceptionFound = false;
    try {
      Config.loadConfigSecurely(new AndroidConfigStore(context, name), null);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }

    /* Null AndroidConfigStore and SaveHandler */
    try {
      Config.loadConfigSecurely(null, null);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }

    /* Null Context */
    try {
      Config.loadConfigSecurely(new AndroidConfigStore(null, name), badParamLoadHandler);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }

    /* Null identifier */
    try {
      Config.loadConfigSecurely(new AndroidConfigStore(context, null), badParamLoadHandler);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }
  }

  @Test
  public void testRemoveWithNullParams() throws InterruptedException {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ConfigStore.RemoveHandler badParamRemoveHandler = new ConfigStore.RemoveHandler() {
      @Override
      public void removeConfigDidSucceed() {
        throw new RuntimeException("TestCase: Unexpected removeConfigDidSucceed");
      }

      @Override
      public void removeConfigDidFail(Throwable e1) {
        throw new RuntimeException("TestCase: Unexpected removeConfigDidFail", e1);
      }
    };

    /* Null AndroidConfigStore */
    try {
      Config.removeConfigSecurely(null, badParamRemoveHandler);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }

    /* Null SaveHandler */
    try {
      Config.removeConfigSecurely(new AndroidConfigStore(context, name), null);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) { }

    /* Null AndroidConfigStore and SaveHandler */
    try {
      Config.removeConfigSecurely(null, null);
      fail("Didn't hit expected exception");
    } catch (Exception e) {  }

    /* Null Context */
    try {
      Config.removeConfigSecurely(new AndroidConfigStore(null, name), badParamRemoveHandler);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) {
    }

    /* Null identifier */
    try {
      Config.removeConfigSecurely(new AndroidConfigStore(context, null), badParamRemoveHandler);
      fail("Didn't hit expected exception");
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void testBadIdentifierStrings() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    String string = "string";

    String badIdentifiers[] = {"",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec pellentesque orci eget ipsum porttitor, tincidunt luctus massa pharetra. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Pellentesque elementum nibh nec vehicula aliquam. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Quisque id lobortis ante...",
        "$#!#%@#$@",
        "FDSA$#@DSS)D((#@",
        "   ",
        "   FSDFS   ",
        "FDS FS FS F SDX",
        "\tdfjskfjls\rjfskjfdls"
    };

    for(final String badIdentifier : badIdentifiers) {
      try {
        Config.saveConfigSecurely(new AndroidConfigStore(context, badIdentifier), string, new ConfigStore.SaveHandler() {
          @Override
          public void saveConfigDidSucceed() {
            fail("TestCase: Unexpected saveConfigDidSucceed: " + badIdentifier);
          }

          @Override
          public void saveConfigDidCancel() {
            fail("TestCase: Unexpected saveConfigDidCancel: " + badIdentifier);
          }

          @Override
          public void saveConfigDidFail(Throwable e) {
            throw new RuntimeException("TestCase: Unexpected saveConfigDidFail: : " + badIdentifier, e);
          }
        });
        fail("Identifier '" + badIdentifier + "' should cause exception.");
      } catch (IllegalArgumentException e) { }
    }
  }
}
