package com.intellicar.swapstation.data.live.heartbeat;

import com.intellicar.swapstation.core.config.LiveServerConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class HeartbeatBuilder_Factory implements Factory<HeartbeatBuilder> {
  private final Provider<LiveServerConfig> configProvider;

  private HeartbeatBuilder_Factory(Provider<LiveServerConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public HeartbeatBuilder get() {
    return newInstance(configProvider.get());
  }

  public static HeartbeatBuilder_Factory create(Provider<LiveServerConfig> configProvider) {
    return new HeartbeatBuilder_Factory(configProvider);
  }

  public static HeartbeatBuilder newInstance(LiveServerConfig config) {
    return new HeartbeatBuilder(config);
  }
}
