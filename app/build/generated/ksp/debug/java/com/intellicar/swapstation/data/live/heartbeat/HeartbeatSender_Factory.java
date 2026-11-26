package com.intellicar.swapstation.data.live.heartbeat;

import com.intellicar.swapstation.core.config.LiveServerConfig;
import com.intellicar.swapstation.data.live.LiveMqttManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.serialization.json.Json;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.intellicar.swapstation.core.di.ApplicationScope")
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
public final class HeartbeatSender_Factory implements Factory<HeartbeatSender> {
  private final Provider<LiveMqttManager> liveProvider;

  private final Provider<LiveServerConfig> configProvider;

  private final Provider<HeartbeatBuilder> builderProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<CoroutineScope> scopeProvider;

  private HeartbeatSender_Factory(Provider<LiveMqttManager> liveProvider,
      Provider<LiveServerConfig> configProvider, Provider<HeartbeatBuilder> builderProvider,
      Provider<Json> jsonProvider, Provider<CoroutineScope> scopeProvider) {
    this.liveProvider = liveProvider;
    this.configProvider = configProvider;
    this.builderProvider = builderProvider;
    this.jsonProvider = jsonProvider;
    this.scopeProvider = scopeProvider;
  }

  @Override
  public HeartbeatSender get() {
    return newInstance(liveProvider.get(), configProvider.get(), builderProvider.get(), jsonProvider.get(), scopeProvider.get());
  }

  public static HeartbeatSender_Factory create(Provider<LiveMqttManager> liveProvider,
      Provider<LiveServerConfig> configProvider, Provider<HeartbeatBuilder> builderProvider,
      Provider<Json> jsonProvider, Provider<CoroutineScope> scopeProvider) {
    return new HeartbeatSender_Factory(liveProvider, configProvider, builderProvider, jsonProvider, scopeProvider);
  }

  public static HeartbeatSender newInstance(LiveMqttManager live, LiveServerConfig config,
      HeartbeatBuilder builder, Json json, CoroutineScope scope) {
    return new HeartbeatSender(live, config, builder, json, scope);
  }
}
