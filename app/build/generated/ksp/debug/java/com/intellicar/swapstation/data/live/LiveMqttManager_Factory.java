package com.intellicar.swapstation.data.live;

import android.content.Context;
import com.intellicar.swapstation.core.config.LiveServerConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "dagger.hilt.android.qualifiers.ApplicationContext",
    "com.intellicar.swapstation.core.di.ApplicationScope"
})
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
public final class LiveMqttManager_Factory implements Factory<LiveMqttManager> {
  private final Provider<Context> contextProvider;

  private final Provider<LiveServerConfig> configProvider;

  private final Provider<ClientIdProvider> clientIdProvider;

  private final Provider<CoroutineScope> scopeProvider;

  private LiveMqttManager_Factory(Provider<Context> contextProvider,
      Provider<LiveServerConfig> configProvider, Provider<ClientIdProvider> clientIdProvider,
      Provider<CoroutineScope> scopeProvider) {
    this.contextProvider = contextProvider;
    this.configProvider = configProvider;
    this.clientIdProvider = clientIdProvider;
    this.scopeProvider = scopeProvider;
  }

  @Override
  public LiveMqttManager get() {
    return newInstance(contextProvider.get(), configProvider.get(), clientIdProvider.get(), scopeProvider.get());
  }

  public static LiveMqttManager_Factory create(Provider<Context> contextProvider,
      Provider<LiveServerConfig> configProvider, Provider<ClientIdProvider> clientIdProvider,
      Provider<CoroutineScope> scopeProvider) {
    return new LiveMqttManager_Factory(contextProvider, configProvider, clientIdProvider, scopeProvider);
  }

  public static LiveMqttManager newInstance(Context context, LiveServerConfig config,
      ClientIdProvider clientIdProvider, CoroutineScope scope) {
    return new LiveMqttManager(context, config, clientIdProvider, scope);
  }
}
