package com.intellicar.swapstation.presentation.live;

import com.intellicar.swapstation.core.config.LiveServerConfig;
import com.intellicar.swapstation.data.live.LiveMqttManager;
import com.intellicar.swapstation.data.live.heartbeat.HeartbeatSender;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class LiveServerViewModel_Factory implements Factory<LiveServerViewModel> {
  private final Provider<LiveMqttManager> liveProvider;

  private final Provider<LiveServerConfig> configProvider;

  private final Provider<HeartbeatSender> heartbeatProvider;

  private LiveServerViewModel_Factory(Provider<LiveMqttManager> liveProvider,
      Provider<LiveServerConfig> configProvider, Provider<HeartbeatSender> heartbeatProvider) {
    this.liveProvider = liveProvider;
    this.configProvider = configProvider;
    this.heartbeatProvider = heartbeatProvider;
  }

  @Override
  public LiveServerViewModel get() {
    return newInstance(liveProvider.get(), configProvider.get(), heartbeatProvider.get());
  }

  public static LiveServerViewModel_Factory create(Provider<LiveMqttManager> liveProvider,
      Provider<LiveServerConfig> configProvider, Provider<HeartbeatSender> heartbeatProvider) {
    return new LiveServerViewModel_Factory(liveProvider, configProvider, heartbeatProvider);
  }

  public static LiveServerViewModel newInstance(LiveMqttManager live, LiveServerConfig config,
      HeartbeatSender heartbeat) {
    return new LiveServerViewModel(live, config, heartbeat);
  }
}
