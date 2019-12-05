/*
Copyright 2019 Jigsaw Operations LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package app.intra.sys.firebase;

import android.os.Build;
import android.os.Build.VERSION;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

/**
 * Utility class for initializing Firebase Remote Config.  Remote Configuration allows us to conduct
 * A/B tests of experimental functionality, and to enable or disable features without having to
 * release a new version of the app.
 */
public class RemoteConfig {
  public static Task<Boolean> update() {
    try {
      FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
      return config.fetchAndActivate();
    } catch (IllegalStateException e) {
      LogWrapper.logException(e);
      return Tasks.forResult(false);
    }
  }

  public static boolean getUseGoDoh() {
    if (getUseSplitMode()) {
      // Split mode doesn't use Go-DOH, so Go-DOH shouldn't be used for other purposes
      // (especially probes) in split mode, for consistency.
      return false;
    }
    try {
      return !FirebaseRemoteConfig.getInstance()
          .getBoolean("disable_go_doh");
    } catch (IllegalStateException e) {
      LogWrapper.logException(e);
      return true;
    }
  }

  public static boolean getUseSplitMode() {
    // On M and later, Chrome uses getActiveNetwork() to determine which DNS servers to use.
    // A full tunnel configuration makes this VPN the active network, whereas a
    // split-tunnel configuration does not.  Therefore, on M and later, we cannot use
    // split mode.  Additionally, M and later also exhibit DownloadManager bugs when used
    // with a split-tunnel VPN.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || true) {
      return false;
    }

    try {
      // go_min_version is the minimum Android version that should use the GoVpnAdapter (i.e. the
      // full tunnel configuration).
      long minVersion = FirebaseRemoteConfig.getInstance().getLong("go_min_version");
      if (minVersion == FirebaseRemoteConfig.DEFAULT_VALUE_FOR_LONG) {
        // The split mode remote configuration is not present.  Use split mode.
        return true;
      }
      return VERSION.SDK_INT < minVersion;
    } catch (IllegalStateException e) {
      LogWrapper.logException(e);
      return true;
    }
  }
}
